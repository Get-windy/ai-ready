package cn.aiedge.base.social;

import cn.aiedge.common.exception.BusinessException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 企业微信扫码登录（自建应用）。
 *
 * <p>流程：① 跳 {@code login.work.weixin.qq.com/wwlogin/sso/login}（自建应用 {@code login_type=CorpApp}）；
 * ② 回调带回 {@code code}；③ CorpID + 应用 Secret 换 {@code access_token}；
 * ④ {@code /cgi-bin/auth/getuserinfo} 取成员 userid。</p>
 *
 * <p><b>⚠️ 自建应用只能让本企业成员登录。</b>若要支持「每个客户企业用各自的企微登录」，
 * 必须升级为服务商模式（第三方应用 / 代开发应用）：改用 suite_access_token +
 * {@code /cgi-bin/service/getuserinfo3rd}，并额外实现一个接收 {@code suite_ticket} 的
 * 指令回调地址来维持 token —— 那部分待服务商资质具备后再接。</p>
 *
 * <p>⚠️ 此处的 CorpID 是 {@code ww} 开头，与微信开放平台的 {@code wx} 开头 AppID
 * 属于两套完全不同的体系，不可混用（个人微信扫码不在本适配器职责内）。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomOAuthProvider implements SocialOAuthProvider {

    private static final String AUTHORIZE_URL = "https://login.work.weixin.qq.com/wwlogin/sso/login";
    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";

    private final SocialLoginProperties properties;

    @Override
    public String platform() {
        return "wecom";
    }

    @Override
    public String displayName() {
        return "企业微信";
    }

    @Override
    public boolean configured() {
        SocialLoginProperties.Wecom config = properties.getWecom();
        return properties.isEnabled()
                && StrUtil.isNotBlank(config.getCorpId())
                && StrUtil.isNotBlank(config.getAgentId())
                && StrUtil.isNotBlank(config.getSecret());
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        SocialLoginProperties.Wecom config = properties.getWecom();
        return AUTHORIZE_URL
                + "?login_type=CorpApp"
                + "&appid=" + encode(config.getCorpId())
                + "&agentid=" + encode(config.getAgentId())
                + "&redirect_uri=" + encode(properties.callbackUrl(platform()))
                + "&scope=snsapi_base"
                + "&state=" + encode(state);
    }

    @Override
    public SocialUser exchangeUser(String code) {
        SocialLoginProperties.Wecom config = properties.getWecom();

        // ① CorpID + 应用 Secret → access_token（每个应用独立一套，不可全企业共用）
        JSONObject tokenResp = SocialHttp.get(GET_TOKEN_URL
                + "?corpid=" + encode(config.getCorpId())
                + "&corpsecret=" + encode(config.getSecret()), null);
        String accessToken = SocialHttp.str(tokenResp, "access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("[三方登录] 企微换取 access_token 失败: {}", tokenResp);
            throw BusinessException.badRequest("企业微信授权失败，请重试");
        }

        // ② code → 成员身份
        JSONObject user = SocialHttp.get(GET_USER_INFO_URL
                + "?access_token=" + encode(accessToken)
                + "&code=" + encode(code), null);
        String userId = SocialHttp.str(user, "userid");
        if (StrUtil.isBlank(userId)) {
            log.warn("[三方登录] 企微未返回 userid（可能非本企业成员）: {}", user);
            throw BusinessException.badRequest("企业微信未返回成员身份，请确认是本企业成员");
        }

        // 企微没有跨应用 unionId：以 corpId + userid 作为唯一标识（与建表唯一索引口径一致）
        return new SocialUser(platform(), config.getCorpId(), userId,
                null, userId, null, null);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
