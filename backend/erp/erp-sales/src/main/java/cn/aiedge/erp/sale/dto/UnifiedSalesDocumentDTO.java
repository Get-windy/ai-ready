package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一销售单据DTO - 对应销售单据查询页表格列（51个字段）
 * 文档要求字段：
 * 1单据日期、2单据编号、3单据类型、4入库仓库、5出库仓库、6客户、7客户编号、8客户级别、9收货人、10联系电话、
 * 11收货地址、12表头自定义字段1(数字)、13表头自定义字段2(数字)、14表头自定义字段3(文本)、15表头自定义字段4(文本)、
 * 16表头自定义字段5(文本)、17买家备注、18客户备注、19来源订单、20来源订单日期、21物流公司、22运单号、23区域、
 * 24产生方式、25经手人、26部门、27结算状态、28销售数量、29金额、30折后金额、31销售收入、32运费承担方、33运费、
 * 34其它费用、35抹零金额、36本单金额、37促销优惠、38优惠券优惠、39直接优惠、40积分抵扣、41成本金额、42毛利、
 * 43销售类型、44单据备注、45摘要、46附件、47记账人、48制单人、49记账时间、50制单时间、51打印次数
 */
@Data
@Schema(description = "统一销售单据DTO")
public class UnifiedSalesDocumentDTO {

    @Schema(description = "单据ID")
    private Long id;

    @Schema(description = "单据日期")
    private LocalDateTime documentDate;  // 1单据日期

    @Schema(description = "单据编号")
    private String documentNo;  // 2单据编号

    @Schema(description = "单据类型（SALE_ORDER, OUTBOUND, RETURN, EXCHANGE）")
    private String documentType;  // 3单据类型

    @Schema(description = "入库仓库")
    private String inboundWarehouse;  // 4入库仓库

    @Schema(description = "出库仓库")
    private String outboundWarehouse;  // 5出库仓库

    @Schema(description = "客户")
    private String customerName;  // 6客户

    @Schema(description = "客户编号")
    private String customerCode;  // 7客户编号

    @Schema(description = "客户级别")
    private String customerLevel;  // 8客户级别

    @Schema(description = "收货人")
    private String receiverName;  // 9收货人

    @Schema(description = "联系电话")
    private String receiverPhone;  // 10联系电话

    @Schema(description = "收货地址")
    private String shippingAddress;  // 11收货地址

    @Schema(description = "表头自定义字段1(数字)")
    private BigDecimal extNum1;  // 12表头自定义字段1(数字)

    @Schema(description = "表头自定义字段2(数字)")
    private BigDecimal extNum2;  // 13表头自定义字段2(数字)

    @Schema(description = "表头自定义字段3(文本)")
    private String extText1;  // 14表头自定义字段3(文本)

    @Schema(description = "表头自定义字段4(文本)")
    private String extText2;  // 15表头自定义字段4(文本)

    @Schema(description = "表头自定义字段5(文本)")
    private String extText3;  // 16表头自定义字段5(文本)

    @Schema(description = "买家备注")
    private String buyerRemark;  // 17买家备注

    @Schema(description = "客户备注")
    private String customerRemark;  // 18客户备注

    @Schema(description = "来源订单")
    private String sourceOrder;  // 19来源订单

    @Schema(description = "来源订单日期")
    private LocalDateTime sourceOrderDate;  // 20来源订单日期

    @Schema(description = "物流公司")
    private String logisticsCompany;  // 21物流公司

    @Schema(description = "运单号")
    private String trackingNumber;  // 22运单号

    @Schema(description = "区域")
    private String region;  // 23区域

    @Schema(description = "产生方式")
    private String generationMethod;  // 24产生方式

    @Schema(description = "经手人")
    private String handlerName;  // 25经手人

    @Schema(description = "部门")
    private String departmentName;  // 26部门

    @Schema(description = "结算状态")
    private String settlementStatus;  // 27结算状态

    @Schema(description = "销售数量")
    private BigDecimal salesQuantity;  // 28销售数量

    @Schema(description = "金额")
    private BigDecimal amount;  // 29金额

    @Schema(description = "折后金额")
    private BigDecimal discountedAmount;  // 30折后金额

    @Schema(description = "销售收入")
    private BigDecimal salesRevenue;  // 31销售收入

    @Schema(description = "运费承担方")
    private String freightPayer;  // 32运费承担方

    @Schema(description = "运费")
    private BigDecimal freight;  // 33运费

    @Schema(description = "其它费用")
    private BigDecimal otherFee;  // 34其它费用

    @Schema(description = "抹零金额")
    private BigDecimal roundingAmount;  // 35抹零金额

    @Schema(description = "本单金额")
    private BigDecimal totalAmount;  // 36本单金额

    @Schema(description = "促销优惠")
    private BigDecimal promoDiscount;  // 37促销优惠

    @Schema(description = "优惠券优惠")
    private BigDecimal couponAmount;  // 38优惠券优惠

    @Schema(description = "直接优惠")
    private BigDecimal directDiscount;  // 39直接优惠

    @Schema(description = "积分抵扣")
    private BigDecimal pointsDeduction;  // 40积分抵扣

    @Schema(description = "成本金额")
    private BigDecimal costAmount;  // 41成本金额

    @Schema(description = "毛利")
    private BigDecimal grossProfit;  // 42毛利

