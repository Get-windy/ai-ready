package cn.aiedge.erp.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付渠道新增/修改入参（资料 → 支付管理 → 支付渠道）
 */
@Data
@Schema(description = "支付渠道保存入参")
public class PaymentChannelDTO {

    @Schema(description = "渠道编码（新增必填；修改时不允许变更）")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "关联支付方式ID → md_payment_method.id")
    private Long methodId;

    @Schema(description = "商户号（微信商户号/支付宝PID等）")
    private String merchantNo;

    @Schema(description = "渠道扩展配置（JSON，如 app_id/api_key/回调地址）")
    private String configJson;

    @Schema(description = "排序号（升序）")
    private Integer sort;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
