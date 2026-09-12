package cn.aiedge.erp.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付渠道查询条件（资料 → 支付管理 → 支付渠道）
 */
@Data
@Schema(description = "支付渠道查询条件")
public class PaymentChannelQuery {

    @Schema(description = "关键字：渠道编码/渠道名称/商户号 模糊匹配")
    private String keyword;

    @Schema(description = "渠道编码（精确匹配，供单据按编码回查）")
    private String channelCode;

    @Schema(description = "支付方式ID")
    private Long methodId;

    @Schema(description = "状态：1-启用 0-停用；不传=全部")
    private Integer status;

    @Schema(description = "页码，默认 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认 20")
    private Integer pageSize = 20;

    @Schema(description = "排序列：channelCode/channelName/sort/status/createTime")
    private String sortField;

    @Schema(description = "排序方向：asc/desc")
    private String sortOrder;
}