    @Schema(description = "销售类型")
    private String salesType;  // 43销售类型

    @Schema(description = "单据备注")
    private String remark;  // 44单据备注

    @Schema(description = "摘要")
    private String summary;  // 45摘要

    @Schema(description = "附件")
    private String attachment;  // 46附件

    @Schema(description = "记账人")
    private String bookkeeperName;  // 47记账人

    @Schema(description = "制单人")
    private String creatorName;  // 48制单人

    @Schema(description = "记账时间")
    private LocalDateTime bookkeepingTime;  // 49记账时间

    @Schema(description = "制单时间")
    private LocalDateTime createTime;  // 50制单时间

    @Schema(description = "打印次数")
    private Integer printCount;  // 51打印次数

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "经手人ID")
    private Long handlerId;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "结算方式")
    private String settlementMethod;

    @Schema(description = "源单类型")
    private String sourceOrderType;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "已付金额")
    private BigDecimal paidAmount;

    @Schema(description = "待付金额")
    private BigDecimal pendingAmount;

    @Schema(description = "预收金额")
    private BigDecimal advanceReceived;

    @Schema(description = "优惠后金额")
    private BigDecimal favorableAmount;

    @Schema(description = "代收货款")
    private BigDecimal codAmount;

    @Schema(description = "代收货款状态")
    private String codStatus;

    @Schema(description = "配送方式")
    private String deliveryMethod;

    @Schema(description = "配送司机")
    private String deliveryDriver;

    @Schema(description = "物流网点")
    private String logisticsBranch;

    @Schema(description = "预计发货时间")
    private LocalDateTime expectedShipmentTime;

    @Schema(description = "实际发货时间")
    private LocalDateTime actualShipmentTime;

    @Schema(description = "收货时间")
    private LocalDateTime receiptTime;

    @Schema(description = "审核人")
    private String approverName;

    @Schema(description = "审核时间")
    private LocalDateTime approvedTime;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "关闭时间")
    private LocalDateTime closedTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "完成人")
    private String completedBy;

    @Schema(description = "关闭人")
    private String closedBy;

    @Schema(description = "取消人")
    private String cancelledBy;

    @Schema(description = "单据来源")
    private String source;

    @Schema(description = "单据优先级")
    private Integer priority;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "税率")
    private BigDecimal taxRate;

    @Schema(description = "含税金额")
    private BigDecimal taxIncludedAmount;

    @Schema(description = "不含税金额")
    private BigDecimal taxExcludedAmount;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "汇率")
    private BigDecimal exchangeRate;

    @Schema(description = "外币金额")
    private BigDecimal foreignCurrencyAmount;

    @Schema(description = "付款条件")
    private String paymentTerms;

    @Schema(description = "付款期限")
    private String paymentDueDate;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票日期")
    private LocalDateTime invoiceDate;

    @Schema(description = "开票状态")
    private String invoiceStatus;

    @Schema(description = "发票类型")
    private String invoiceType;

    @Schema(description = "发票抬头")
    private String invoiceTitle;

    @Schema(description = "税务登记号")
    private String taxRegistrationNo;

    @Schema(description = "开票人")
    private String invoicerName;

    @Schema(description = "开票时间")
    private LocalDateTime invoiceTime;

    @Schema(description = "备注2")
    private String remark2;

    @Schema(description = "备注3")
    private String remark3;

    @Schema(description = "自定义字段1")
    private String customField1;

    @Schema(description = "自定义字段2")
    private String customField2;

    @Schema(description = "自定义字段3")
    private String customField3;

    @Schema(description = "自定义字段4")
    private String customField4;

    @Schema(description = "自定义字段5")
    private String customField5;

    @Schema(description = "自定义数值字段1")
    private BigDecimal customNumField1;

    @Schema(description = "自定义数值字段2")
    private BigDecimal customNumField2;

    @Schema(description = "自定义数值字段3")
    private BigDecimal customNumField3;

    @Schema(description = "自定义数值字段4")
    private BigDecimal customNumField4;

    @Schema(description = "自定义数值字段5")
    private BigDecimal customNumField5;

    @Schema(description = "自定义日期字段1")
    private LocalDateTime customDateField1;

    @Schema(description = "自定义日期字段2")
    private LocalDateTime customDateField2;

    @Schema(description = "自定义日期字段3")
    private LocalDateTime customDateField3;

    @Schema(description = "自定义日期字段4")
    private LocalDateTime customDateField4;

    @Schema(description = "自定义日期字段5")
    private LocalDateTime customDateField5;

    @Schema(description = "自定义布尔字段1")
    private Boolean customBoolField1;

    @Schema(description = "自定义布尔字段2")
    private Boolean customBoolField2;

    @Schema(description = "自定义布尔字段3")
    private Boolean customBoolField3;

    @Schema(description = "自定义布尔字段4")
    private Boolean customBoolField4;

    @Schema(description = "自定义布尔字段5")
    private Boolean customBoolField5;

    @Schema(description = "自定义对象字段1")
    private String customObjField1;

    @Schema(description = "自定义对象字段2")
    private String customObjField2;

    @Schema(description = "自定义对象字段3")
    private String customObjField3;

    @Schema(description = "自定义对象字段4")
    private String customObjField4;

    @Schema(description = "自定义对象字段5")
    private String customObjField5;
}