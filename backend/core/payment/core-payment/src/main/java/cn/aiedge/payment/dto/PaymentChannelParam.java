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

    @Schema(description = "【验签】微信支付平台证书序列号（对应回调头 Wechatpay-Serial）")
    private String wechatPlatformSerial;

    @Schema(description = "【验签】微信支付平台证书公钥（PEM 文本）")
    private String wechatPlatformPublicKey;
}
