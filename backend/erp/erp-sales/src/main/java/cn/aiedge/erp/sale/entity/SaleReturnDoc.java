package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售退货单实体
 */
@Data
@TableName("t_sale_return_doc")
public class SaleReturnDoc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentNo;  // 单据编号
    private LocalDateTime documentDate;  // 单据日期
    private String documentType;  // 单据类型
    private String inboundWarehouse;  // 入库仓库
    private String outboundWarehouse;  // 出库仓库
    private String customerName;  // 客户名称
    private String customerCode;  // 客户编号
    private String customerLevel;  // 客户级别
    private String receiverName;  // 收货人
    private String receiverPhone;  // 联系电话
    private String shippingAddress;  // 收货地址
    private BigDecimal extNum1;  // 表头自定义字段1(数字)
    private BigDecimal extNum2;  // 表头自定义字段2(数字)
    private String extText1;  // 表头自定义字段3(文本)
    private String extText2;  // 表头自定义字段4(文本)
    private String extText3;  // 表头自定义字段5(文本)
    private String buyerRemark;  // 买家备注
    private String customerRemark;  // 客户备注
    private String sourceOrder;  // 来源订单
    private LocalDateTime sourceOrderDate;  // 来源订单日期
    private String logisticsCompany;  // 物流公司
    private String trackingNumber;  // 运单号
    private String region;  // 区域
    private String generationMethod;  // 产生方式
    private String handlerName;  // 经手人
    private String departmentName;  // 部门
    private String settlementStatus;  // 结算状态
    private BigDecimal salesQuantity;  // 销售数量
    private BigDecimal amount;  // 金额
    private BigDecimal discountedAmount;  // 折后金额
    private BigDecimal salesRevenue;  // 销售收入
    private String freightPayer;  // 运费承担方
    private BigDecimal freight;  // 运费
    private BigDecimal otherFee;  // 其它费用
    private BigDecimal roundingAmount;  // 抹零金额
    private BigDecimal totalAmount;  // 本单金额
    private BigDecimal promoDiscount;  // 促销优惠
    private BigDecimal couponAmount;  // 优惠券优惠
    private BigDecimal directDiscount;  // 直接优惠
    private BigDecimal pointsDeduction;  // 积分抵扣
    private BigDecimal costAmount;  // 成本金额
    private BigDecimal grossProfit;  // 毛利
    private String salesType;  // 销售类型
    private String remark;  // 单据备注
    private String summary;  // 摘要
    private String attachment;  // 附件
    private String bookkeeperName;  // 记账人
    private String creatorName;  // 制单人
    private LocalDateTime bookkeepingTime;  // 记账时间
    private LocalDateTime createTime;  // 制单时间
    private Integer printCount;  // 打印次数

    private Integer status;
    private Long warehouseId;
    private Long customerId;
    private Long handlerId;
    private Long departmentId;
    private String settlementMethod;
    private String sourceOrderType;
    private BigDecimal orderAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private BigDecimal advanceReceived;
    private BigDecimal favorableAmount;
    private BigDecimal codAmount;
    private String codStatus;
    private String deliveryMethod;
    private String deliveryDriver;
    private String logisticsBranch;
    private LocalDateTime expectedShipmentTime;
    private LocalDateTime actualShipmentTime;
    private LocalDateTime receiptTime;
    private String approverName;
    private LocalDateTime approvedTime;
    private LocalDateTime completedTime;
    private LocalDateTime closedTime;
    private LocalDateTime cancelledTime;
    private String cancelReason;
    private String completedBy;
    private String closedBy;
    private String cancelledBy;
    private String source;
    private Integer priority;
    private String businessType;
    private Long projectId;
    private String projectName;
    private Long contractId;
    private String contractNo;
    private BigDecimal taxRate;
    private BigDecimal taxIncludedAmount;
    private BigDecimal taxExcludedAmount;
    private BigDecimal taxAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal foreignCurrencyAmount;
    private String paymentTerms;
    private String paymentDueDate;
    private String invoiceNo;
    private LocalDateTime invoiceDate;
    private String invoiceStatus;
    private String invoiceType;
    private String invoiceTitle;
    private String taxRegistrationNo;
    private String invoicerName;
    private LocalDateTime invoiceTime;
    private String remark2;
    private String remark3;
    private String customField1;
    private String customField2;
    private String customField3;
    private String customField4;
    private String customField5;
    private BigDecimal customNumField1;
    private BigDecimal customNumField2;
    private BigDecimal customNumField3;
    private BigDecimal customNumField4;
    private BigDecimal customNumField5;
    private LocalDateTime customDateField1;
    private LocalDateTime customDateField2;
    private LocalDateTime customDateField3;
    private LocalDateTime customDateField4;
    private LocalDateTime customDateField5;
    private Boolean customBoolField1;
    private Boolean customBoolField2;
    private Boolean customBoolField3;
    private Boolean customBoolField4;
    private Boolean customBoolField5;
    private String customObjField1;
    private String customObjField2;
    private String customObjField3;
    private String customObjField4;
    private String customObjField5;
}