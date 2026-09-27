package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录分流：身份确认之后、Token 签发之前的公共逻辑。
 *
 * <p><b>为什么单独成 Service</b>：账号密码登录、手机号验证码登录、三方扫码登录
 * 三条路径在「确认身份」之后的行为必须完全一致（单企业直接登入 / 多企业返回候选待选）。
 * 若各自实现一份，改一处漏一处的风险极高 —— 本仓已有对称模块不同步的历史教训。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginFlowService {

    /** 「待选企业」票据：多企业登录时先验身份，待用户选定企业后再签发 Token */
    public static final String SELECT_TOKEN_PREFIX = "login:select-tenant:";
    private static final long SELECT_TOKEN_TTL_MINUTES = 5;

    private final SysUserService userService;
    private final SysLoginLogService loginLogService;
    private final StringRedisTemplate redisTemplate;
    private final SecurityContext securityContext;

    /**
     * 登录结果。
     *
     * @param needSelectTenant  是否还需用户选择企业（为 true 时 {@code token} 与 {@code tenant} 为 null）
     * @param token             登录 Token（已选定企业时）
     * @param userId            用户ID
     * @param tenant            已登入的企业（{@code needSelectTenant=false} 时非空）
     * @param tenants           该账号可访问的企业候选（顺序即推荐顺序，上次登录的排第一）
     * @param selectToken       选择企业用的短期票据
     * @param lastLoginTenantId 上次登录的企业ID（前端据此在候选里打标）
     */
    public record Outcome(
            boolean needSelectTenant,
            String token,
            Long userId,
            SysTenant tenant,
            List<SysTenant> tenants,
            String selectToken,
            Long lastLoginTenantId
    ) {
    }

    /**
     * 身份已确认后的分流：按可访问企业数量决定「直接登入」还是「返回候选待选」。
     *
     * @param user    已通过身份校验的用户（密码 / 短信码 / 三方身份）
     * @param loginIp 登录IP
     */
    public Outcome proceed(SysUser user, String loginIp) {
        List<SysTenant> tenants = userService.getUserTenants(user.getId());
        if (tenants.isEmpty()) {
            throw BusinessException.badRequest("该账号未关联任何企业，请联系管理员");
        }

        // 只有一个企业 —— 直接登入，不让用户做无谓的选择
        if (tenants.size() == 1) {
            SysTenant tenant = tenants.get(0);
            String token = loginInTenant(user, tenant, loginIp);
            return new Outcome(false, token, user.getId(), tenant, tenants, null, null);
        }

        // 多个企业 —— 先不签发 Token，返回候选列表与短期票据，待用户选定后再签
        String selectToken = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(SELECT_TOKEN_PREFIX + selectToken,
                String.valueOf(user.getId()), SELECT_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        // 此时尚未更新 last_login_time，查到的正是「上一次」登录的企业
        return new Outcome(true, null, user.getId(), null, tenants,
                selectToken, userService.getLastLoginTenantId(user.getId()));
    }

    /**
     * 在指定企业下签发 Token（含租户上下文包裹，避免多租户拦截器注入 tenant_id=0）
     */
    public String loginInTenant(SysUser user, SysTenant tenant, String loginIp) {
        securityContext.setTempTenantId(tenant.getId());
        try {
            return userService.completeLogin(user, tenant.getId(), loginIp);
        } finally {
            securityContext.clearTempTenantId();
        }
    }

    // ========== 返回体组装（三条登录路径共用，保证前端拿到的结构一致） ==========

    /**
     * 登录成功的返回体（Token + 当前企业 + 企业候选列表）
     */
    public Map<String, Object> buildLoginResult(Outcome outcome) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", outcome.token());
        result.put("tokenName", "Authorization");
        result.put("userId", outcome.userId());
        result.put("tenantId", outcome.tenant().getId());
        result.put("tenantName", outcome.tenant().getTenantName());
        result.put("needSelectTenant", false);
        result.put("tenants", toTenantOptions(outcome.tenants()));
        return result;
    }

    /**
     * 「待选企业」的返回体：不签发 Token，只给候选列表与短期票据
     */
    public Map<String, Object> buildSelectTenantResult(Outcome outcome) {
        Map<String, Object> result = new HashMap<>();
        result.put("needSelectTenant", true);
        result.put("selectToken", outcome.selectToken());
        result.put("tenants", toTenantOptions(outcome.tenants()));
        result.put("lastLoginTenantId", outcome.lastLoginTenantId());
        return result;
    }

    /**
     * 企业候选列表（顺序即登录时的推荐顺序：上次登录的排第一）
     */
    public List<Map<String, Object>> toTenantOptions(List<SysTenant> tenants) {
        return tenants.stream().map(t -> Map.<String, Object>of(
                "id", t.getId(),
                "tenantName", t.getTenantName(),
                "tenantCode", t.getTenantCode(),
                "status", t.getStatus()
        )).toList();
    }

    /**
     * 记录成功登录日志（出错不影响登录）
     */
    public void recordLoginSuccess(SysUser user, Long tenantId, String loginIp, String userAgent) {
        try {
            loginLogService.recordLogin(tenantId, user.getId(), user.getUsername(),
                    1, 0, null, loginIp, userAgent, IdUtil.fastSimpleUUID());
        } catch (Exception e) {
            log.warn("记录登录日志失败，不影响登录: {}", e.getMessage());
        }
    }
}
