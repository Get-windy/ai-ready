package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账款交账 - 按单据视图行 VO
 * 数据源：销售出库单/退货单（职员经手代收待交账）
 * 对齐对标 19 可配列 + 派生冻结列（收款账户分组 / 原单详情分组）
 */
@Data
@Schema(description = "账款交账 按单据视图行VO")
public class AccountDeliveryDocVO {

    private Long id;

    @Schema(description = "来源单据类型（OUTBOUND/RETURN）")
    private String sourceType;

    // ═══ 基础 19 列 ═══
    @Schema(description = "1 账款确认（√/否）")
    private Boolean confirmFlag;

    @Schema(description = "2 业务日期")
    private LocalDate bizDate;

    @Schema(description = "3 单据编号（扫码定位）")
    private String documentNo;

    @Schema(description = "4 结款方式")
    private String settlementMethod;

    @Schema(description = "5 客户")
    private String customerName;

    @Schema(description = "6 交账职员")
    private String deliverStaff;

    @Schema(description = "7 账款类型")
    private String deliveryType;

    @Schema(description = "8 交账状态")
    private String deliverStatus;

    @Schema(description = "9 业务类型")
    private String businessType;

    @Schema(description = "10 单据类型")
    private String documentType;

    @Schema(description = "11 配送任务编号")
    private String deliveryTaskNo;

    @Schema(description = "12 结算单位")
    private String settlementUnit;

    @Schema(description = "13 业务经手人")
    private String handlerName;

    @Schema(description = "14 应交金额")
    private BigDecimal dueAmount;

    @Schema(description = "15 收款优惠")
    private BigDecimal favorableAmount;

    @Schema(description = "16 欠款金额")
    private BigDecimal arrearsAmount;

    @Schema(description = "17 收款单详情")
    private String receiptDetail;

    @Schema(description = "18 确认交账人")
    private String confirmStaff;

    @Schema(description = "19 确认交账时间")
    private LocalDateTime confirmTime;

    // ═══ 收款账户分组（派生冻结列） ═══
    @Schema(description = "收款账户1")
    private String paymentAccount1;

    @Schema(description = "收款账户2")
    private String paymentAccount2;

    @Schema(description = "收款账户3")
    private String paymentAccount3;

    @Schema(description = "收款账户4")
    private String paymentAccount4;

    @Schema(description = "使用预收（预收抵扣部分）")
    private BigDecimal usedAdvance;

    @Schema(description = "收款合计（实际现金/转账收取部分）")
    private BigDecimal receiveTotal;

    // ═══ 原单详情分组（派生冻结列） ═══
    @Schema(description = "发货数量")
    private BigDecimal shipQty;

    @Schema(description = "发货金额")
    private BigDecimal shipAmount;

    @Schema(description = "签收数量")
    private BigDecimal receiveQty;

    @Schema(description = "签收金额")
    private BigDecimal receiveAmount;

    @Schema(description = "拒收数量")
    private BigDecimal rejectQty;

    @Schema(description = "拒收金额")
    private BigDecimal rejectAmount;

    @Schema(description = "退货数量")
    private BigDecimal returnQty;

    @Schema(description = "退货金额")
    private BigDecimal returnAmount;

    @Schema(description = "已收订金")
    private BigDecimal orderDeposit;

    @Schema(description = "待确认订金")
    private BigDecimal pendingDeposit;

    @Schema(description = "原单使用预收款")
    private BigDecimal originalAdvance;

    @Schema(description = "优惠")
    private BigDecimal discountAmount;

    @Schema(description = "其他费用")
    private BigDecimal otherFee;

    @Schema(description = "附件")
    private String attachments;

    @Schema(description = "单据备注")
    private String remark;

    @Schema(description = "部门")
    private String departmentName;

    @Schema(description = "收款日期")
    private LocalDate paymentDate;

    @Schema(description = "对账日期")
    private LocalDate reconciliationDate;

    @Schema(description = "业务员ID")
    private Long salesPersonId;
}
