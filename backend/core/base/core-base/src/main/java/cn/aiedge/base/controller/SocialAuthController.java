package cn.aiedge.base.controller;

import cn.aiedge.base.dto.AuthDTO;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserSocialBinding;
import cn.aiedge.base.service.LoginFlowService;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.social.SocialBindingService;
import cn.aiedge.base.social.SocialLoginProperties;
import cn.aiedge.base.social.SocialOAuthProvider;
import cn.aiedge.base.social.SocialUser;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 三方登录控制器（钉钉 / 企业微信 / 飞书）。
 *
 * <p><b>两条链路</b>：</p>
 * <ol>
 *   <li><b>绑定</b>（需已登录）：取授权地址（{@code mode=bind}）→ 用户授权 →
 *       回调把三方身份挂到当前账号，之后可用于免密登录；</li>
 *   <li><b>登录</b>：取授权地址（{@code mode=login}）→ 用户授权 → 回调按三方身份反查系统账号 →
 *       签发一次性票据 → 前端拿票据换 Token。<b>Token 不放进 URL</b>，避免进入浏览器历史与 Referer。</li>
 * </ol>
 *
 * <p>未配置凭据的平台不会出现在 {@code /providers} 里，前端也就不渲染入口 —— 不做点了没反应的死按钮。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
@Tag(name = "三方登录", description = "钉钉 / 企业微信 / 飞书 的绑定与扫码登录")
public class SocialAuthController {

    /** 授权 state：防 CSRF + 承载「绑定/登录」意图与用户身份，一次性消费 */
    private static final String STATE_PREFIX = "login:social:state:";
    /** 回调完成后交给前端的一次性票据 */
    private static final String TICKET_PREFIX = "login:social:ticket:";
    private static final long TTL_MINUTES = 5;

    private final List<SocialOAuthProvider> providers;
    private final SocialBindingService bindingService;
    private final SocialLoginProperties properties;
    private final LoginFlowService loginFlowService;
    private final SysUserService userService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 可用的三方平台（前端据此渲染入口；未配置凭据的平台不出现）
     */
    @Operation(summary = "可用的三方登录平台")
    @GetMapping("/providers")
    public Result<List<Map<String, Object>>> providers() {
        List<Map<String, Object>> list = providers.stream()
                .filter(SocialOAuthProvider::configured)
                .map(p -> Map.<String, Object>of("platform", p.platform(), "name", p.displayName()))
                .toList();
        return Result.ok(list);
    }

    /**
     * 取三方授权地址
     *
     * @param mode {@code login} 登录 / {@code bind} 绑定（绑定需已登录）
     */
    @Operation(summary = "获取三方授权地址", description = "mode=login 登录 / mode=bind 绑定三方账号")
    @GetMapping("/{platform}/authorize-url")
    public Result<String> authorizeUrl(@PathVariable String platform,
                                       @RequestParam(defaultValue = "login") String mode) {
        SocialOAuthProvider provider = provider(platform);
        if (!provider.configured()) {
            return Result.fail(503, provider.displayName() + "登录未配置，请联系管理员");
        }

        Long userId = null;
        if ("bind".equals(mode)) {
            if (!StpUtil.isLogin()) {
                return Result.fail(401, "请先登录后再绑定");
            }
            userId = StpUtil.getLoginIdAsLong();
        }

        String state = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(STATE_PREFIX + state, JSONUtil.toJsonStr(Map.of(
                "mode", mode,
                "platform", platform,
                "userId", userId == null ? "" : userId)), TTL_MINUTES, TimeUnit.MINUTES);

        return Result.ok(provider.buildAuthorizeUrl(state));
    }

