package cn.aiedge.base.social;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 三方登录配置（钉钉 / 企业微信 / 飞书）。
 *
 * <p><b>采用路线 B（服务商模式）</b>：平台方申请服务商资质与第三方应用，
 * 客户企业安装应用后，其成员即可用各自企业的钉钉/企微/飞书登录。</p>
 *
 * <p><b>凭据由运营后期填入</b>。未配置（或 {@code enabled=false}）的平台自动不可用：
 * 前端不会渲染该入口，也不会签发授权地址 —— 避免出现"点了没反应"的死按钮。</p>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * social-login:
 *   enabled: true
 *   backend-callback-base: https://erp.example.com
 *   frontend-callback-url: /login
 *   dingtalk:
 *     enabled: true
 *     suite-key: dingxxxxxxx
 *     suite-secret: xxxx
 *   wecom:
 *     enabled: true
 *     suite-id: wwxxxxxxx
 *     suite-secret: xxxx
 *   feishu:
 *     enabled: true
 *     app-id: cli_xxxxxxx
 *     app-secret: xxxx
 * </pre>
 * </p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Data
@Component
@ConfigurationProperties(prefix = "social-login")
public class SocialLoginProperties {

    /** 总开关：关闭时所有三方入口都不暴露 */
    private boolean enabled = false;

    /**
     * 后端回调地址前缀（三方授权服务器回调到 {@code <base>/api/auth/social/{platform}/callback}）。
     * <p>必须是**公网可访问**的地址，且与三方平台后台配置的回调域完全一致（含协议与端口），
     * 否则会报 redirect_uri 不匹配。</p>
     */
    private String backendCallbackBase = "";

    /** 登录授权完成后的前端落地页（后端处理完回调后跳到这里，并带上结果参数） */
    private String frontendCallbackUrl = "/login";

    /** 绑定授权完成后的前端落地页（绑定是从个人中心发起的，故回到个人中心） */
    private String bindCallbackUrl = "/profile";

    private Dingtalk dingtalk = new Dingtalk();

    private Wecom wecom = new Wecom();

    private Feishu feishu = new Feishu();

    /**
     * 后端回调地址（三方平台侧须配置成完全相同的值）
     */
    public String callbackUrl(String platform) {
        return backendCallbackBase + "/api/auth/social/" + platform + "/callback";
    }

    /**
     * 钉钉：企业内部应用（AppKey / AppSecret）。
     * <p>钉钉的「扫码登录第三方网站」用企业内部应用即可，<b>不需要服务商资质</b>，
     * 任意钉钉用户都能扫码；但按官方限制**只返回 unionId，拿不到手机号与企业信息**，
     * 因此本系统只把它当身份认证手段，企业归属由系统内的绑定关系决定。</p>
     */
    @Data
    public static class Dingtalk {
        private String appKey;
        private String appSecret;
    }

    /**
     * 企业微信：自建应用（CorpID + AgentId + Secret）。
     * <p>自建应用**只能让本企业成员登录**。若要支持「每个客户企业用各自的企微登录」，
     * 必须升级为服务商模式（第三方应用 / 代开发应用），额外需要 suite_id / suite_secret，
     * 以及一个能接收 {@code suite_ticket} 的指令回调地址 —— 这部分待服务商资质下来后再接。</p>
     * <p>⚠️ 这里的 CorpID 是 {@code ww} 开头；微信开放平台的 AppID 是 {@code wx} 开头，
     * 属于**两套完全不同的体系**，不可混用（个人微信扫码登录不在本适配器职责内）。</p>
     */
    @Data
    public static class Wecom {
        /** 企业ID（{@code ww} 开头） */
        private String corpId;
        /** 自建应用 AgentId（纯数字） */
        private String agentId;
        /** 自建应用 Secret（注意：不是通讯录同步的 Secret） */
        private String secret;
    }

    /** 飞书：网页应用（应用需先发布，否则凭据不生效） */
    @Data
    public static class Feishu {
        /** 应用 App ID（形如 {@code cli_xxx}） */
        private String appId;
        private String appSecret;
    }
}
