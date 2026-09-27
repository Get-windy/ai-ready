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
 * 钉钉扫码登录（企业内部应用）。
 *
 * <p>流程（依据钉钉开放平台「扫码登录第三方网站」）：
 * ① 跳 {@code login.dingtalk.com/oauth2/auth} 让用户扫码；
 * ② 回调带回 {@code code}；③ {@code POST /v1.0/oauth2/userAccessToken} 换 userAccessToken；
 * ④ {@code GET /v1.0/contact/users/me} 取 unionId / openId / 昵称 / 头像。</p>
 *
 * <p><b>不需要服务商资质</b>，任意钉钉用户都能扫码；但按官方限制**只返回 unionId，
 * 拿不到手机号与企业信息**（需额外权限），因此本系统只把它当身份认证手段，
 * 企业归属完全由系统内的账号绑定关系决定。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkOAuthProvider implements SocialOAuthProvider {

    private static final String AUTHORIZE_URL = "https://login.dingtalk.com/oauth2/auth";
    private static final String TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String USER_INFO_URL = "https://api.dingtalk.com/v1.0/contact/users/me";

    private final SocialLoginProperties properties;

    @Override
    public String platform() {
        return "dingtalk";
    }

    @Override
    public String displayName() {
        return "钉钉";
    }

    @Override
    public boolean configured() {
        SocialLoginProperties.Dingtalk config = properties.getDingtalk();
        return properties.isEnabled()
                && StrUtil.isNotBlank(config.getAppKey())
                && StrUtil.isNotBlank(config.getAppSecret());
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return AUTHORIZE_URL
                + "?redirect_uri=" + encode(properties.callbackUrl(platform()))
                + "&response_type=code"
                + "&client_id=" + encode(properties.getDingtalk().getAppKey())
                + "&scope=openid"
                + "&prompt=consent"
                + "&state=" + encode(state);
    }

    @Override
    public SocialUser exchangeUser(String code) {
        SocialLoginProperties.Dingtalk config = properties.getDingtalk();

        // ① code → userAccessToken（授权码约 5 分钟有效、仅能用一次）
        JSONObject tokenResp = SocialHttp.postJson(TOKEN_URL, null, Map.of(
                "clientId", config.getAppKey(),
                "clientSecret", config.getAppSecret(),
                "code", code,
                "grantType", "authorization_code"));
        String accessToken = SocialHttp.str(tokenResp, "accessToken");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("[三方登录] 钉钉换取 token 失败: {}", tokenResp);
            throw BusinessException.badRequest("钉钉授权失败，请重试");
        }

        // ② userAccessToken → 用户身份
        JSONObject user = SocialHttp.get(USER_INFO_URL,
                Map.of("x-acs-dingtalk-access-token", accessToken));

        String openId = SocialHttp.str(user, "openId");
        if (StrUtil.isBlank(openId)) {
            log.warn("[三方登录] 钉钉未返回 openId: {}", user);
            throw BusinessException.badRequest("钉钉未返回用户标识");
        }
        return new SocialUser(platform(), null, openId,
                SocialHttp.str(user, "unionId"), null,
                SocialHttp.str(user, "nick"), SocialHttp.str(user, "avatarUrl"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
