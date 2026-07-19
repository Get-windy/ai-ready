package cn.aiedge.erp.sale.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售出库单主表实体
 * 遵循主从表快照模型：存外键ID + 快照字段（成交后锁定不变）
 * 预留 ext_num/ext_text/ext_partner/ext_staff/ext_dept 自定义扩展字段
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_outbound")
public class SaleOutbound {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    // ═══ 单据基本信息 ═══
    private String outboundNo;       // 出库单号（XSCKD-YYYYMMDD-NNNN）
    private Long orderId;            // 来源订单ID
    private String orderNo;          // 来源订单编号
    private LocalDate outboundDate;  // 单据日期
    private Integer outboundType;    // 销售类型（0正常/1换货/2调拨/3其他）
    private Integer status;          // 状态（0草稿→1待审批→2已审批→...→11已完成→12已取消）
    private String generationMethod; // 产生方式（手工/订单生成/复制等）
    private String summary;          // 摘要

    // ═══ 客户快照 ═══
    private Long customerId;
    private String customerName;
    private String customerCode;     // 客户编号
    private String customerLevel;    // 客户级别
    private Long contactId;
    private String contactName;
    private String customerRemark;   // 客户备注

    // ══ 银行/税务 ═══
    private String bankName;
    private String bankAccount;
    private String taxNo;

    // ═══ 仓库/经手人 ═══
    private Long warehouseId;
    private String warehouseName;
    private Long salesPersonId;
    private String salesPersonName;
    private Long departmentId;
    private String departmentName;
    private String location;         // 点位
    private String region;           // 区域

    // ═══ 收货信息 ═══
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    // ═══ 数量/金额 ═══
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;        // 商品金额
    private BigDecimal promoDiscount;      // 促销优惠
    private BigDecimal couponAmount;       // 优惠券
    private BigDecimal directDiscount;     // 直接优惠
    private BigDecimal otherFee;           // 其他费用
    private BigDecimal roundingAmount;     // 抹零金额
    private BigDecimal totalWeight;        // 总重量(kg)
    private BigDecimal totalVolume;        // 总体积(m³)
    private BigDecimal returnQuantity;     // 退货数量
    private BigDecimal returnAmount;       // 退货金额
    private Integer boxCount;              // 商品行数/箱数

    // ═══ 结算 ═══
    private String settlementMethod;
    private BigDecimal settledAmount;
    private String settlementStatus;

    // ═══ 预收款 ═══
    private BigDecimal advancePaymentAmount;      // 使用预订货款
    private BigDecimal prevAdvancePayment;        // 此前预收
    private BigDecimal usedAdvancePayment;        // 使用预收款
    private BigDecimal orderDeposit;              // 订单已收订金
    private BigDecimal availableAdvancePayment;   // 可用预收
    private BigDecimal advancePaymentBalance;     // 预收余额

    // ═══ 信用 ═══
    private BigDecimal creditLimit;               // 信用额度
    private BigDecimal availableCredit;           // 可用额度
    private BigDecimal prevArrears;               // 此前欠款
    private BigDecimal currentArrears;            // 本次欠款
    private BigDecimal arrearsBalance;            // 欠款余额

    // ═══ 收款账户（4个） ═══
    private String paymentAccount1;
    private String paymentAccount2;
    private String paymentAccount3;
    private String paymentAccount4;

    // ═══ 物流 ══
    private String deliveryMethod;         // 配送方式
    private String logisticsCompany;
    private String logisticsBranch;        // 物流网点
    private String freightPayer;           // 运费承担方
    private BigDecimal freight;            // 运费
    private String trackingNumber;         // 运单号
    private String waybillNo;              // 运单号（兼容）
    private BigDecimal codAmount;          // 物流公司代收货款
    private String deliveryOrderNo;        // 配送单号
    private String deliveryDriver;         // 配送司机

    // ═══ 流程时间/人员 ═══
    private LocalDateTime expectedShipTime;
    private LocalDateTime actualShipTime;
    private Long pickingBy;
    private LocalDateTime pickingTime;
    private Long packingBy;
    private LocalDateTime packingTime;
    private Long shippedBy;
    private LocalDateTime shippedTime;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String approvedNote;
    private Long completedBy;
    private LocalDateTime completedTime;

    // ═══ 会员/积分 ═══
    private String memberCardNo;
    private BigDecimal prevPoints;           // 此前积分
    private BigDecimal memberGeneratedPoints;// 产生积分
    private BigDecimal memberExchangePoints; // 兑换积分
    private BigDecimal memberUsedPoints;     // 使用积分
    private BigDecimal currentPoints;        // 当前积分

    // ═══ 收款日/对账日 ═══
    private LocalDate paymentDate;
    private LocalDate reconciliationDate;

    // ══ 备注 ═══
    private String remark;           // 单据备注
    private String internalNote;     // 内部备注
    private String buyerRemark;      // 买家备注

    // ═══ 列表页所需字段 ═══
    private String bookkeeperName;
    private String creatorName;
    private String auditorName;
    private Integer printCount;
    private LocalDateTime bookkeepingTime;   // 记账时间
    private LocalDateTime printTime;         // 打印时间

    // ═══ 表头自定义字段（数字 1-5） ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;

    // ═══ 表头自定义字段（文本 1-5） ═══
    private String extText1;
    private String extText2;
    private String extText3;
    private String extText4;
    private String extText5;

    // ═══ 表头自定义字段（往来单位/职员/部门） ═══
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // ═══ 表尾自定义字段 ═══
    private String footerExtText1;
    private String footerExtText2;

    // ═══ 系统字段 ═══
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
