package cn.aiedge.base.social;

/**
 * 三方平台 OAuth 适配器。
 *
 * <p>各平台的授权地址、换 token 与取用户信息的接口差异全部收敛在实现里，
 * 业务侧只依赖本接口，新增平台不改业务代码。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
public interface SocialOAuthProvider {

    /** 平台标识：{@code dingtalk} / {@code wecom} / {@code feishu} */
    String platform();

    /** 平台展示名（前端图标下方文字用） */
    String displayName();

    /** 该平台凭据是否已配置齐（未配置则前端不暴露入口） */
    boolean configured();

    /**
     * 构造三方授权页地址（前端跳转过去让用户扫码/授权）。
     *
     * @param state 防 CSRF 状态串，回调时原样带回；本系统用它同时承载「绑定」或「登录」意图与用户身份
     * @return 完整授权 URL
     */
    String buildAuthorizeUrl(String state);

    /**
     * 用授权码换取三方用户身份。
     *
     * @param code 三方回调带回的授权码（一次性、通常 5 分钟内有效）
     * @return 统一口径的用户身份
     * @throws cn.aiedge.common.exception.BusinessException 换取失败时
     */
    SocialUser exchangeUser(String code);
}
