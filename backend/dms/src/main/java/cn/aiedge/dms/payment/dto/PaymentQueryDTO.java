package cn.aiedge.dms.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收款管理查询条件（台账 / 未付 / 统计共用，字段名与列表接口 query 对齐）
 */
@Data
@Schema(description = "收款管理查询条件")
public class PaymentQueryDTO {

    @Schema(description = "页码（从 1 开始）")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 20L;

    @Schema(description = "任务编号（模糊）")
    private String taskNo;

    @Schema(description = "客户名称（模糊）")
    private String customerName;

    @Schema(description = "关键词：任务编号 / 客户 / 配送员 / 平台交易号 / 外部单号")
    private String keyword;

    @Schema(description = "收款类型 1-代收货款 2-配送费")
    private Integer paymentType;

    @Schema(description = "支付方式 1-微信 2-支付宝 3-现金 4-POS 5-银行转账 9-其他")
    private Integer payChannel;

    @Schema(description = "支付状态 0-待支付 1-已支付 2-已退款 3-未付(挂账)")
    private Integer status;

    @Schema(description = "交款状态 0-未交 1-部分交 2-已交")
    private Integer handoverStatus;

    @Schema(description = "配送员ID")
    private Long riderId;

    @Schema(description = "创建日期-起（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "创建日期-止（yyyy-MM-dd）")
    private String endDate;

    @Schema(description = "仅看交款超时（已支付未交清，且超过交款时限）")
    private Boolean overdueOnly;
}