    /**
     * 三方授权回调：处理绑定或登录，然后 302 回前端落地页（只带一次性票据，不带 Token）
     */
    @Operation(summary = "三方授权回调")
    @GetMapping("/{platform}/callback")
    public void callback(@PathVariable String platform,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        try {
            // state 一次性消费：取不到即视为过期或被重放
            String payload = state == null ? null : redisTemplate.opsForValue().get(STATE_PREFIX + state);
            if (payload == null) {
                log.warn("[三方登录] state 无效或已过期: platform={}", platform);
                redirectToFrontend(response, properties.getFrontendCallbackUrl(), "socialError", "state_expired");
                return;
            }
            redisTemplate.delete(STATE_PREFIX + state);

            if (code == null || code.isBlank()) {
                log.warn("[三方登录] 未拿到授权码（用户可能取消了授权）: platform={}", platform);
                redirectToFrontend(response, properties.getFrontendCallbackUrl(), "socialError", "cancelled");
                return;
            }

            JSONObject stateJson = JSONUtil.parseObj(payload);
            String mode = stateJson.getStr("mode");
            String boundUserId = stateJson.getStr("userId");

            SocialUser social = provider(platform).exchangeUser(code);

            // ① 绑定：把三方身份挂到当前登录用户；完成回个人中心（绑定就是从那里发起的）
            if ("bind".equals(mode)) {
                String bindBase = properties.getBindCallbackUrl();
                if (boundUserId == null || boundUserId.isBlank()) {
                    redirectToFrontend(response, bindBase, "socialError", "failed");
                    return;
                }
                bindingService.bind(Long.parseLong(boundUserId), social);
                redirectToFrontend(response, bindBase, "socialBind", "ok");
                return;
            }

            // ② 登录：按三方身份反查系统账号
            SysUserSocialBinding binding = bindingService.findByIdentity(social);
            if (binding == null) {
                log.warn("[三方登录] 该三方账号尚未绑定系统账号: platform={}, openId={}",
                        platform, social.openId());
                redirectToFrontend(response, properties.getFrontendCallbackUrl(), "socialError", "not_bound");
                return;
            }
            bindingService.touchLastLogin(binding.getId());

            String ticket = IdUtil.fastSimpleUUID();
            redisTemplate.opsForValue().set(TICKET_PREFIX + ticket,
                    String.valueOf(binding.getUserId()), TTL_MINUTES, TimeUnit.MINUTES);
            redirectToFrontend(response, properties.getFrontendCallbackUrl(), "socialTicket", ticket);

        } catch (Exception e) {
            log.warn("[三方登录] 回调处理失败: platform={}, reason={}", platform, e.getMessage());
            redirectToFrontend(response, properties.getFrontendCallbackUrl(), "socialError", "failed");
        }
    }

    /**
     * 用回调票据换取登录结果（Token 或「待选企业」）
     */
    @Operation(summary = "三方登录换取 Token", description = "回调后前端用一次性票据兑换，Token 不出现在 URL 中")
    @PostMapping("/exchange")
    public Result<Map<String, Object>> exchange(@Valid @RequestBody AuthDTO.SocialExchange dto,
                                                HttpServletRequest request) {
        String key = TICKET_PREFIX + dto.ticket();
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            return Result.fail(401, "登录已超时，请重新发起");
        }
        redisTemplate.delete(key);

        Long userId = Long.parseLong(userIdStr);
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() != 1) {
            return Result.fail(401, "账号状态异常，请联系管理员");
        }

        String loginIp = JakartaServletUtil.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        LoginFlowService.Outcome outcome = loginFlowService.proceed(user, loginIp);

        if (outcome.needSelectTenant()) {
            return Result.ok("请选择要登录的企业", loginFlowService.buildSelectTenantResult(outcome));
        }
        loginFlowService.recordLoginSuccess(user, outcome.tenant().getId(), loginIp, userAgent);
        return Result.ok("登录成功", loginFlowService.buildLoginResult(outcome));
    }

    /**
     * 当前账号的三方绑定状态（含未绑定的平台，前端才能展示「去绑定」）
     */
    @Operation(summary = "我的三方绑定")
    @GetMapping("/bindings")
    @SaCheckLogin
    public Result<List<Map<String, Object>>> bindings() {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, SysUserSocialBinding> bound = new HashMap<>();
        for (SysUserSocialBinding binding : bindingService.listByUser(userId)) {
            bound.put(binding.getPlatform(), binding);
        }

        List<Map<String, Object>> list = providers.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("platform", p.platform());
            item.put("name", p.displayName());
            item.put("configured", p.configured());
            SysUserSocialBinding binding = bound.get(p.platform());
            item.put("bound", binding != null);
            if (binding != null) {
                item.put("nickname", binding.getNickname());
                item.put("bindTime", binding.getBindTime());
            }
            return item;
        }).toList();
        return Result.ok(list);
    }

    /**
     * 解绑
     */
    @Operation(summary = "解绑三方账号")
    @DeleteMapping("/bindings/{platform}")
    @SaCheckLogin
    public Result<Void> unbind(@PathVariable String platform) {
        bindingService.unbind(StpUtil.getLoginIdAsLong(), platform);
        return Result.ok("已解绑", null);
    }

    // ========== 内部辅助 ==========

    private SocialOAuthProvider provider(String platform) {
        return providers.stream()
                .filter(p -> p.platform().equals(platform))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("不支持的三方平台"));
    }

    /**
     * 302 回前端落地页并带上结果参数（只带票据/错误码，不带 Token）
     *
     * @param base 落地页地址（登录回登录页，绑定回个人中心）
     */
    private void redirectToFrontend(HttpServletResponse response, String base, String key, String value)
            throws IOException {
        String separator = base.contains("?") ? "&" : "?";
        response.sendRedirect(base + separator + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
