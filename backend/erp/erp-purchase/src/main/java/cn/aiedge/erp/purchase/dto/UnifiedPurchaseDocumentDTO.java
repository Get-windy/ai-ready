package cn.aiedge.erp.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一采购单据DTO - 对应采购单据查询页表格列（37列）
 * 数据源：采购入库单(INBOUND) / 采购退货单(RETURN) / 采购换货单(EXCHANGE)
 * 字段按文档列序编号标注。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "统一采购单据DTO")
public class UnifiedPurchaseDocumentDTO {

    @Schema(description = "单据ID")
    private Long id;

    @Schema(description = "单据类型（INBOUND/RETURN/EXCHANGE）")
    private String documentType;

    // ═══ 1-11 单据基础 & 供应商信息 ═══

    @Schema(description = "1 单据日期")
    private LocalDate documentDate;

    @Schema(description = "2 单据编号")
    private String documentNo;

    @Schema(description = "4 入库仓库")
    private String inboundWarehouse;

    @Schema(description = "5 出库仓库")
    private String outboundWarehouse;

    @Schema(description = "6 供应商名称")
    private String supplierName;

    @Schema(description = "7 供应商编号")
    private String supplierCode;

    @Schema(description = "8 联系人")
    private String contactName;

    @Schema(description = "9 联系电话")
    private String contactPhone;

    @Schema(description = "10 联系地址")
    private String contactAddress;

    @Schema(description = "11 供应商备注")
    private String supplierRemark;

    // ═══ 12-15 业务信息 ═══

    @Schema(description = "12 来源订单")
    private String sourceOrder;

    @Schema(description = "13 经手人")
    private String handlerName;

    @Schema(description = "14 部门")
    private String departmentName;

    @Schema(description = "15 结算状态")
    private String settlementStatus;

    // ═══ 16-24 数量 & 金额 ═══

    @Schema(description = "16 采购数量")
    private BigDecimal purchaseQuantity;

    @Schema(description = "17 金额")
    private BigDecimal amount;

    @Schema(description = "18 折后金额")
    private BigDecimal discountedAmount;

    @Schema(description = "19 优惠后金额")
    private BigDecimal favorableAmount;

    @Schema(description = "20 税额")
    private BigDecimal taxAmount;

    @Schema(description = "21 价税合计")
    private BigDecimal totalAmountWithTax;

    @Schema(description = "22 本单金额")
    private BigDecimal totalAmount;

    @Schema(description = "23 其他费用")
    private BigDecimal fee;

    @Schema(description = "24 优惠金额")
    private BigDecimal discountAmount;

    // ═══ 25-29 表头自定义字段 ═══

    @Schema(description = "25 表头自定义字段1(数字)")
    private BigDecimal extNum1;

    @Schema(description = "26 表头自定义字段2(数字)")
    private BigDecimal extNum2;

    @Schema(description = "27 表头自定义字段3(文本)")
    private String extText1;

    @Schema(description = "28 表头自定义字段4(文本)")
    private String extText2;

    @Schema(description = "29 表头自定义字段5(文本)")
    private String extText3;

    // ═══ 30-37 备注 & 制单信息 ═══

    @Schema(description = "30 单据备注")
    private String remark;

    @Schema(description = "31 摘要")
    private String summary;

    @Schema(description = "32 附件")
    private String attachment;

    @Schema(description = "33 记账人")
    private String bookkeeperName;

    @Schema(description = "34 制单人")
    private String creatorName;

    @Schema(description = "35 记账时间")
    private LocalDateTime bookkeepingTime;

    @Schema(description = "36 制单时间")
    private LocalDateTime createTime;

    @Schema(description = "37 打印次数")
    private Integer printCount;

    // ═══ 额外字段（用于跳转/排序/内部逻辑） ═══

    @Schema(description = "单据状态")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "出库仓库ID")
    private Long outWarehouseId;

    @Schema(description = "经手人ID")
    private Long handlerId;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "重量(kg)")
    private BigDecimal weight;

    @Schema(description = "体积(m³)")
    private BigDecimal volume;

    @Schema(description = "源单类型")
    private String sourceOrderType;

    @Schema(description = "审核人")
    private String approverName;

    @Schema(description = "审核时间")
    private LocalDateTime approvedTime;

    // ═══ 按单付款核销工作台字段（采购单据作为应付来源单据） ═══

    @Schema(description = "结算单位")
    private String settlementUnit;

    @Schema(description = "运单号")
    private String waybillNo;

    @Schema(description = "付款日期")
    private LocalDate paymentDate;

    @Schema(description = "动态付款期限")
    private String dynamicPayTerm;

    @Schema(description = "固定账期")
    private String fixedTerms;

    @Schema(description = "结算期")
    private String settlePeriod;

    @Schema(description = "已结金额")
    private BigDecimal settledAmount;

    @Schema(description = "待审金额")
    private BigDecimal pendingApproveAmount;

    @Schema(description = "未结金额")
    private BigDecimal unsettledAmount;

    @Schema(description = "强制结算金额")
    private BigDecimal forceSettleAmount;

    @Schema(description = "发票号码")
    private String invoiceNumber;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Schema(description = "对账标记（true=√ false=否）")
    private Boolean reconcile;

    @Schema(description = "最后对账标记人")
    private String lastReconcileBy;

    @Schema(description = "最后对账标记时间")
    private LocalDateTime lastReconcileTime;
}
