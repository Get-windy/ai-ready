package cn.aiedge.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付渠道参数（设置 → 系统配置 → 支付配置 →「支付方式」Tab 的参数配置抽屉）。
 *
 * <p>字段逐字取自原前端 {@code api/payment/index.ts} 的 {@code PaymentChannelParam} 接口，
 * 未新增任何文档没有的字段。区别只在于：<b>载体由「只写缓存」改为真实落库</b>
 * （{@code sys_project_config}，键 {@code payment.channel.{渠道码小写}}，值为本对象的 JSON）。
 * 见《设置模块/支付配置开发文档.md》§3.7 / §5.3 / §12-①。</p>
 */
@Data
@Schema(description = "支付渠道参数")
public class PaymentChannelParam {

    @Schema(description = "渠道分配的 APP ID")
    private String appId;

    @Schema(description = "商户号 / MCH ID")
    private String merchantNo;

    @Schema(description = "API 密钥 / API Secret")
    private String appSecret;

    @Schema(description = "异步通知 URL")
    private String notifyUrl;

    @Schema(description = "是否启用该渠道")
    private Boolean enabled;

    // ═══════════════════════════════════════════════════════════════════════
    // 回调验签凭据（2026-09-21 补）
    //
    // 【为什么打破「不加字段」】本类原设计刻意「逐字对齐对标前端、不加字段」。
    // 但原 5 个字段够「发起支付」、**不够验签回调**：
    //   · 支付宝异步通知是 RSA2(SHA256withRSA) 签名，验签要**支付宝公钥**（不是应用私钥）；
    //   · 微信支付 APIv3 是「平台证书私钥签名 + APIv3 密钥 AES-GCM 加密 resource」，
    //     验签要**平台证书公钥**，解 resource 还要 **APIv3 密钥**。
    // 没有这些字段，`PaymentCallbackVerifier` 的实现就只能像原来的桩那样
    // 「不验签直接当成功」—— 即「任何人 POST 一个单号就能把订单标为已支付」。
    // 故这里必须加；加的是 **JSON 内的可选字段**，对既有配置向后兼容。
    //
    // 【⚠️ 明文存储】这些值与其他 sys_project_config 值一样**明文落库**。
    // 真实收款前应改为加密存储或接密钥管理；此处如实标注，不做半成品的「假加密」。
    // ═══════════════════════════════════════════════════════════════════════

    @Schema(description = "【验签】支付宝公钥（Base64，不含 PEM 头尾）")
    private String alipayPublicKey;

    @Schema(description = "【验签】微信支付 APIv3 密钥（32 位，用于 AES-256-GCM 解 resource）")
    private String wechatApiV3Key;

    /**
     * 【验签】微信支付**平台证书公钥表**：JSON 对象，键=证书序列号，值=PEM 公钥文本。
     * <pre>{"5157F09E...":"-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"}</pre>
     *
     * <p><b>为什么是「表」而不是单个证书</b>：微信平台证书会**定期轮换**，且轮换期间
     * 新旧证书并存 —— 回调头 {@code Wechatpay-Serial} 告诉本次用哪张。
     * 若只配一张，轮换当天所有回调验签失败。故按序列号建映射，新旧共存，平滑过渡。</p>
     *
     * <p>自动下载平台证书需要商户私钥 + 证书序列号做签名请求（{@code GET /v3/certificates}），
     * 属后续增强；本字段是**验签的权威来源**，自动下载只是它的填充器。</p>
     */
    private String wechatPlatformCerts;

    /**
     * 【验签·银联】**银联平台证书公钥表**：JSON 对象，键=证书 ID（certId），值=PEM 公钥文本。
     * <pre>{"68759529225":"-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"}</pre>
     *
     * <p>与微信同理是「表」：银联回调报文里带 {@code certId} 指明用哪张证书验签，
     * 证书换发时新旧并存 —— 只配一张会在换发当天全部验签失败。</p>
     */
    private String unionPayCerts;
}
