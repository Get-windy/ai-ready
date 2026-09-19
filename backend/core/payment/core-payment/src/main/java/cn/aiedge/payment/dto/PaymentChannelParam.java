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
}
