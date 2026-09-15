package cn.aiedge.erp.sale.salereturn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售退货申请单
 *
 * <p>字段按业务实体分组（字段实体溯源）：</p>
 * <ul>
 *   <li>客户快照（来源: erp_customer）：customerId, customerName, customerCode, customerLevel,
 *       contactName, contactPhone, contactAddress, customerRemark, customerTicket,
 *       bankName, bankAccount, taxNo</li>
 *   <li>仓库快照（来源: erp_warehouse）：warehouseId, warehouseName</li>
 *   <li>职员/部门快照（来源: sys_user/sys_dept）：handlerId, handlerName, deptId, deptName,
 *       auditor, auditorId, auditorName, submitBy, creatorName, createBy</li>
 *   <li>付款/信用：creditLimit, availableCredit, currentDebt, prevDebt, debtBalance,
 *       collectionDeadline, paymentDate, reconciliationDate, settlementMethod,
 *       paymentAccount1-4, settledAmount</li>
 *   <li>物流（来源: erp_delivery）：deliveryMethod, deliveryRoute, deliveryRouteId,
 *       deliveryOrderNo, waybillNo, logisticsCompany, logisticsNo, shippingFee,
 *       freightPayer, codAmount, driverName, driverId, deliveryVehicle, deliveryNo</li>
 *   <li>会员/积分：memberCardNo, memberName, memberDiscount, prevPoints,
 *       memberGeneratedPoints, memberExchangePoints, memberUsedPoints, currentPoints</li>
 *   <li>金额计算链：productAmount, promoDiscount, couponAmount, directDiscount,
 *       discountAmount, otherFee, billAmount, totalQuantity, totalAmount</li>
 *   <li>数量汇总：orderedQuantity, receivedQuantity, unreceivedQuantity, returnQuantityTotal</li>
 *   <li>物理属性汇总：totalWeight, totalVolume</li>
 *   <li>源单关联：sourceOrder, sourceOrderId, generateType, deliveryOrderId</li>
 *   <li>自定义字段（表头）：extNum1-5, extText1-5, extPartner, extStaff, extDept</li>
 *   <li>自定义字段（表尾）：footerExtText1-2</li>
 * </ul>
 *
 * <p>设计说明：采用快照冗余设计（宽表），查询性能优先。
 * 未来如需规范化，可将快照字段拆分至关联表，通过 JOIN 查询。</p>
 */
@Data
@TableName("erp_sale_return")
public class SaleReturn {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String returnNo;

    private Long customerId;

    private String customerName;

    private String customerCode;

    private String customerLevel;

    private String contactName;

    private String contactPhone;

    private String contactAddress;

    private String customerRemark;

    private Long warehouseId;

    private String warehouseName;

    private Long handlerId;

    private String handlerName;

    private String deptName;

    private LocalDateTime orderDate;

    private Integer returnType;

    private String expectedReceiveDate;

    private String auditor;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private Integer status;

    private Integer printCount;

    private String generateType;

    private String settleStatus;

    private String reason;

    private String remark;

    // 付款 tab
    private BigDecimal currentDebt;

    private BigDecimal prevDebt;

    private BigDecimal debtBalance;

    private String collectionDeadline;

    private String sourceOrder;

    // 物流信息 tab
    private String logisticsCompany;

    private String logisticsNo;

    private BigDecimal shippingFee;

    // 会员信息 tab
    private String memberCardNo;

    private String memberName;

    private BigDecimal memberDiscount;

    // 审批相关字段
    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    // 银行/税务快照
    private String bankName;

    private String bankAccount;

    private String taxNo;

    // 客户一票通
    private String customerTicket;

    // 部门
    private Long deptId;

    // 区域
    private String region;

    // 收货信息快照
    private String receiverName;

    private String receiverPhone;

    private String shippingAddress;

    // 附件
    private String attachment;

    // 金额计算链
    private BigDecimal productAmount;

    private BigDecimal promoDiscount;

    private BigDecimal couponAmount;

    private BigDecimal directDiscount;

    private BigDecimal discountAmount;

    private BigDecimal otherFee;

    private BigDecimal billAmount;

    /** 折后金额（商品金额 − 优惠金额），对齐销售退货单口径；本单金额 = 折后金额 + 运费 + 其他费用 */
    private BigDecimal discountBillAmount;

    private BigDecimal settledAmount;

    private String freightPayer;

    // 数量汇总
    private BigDecimal orderedQuantity;

    private BigDecimal receivedQuantity;

    private BigDecimal unreceivedQuantity;

    private BigDecimal returnQuantityTotal;

    // 物理属性汇总
    private BigDecimal totalWeight;

    private BigDecimal totalVolume;

    // 结算方式
    private String settlementMethod;

    // 收款账户
    private String paymentAccount1;

    private String paymentAccount2;

    private String paymentAccount3;

    private String paymentAccount4;

    // 信用额度
    private BigDecimal creditLimit;

    private BigDecimal availableCredit;

    // 物流扩展
    private String deliveryMethod;

    private String deliveryRoute;

    private Long deliveryRouteId;

    private String deliveryOrderNo;

    private String waybillNo;

    private BigDecimal codAmount;

    private String driverName;

    private Long driverId;

    private String deliveryVehicle;

    // 会员/积分扩展
    private BigDecimal prevPoints;

    private BigDecimal memberGeneratedPoints;

    private BigDecimal memberExchangePoints;

    private BigDecimal memberUsedPoints;

    private BigDecimal currentPoints;

    // 源单关联
    private Long sourceOrderId;

    private Long deliveryOrderId;

    // 表头自定义字段（数字）
    private BigDecimal extNum1;

    private BigDecimal extNum2;

    private BigDecimal extNum3;

    private BigDecimal extNum4;

    private BigDecimal extNum5;

    // 表头自定义字段（文本）
    private String extText1;

    private String extText2;

    private String extText3;

    private String extText4;

    private String extText5;

    // 表头自定义字段（往来单位/职员/部门）
    private Long extPartner;

    private Long extStaff;

    private Long extDept;

    // 表尾自定义字段
    private String footerExtText1;

    private String footerExtText2;

    // 备注
    private String internalNote;

    private String buyerRemark;

    // 摘要
    private String summary;

    // 销售类型
    private String salesType;

    // 商品行属性
    private String productLineAttr;

    // 配送单
    private String deliveryNo;

    // 提交/审核扩展
    private Long submitBy;

    private LocalDateTime submitTime;

    private Long auditorId;

    private String auditorName;

    private LocalDateTime auditTime;

    // 收款日/对账日
    private LocalDateTime paymentDate;

    private LocalDateTime reconciliationDate;

    // 记账/打印时间
    private LocalDateTime bookkeepingTime;

    private LocalDateTime printTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    private String creatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    // 非数据库字段 - 明细列表
    @TableField(exist = false)
    private List<SaleReturnItem> items;

    // 非数据库字段 - 产品名称（用于明细tab搜索）
    @TableField(exist = false)
    private String productName;

    // 非数据库字段 - 明细备注（用于明细tab搜索）
    @TableField(exist = false)
    private String itemRemark;

    // 非数据库字段 - 分类ID（用于明细tab搜索）
    @TableField(exist = false)
    private Long categoryId;

    // 非数据库字段 - 提交人姓名（列表「提交人」列，由 submit_by 解析）
    @TableField(exist = false)
    private String submitByName;

    // 非数据库字段 - 商品行数（列表「商品行数」列，明细行计数）
    @TableField(exist = false)
    private Integer lineCount;
}
