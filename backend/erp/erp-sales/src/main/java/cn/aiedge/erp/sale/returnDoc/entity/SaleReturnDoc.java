package cn.aiedge.erp.sale.returnDoc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售退货单
 *
 * <p>字段按业务实体分组（字段实体溯源）：</p>
 * <ul>
 *   <li>客户快照（来源: erp_customer）：customerId, customerName, customerCode, customerLevel,
 *       contactName, contactPhone, contactAddress, customerRemark, customerTicket,
 *       bankName, bankAccount, taxNo</li>
 *   <li>仓库快照（来源: erp_warehouse）：warehouseId, warehouseName</li>
 *   <li>职员/部门快照（来源: sys_user/sys_dept）：handlerId, handlerName, deptId, deptName,
 *       auditor, auditorId, auditorName, submitBy, creatorName, createBy, bookkeeperName</li>
 *   <li>付款/信用：creditLimit, availableCredit, prevDebt, currentDebt, debtBalance,
 *       collectionDeadline, paymentAccount1-4, settledAmount, prevAdvance, returnAdvance,
 *       availableAdvance, advanceBalance, receivableReduce</li>
 *   <li>物流（来源: erp_delivery）：deliveryMethod, deliveryRoute, deliveryRouteId,
 *       deliveryOrderNo, waybillNo, logisticsCompany, shippingFee, freightPayer,
 *       codAmount, driverName, driverId, deliveryVehicle</li>
 *   <li>会员/积分：memberCardNo, memberName, memberDiscount, prevPoints,
 *       memberGeneratedPoints, memberExchangePoints, memberUsedPoints, currentPoints</li>
 *   <li>金额计算链：productAmount, promoDiscount, couponAmount, directDiscount,
 *       discountAmount, otherFee, billAmount, totalAmount, discountBillAmount</li>
 *   <li>数量汇总：totalQuantity, returnQuantityTotal, productLineCount</li>
 *   <li>物理属性汇总：totalWeight, totalVolume</li>
 *   <li>源单关联：sourceOrder, sourceOrderId, returnApplyId, returnApplyNo, generateType</li>
 *   <li>结算：settleStatus, paymentDate, reconciliationDate, bookkeepingTime</li>
 *   <li>自定义字段（表头）：extNum1-5, extText1-5, extPartner, extStaff, extDept</li>
 *   <li>自定义字段（表尾）：footerExtText1-2</li>
 * </ul>
 */
@Data
@TableName("erp_sale_return_doc")
public class SaleReturnDoc {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    // 单据基本信息
    private String returnDocNo;
    private LocalDateTime orderDate;
    private String salesType;
    private Integer status;
    private String generateType;
    private String settleStatus;
    private Integer printCount;
    private String attachment;

    // 客户快照
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
    private String customerRemark;
    private String customerTicket;
    private String bankName;
    private String bankAccount;
    private String taxNo;

    // 仓库/职员/部门快照
    private Long warehouseId;
    private String warehouseName;
    private Long handlerId;
    private String handlerName;
    private Long deptId;
    private String deptName;

    // 收货信息
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String region;

    // 金额计算链
    private BigDecimal productAmount;
    private BigDecimal promoDiscount;
    private BigDecimal couponAmount;
    private BigDecimal directDiscount;
    private BigDecimal discountAmount;
    private BigDecimal otherFee;
    private BigDecimal billAmount;
    private BigDecimal totalAmount;
    private BigDecimal discountBillAmount;
    private BigDecimal settledAmount;

    // 数量汇总
    private BigDecimal totalQuantity;
    private BigDecimal returnQuantityTotal;
    private Integer productLineCount;

    // 物理属性汇总
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    // 付款/预收/信用
    private String paymentAccount1;
    private String paymentAccount2;
    private String paymentAccount3;
    private String paymentAccount4;
    private BigDecimal prevAdvance;
    private BigDecimal returnAdvance;
    private BigDecimal availableAdvance;
    private BigDecimal advanceBalance;
    private BigDecimal receivableReduce;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal prevDebt;
    private BigDecimal currentDebt;
    private BigDecimal debtBalance;
    private String collectionDeadline;

    // 结算日期
    private LocalDateTime paymentDate;
    private LocalDateTime reconciliationDate;
    private LocalDateTime bookkeepingTime;

    // 物流信息
    private String deliveryMethod;
    private String deliveryRoute;
    private Long deliveryRouteId;
    private String deliveryOrderNo;
    private String deliveryNo;
    private String waybillNo;
    private String logisticsCompany;
    private BigDecimal shippingFee;
    private String freightPayer;
    private BigDecimal codAmount;
    private String driverName;
    private Long driverId;
    private String deliveryVehicle;

    // 会员/积分
    private String memberCardNo;
    private String memberName;
    private BigDecimal memberDiscount;
    private BigDecimal prevPoints;
    private BigDecimal memberGeneratedPoints;
    private BigDecimal memberExchangePoints;
    private BigDecimal memberUsedPoints;
    private BigDecimal currentPoints;

    // 源单关联
    private String sourceOrder;
    private Long sourceOrderId;
    private Long returnApplyId;
    private String returnApplyNo;

    // 退货信息
    private Integer returnType;
    private String reason;

    // 自定义字段（表头-数字）
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;

    // 自定义字段（表头-文本）
    private String extText1;
    private String extText2;
    private String extText3;
    private String extText4;
    private String extText5;

    // 自定义字段（表头-关联）
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // 自定义字段（表尾）
    private String footerExtText1;
    private String footerExtText2;

    // 其他
    private String summary;
    private String productLineAttr;
    private String remark;
    private String internalNote;
    private String buyerRemark;

    // 审批相关
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String approvedNote;
    private String auditor;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditTime;

    // 提交相关
    private Long submitBy;
    private LocalDateTime submitTime;

    // 打印时间
    private LocalDateTime printTime;

    // 标准审计字段
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    private String creatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private String updater;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    // 非数据库字段 - 明细列表
    @TableField(exist = false)
    private List<SaleReturnDocItem> items;

    // 非数据库字段 - 产品名称（用于明细tab搜索）
    @TableField(exist = false)
    private String productName;

    // 非数据库字段 - 明细备注（用于明细tab搜索）
    @TableField(exist = false)
    private String itemRemark;
}
