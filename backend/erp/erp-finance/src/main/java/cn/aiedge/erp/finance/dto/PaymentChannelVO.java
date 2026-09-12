package cn.aiedge.erp.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付渠道列表/详情出参（资料 → 支付管理 → 支付渠道）
 */
@Data
@Schema(description = "支付渠道")
public class PaymentChannelVO {

    private Long id;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "支付方式ID")
    private Long methodId;

    @Schema(description = "支付方式编码（回填自 md_payment_method）")
    private String methodCode;

    @Schema(description = "支付方式名称（回填自 md_payment_method）")
    private String methodName;

    @Schema(description = "支付方式类型：CASH/BANK/WECHAT/ALIPAY/CHECK/OTHER")
    private String methodType;

    @Schema(description = "支付方式类型文本")
    private String methodTypeText;

    @Schema(description = "商户号")
    private String merchantNo;

    @Schema(description = "渠道扩展配置（JSON）")
    private String configJson;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "备注")
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
