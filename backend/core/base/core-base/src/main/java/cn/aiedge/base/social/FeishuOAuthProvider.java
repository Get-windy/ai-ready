package cn.aiedge.base.social;

import cn.aiedge.common.exception.BusinessException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 飞书网页登录（网页应用 OAuth2 授权码模式）。
 *
 * <p>流程：① 跳 {@code accounts.feishu.cn/open-apis/authen/v1/authorize}；
 * ② 回调带回 {@code code}；③ {@code POST /open-apis/authen/v2/oauth/token} 换 user_access_token；
 * ④ {@code GET /open-apis/authen/v1/user_info} 取 open_id / union_id / 姓名 / 头像。</p>
 *
 * <p><b>⚠️ 应用必须先发布</b>（「版本管理与发布」创建版本并启用），否则 App ID / App Secret
 * 不生效、授权会被拒 —— 这是最常见的踩坑点。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuOAuthProvider implements SocialOAuthProvider {

    private static final String AUTHORIZE_URL = "https://accounts.feishu.cn/open-apis/authen/v1/authorize";
    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    private final SocialLoginProperties properties;

    @Override
    public String platform() {
        return "feishu";
    }

    @Override
    public String displayName() {
        return "飞书";
    }

    @Override
    public boolean configured() {
        SocialLoginProperties.Feishu config = properties.getFeishu();
        return properties.isEnabled()
                && StrUtil.isNotBlank(config.getAppId())
                && StrUtil.isNotBlank(config.getAppSecret());
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return AUTHORIZE_URL
                + "?client_id=" + encode(properties.getFeishu().getAppId())
                + "&redirect_uri=" + encode(properties.callbackUrl(platform()))
                + "&response_type=code"
                + "&state=" + encode(state);
    }

    @Override
    public SocialUser exchangeUser(String code) {
        SocialLoginProperties.Feishu config = properties.getFeishu();
        String redirectUri = properties.callbackUrl(platform());

        // ① code → user_access_token（授权码 3~5 分钟有效、仅一次）
        JSONObject tokenResp = SocialHttp.postJson(TOKEN_URL, null, Map.of(
                "grant_type", "authorization_code",
                "client_id", config.getAppId(),
                "client_secret", config.getAppSecret(),
                "code", code,
                "redirect_uri", redirectUri));
        String accessToken = SocialHttp.str(tokenResp, "access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("[三方登录] 飞书换取 token 失败: {}", tokenResp);
            throw BusinessException.badRequest("飞书授权失败，请重试");
        }

        // ② 取用户信息（飞书把业务数据包在 data 里）
        JSONObject resp = SocialHttp.get(USER_INFO_URL, Map.of("Authorization", "Bearer " + accessToken));
        JSONObject user = resp.getJSONObject("data");
        if (user == null) {
            log.warn("[三方登录] 飞书返回结构异常: {}", resp);
            throw BusinessException.badRequest("飞书未返回用户信息");
        }

        String openId = SocialHttp.str(user, "open_id");
        if (StrUtil.isBlank(openId)) {
            log.warn("[三方登录] 飞书未返回 open_id: {}", user);
            throw BusinessException.badRequest("飞书未返回用户标识");
        }
        return new SocialUser(platform(), null, openId,
                SocialHttp.str(user, "union_id"), null,
                SocialHttp.str(user, "name"), SocialHttp.str(user, "avatar_url"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
