package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单实体 - 核心字段版 (25字段)
 * 对标 Odoo/SAP/金蝶/用友 生产级 ERP 规范
 *
 * 客户来源: ERP 往来单位 (biz_party)，非 CRM 公海客户
 *
 * 子表分离:
 * - 往来单位快照: erp_sale_order_partner_snapshot (1:1)
 * - 收货地址: erp_sale_order_delivery_address (1:N)
 * - 结算信息: erp_sale_order_settlement (1:1)
 * - 物流信息: erp_sale_order_logistics (1:N)
 * - 订金账户: erp_sale_order_deposit (1:N)
 * - 会员积分: erp_sale_order_points_journal (1:1)
 * - 审核流水: erp_sale_order_audit_trail (1:N)
 * - 扩展信息: erp_sale_order_ext_info (1:1)
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order")
public class SaleOrder {

    /** 订单ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 单据编号 */
    private String orderNo;

    /** 单据日期 */
    private LocalDate orderDate;

    /** 销售类型（1-正常 2-样品 3-促销） */
    private Integer saleType;

    /** 单据状态（0-草稿 1-待审核 2-待发货 3-部分发货 4-发货完成 5-交易完成 6-已取消） */
    private Integer status;

    // ═══ 外键关联 (只存ID，名称通过JOIN查询) ═══

    /** 往来单位ID (ERP biz_party) */
    private Long customerId;

    /** 发货仓库ID */
    private Long warehouseId;

    /** 经手人ID */
    private Long salesmanId;

    /** 部门ID */
    private Long deptId;

    // ═══ 金额汇总 ═══

    /** 商品金额 */
    private BigDecimal productAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 本单金额 */
    private BigDecimal billAmount;

    /** 已结金额 */
    private BigDecimal settledAmount;

    /** 已收款金额 */
    private BigDecimal receivedAmount;

    // ═══ 数量汇总 ═══

    /** 订货数量 */
    private BigDecimal totalQuantity;

    // ═══ 业务信息 ═══

    /** 预计发货时间 */
    private LocalDateTime expectedShipTime;

    /** 补单类型 */
    private String supplementType;

    /** 产生方式 */
    private String generationMethod;

    /** 源单 */
    private String sourceOrder;

    /** 订单来源（1-内部 2-B2B 3-B2C 4-H5 5-小程序） */
    private Integer orderSource;

    /** 备注 */
    private String remark;

    /** 买家备注 */
    private String buyerRemark;

    /** 单据备注(卖家备注) */
    private String orderRemark;

    // ═══ 履约关联 ═══

    /** 原始订单ID(补单关联) */
    private Long originalOrderId;

    /** 原始订单编号 */
    private String originalOrderNo;

    // ═══ 客户快照字段 ═══

    /** 客户名称 (冗余自 biz_party.party_name) */
    private String customerName;

    /** 客户编号 (冗余自 biz_party) */
    private String customerCode;

    /** 客户级别 (冗余自 biz_party) */
    private String customerLevel;

    /** 客户备注 (冗余自 biz_party) */
    private String customerRemark;

    /** 客户一票通 */
    private String customerTicket;

    /** 银行名称 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 税号 */
    private String taxNo;

    // ═══ 仓库/经手人名称快照 ═══

    /** 发货仓库名称 (冗余) */
    private String warehouseName;

    /** 经手人名称 (冗余) */
    private String salesmanName;

    /** 部门名称 (冗余) */
    private String deptName;

    /** 推广人ID */
    private Long promoterId;

    /** 推广人名称 */
    private String promoterName;

    // ═══ 收货信息 ═══

    /** 收货人名称 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String shippingAddress;

    // ═══ 收货/提货信息 ══

    /** 联系人名称 (提货) */
    private String contactName;

    /** 联系人电话 (提货) */
    private String contactPhone;

    /** 提货地址 */
    private String pickupAddress;

    // ═══ 结款/配送 ═══

    /** 结款方式 */
    private String settlementMethod;

    /** 配送方式 */
    private String deliveryMethod;

    /** 配送线路 */
    private String deliveryRoute;

    /** 配送线路ID */
    private Long deliveryRouteId;

    /** 司机ID */
    private Long driverId;

    /** 司机名称 */
    private String driverName;

    /** 配送车辆 */
    private String deliveryVehicle;

    // ═══ 物流 ═══

    /** 运费承担方 */
    private String freightPayer;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 物流公司 */
    private String logisticsCompany;

    /** 运单号 */
    private String waybillNo;

    /** 代收货款 */
    private BigDecimal codAmount;

    // ═══ 金额细分 ═══

    /** 促销优惠 */
    private BigDecimal promoDiscount;

    /** 优惠券金额 */
    private BigDecimal couponAmount;

    /** 直接优惠 */
    private BigDecimal directDiscount;

    /** 其他费用 */
    private BigDecimal otherFee;

    // ═══ 订金/预收 ═══

    /** 订金账户 */
    private String depositAccount;

    /** 订金金额 */
    private BigDecimal depositAmount;

    /** 此前预收 */
    private BigDecimal prevAdvance;

    /** 预收余额 */
    private BigDecimal advanceBalance;

    /** 订金账户1 */
    private String depositAccount1;

    /** 订金账户2 */
    private String depositAccount2;

    /** 订金账户3 */
    private String depositAccount3;

    /** 订金账户4 */
    private String depositAccount4;

    // ═══ 信用额度 ═══

    /** 信用额度 */
    private BigDecimal creditLimit;

    /** 可用额度 */
    private BigDecimal availableCredit;

    /** 此前欠款 */
    private BigDecimal prevDebt;

    // ═══ 收款日/对账日 ═══

    /** 收款日 */
    private LocalDate paymentDate;

    /** 对账日 */
    private LocalDate reconciliationDate;

    // ═══ 会员/积分 ══

    /** 会员卡号 */
    private String memberCardNo;

    /** 会员名称 */
    private String memberName;

    /** 会员折扣 */
    private Integer memberDiscount;

    /** 此前积分 */
    private BigDecimal prevPoints;

    /** 销售积分 */
    private BigDecimal salePoints;

    /** 退货积分 */
    private BigDecimal returnPoints;

    /** 兑换积分 */
    private BigDecimal exchangePoints;

    /** 使用积分 */
    private BigDecimal usedPoints;

    /** 当前积分 */
    private BigDecimal currentPoints;

    // ═══ 数量汇总 ══

    /** 已发数量 */
    private BigDecimal shippedQuantity;

    /** 未发数量 */
    private BigDecimal unshippedQuantity;

    /** 退货数量 */
    private BigDecimal returnQuantity;

    /** 退货金额 */
    private BigDecimal returnAmount;

    // ═══ 物理属性汇总 ═══

    /** 总重量(kg) */
    private BigDecimal totalWeight;

    /** 总体积(m³) */
    private BigDecimal totalVolume;

    // ═══ 备注/摘要/扩展 ═══

    /** 摘要 */
    private String summary;

    /** 区域 */
    private String region;

    /** 附件 */
    private String attachment;

    /** 自定义字段1(数字) */
    private BigDecimal extNum1;

    /** 自定义字段2(数字) */
    private BigDecimal extNum2;

    /** 自定义字段3(文本) */
    private String extText1;

    /** 自定义字段4(文本) */
    private String extText2;

    /** 自定义字段5(文本) */
    private String extText3;

    /** 自定义字段6(文本) */
    private String extText4;

    /** 自定义字段7(文本) */
    private String extText5;

    /** 表尾自定义字段1 */
    private String footerExtText1;

    /** 表尾自定义字段2 */
    private String footerExtText2;

    // ═══ 审核信息 ═══

    /** 审核人ID */
    private Long auditorId;

    /** 审核人名称 */
    private String auditorName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    // ═══ 提交信息 ═══

    /** 提交人ID */
    private Long submitterId;

    /** 提交人名称 */
    private String submitterName;

    /** 提交时间 */
    private LocalDateTime submitTime;

    // ═══ 制单/打印 ═══

    /** 打印次数 */
    private Integer printCount;

    /** 制单时间 */
    private LocalDateTime bookkeepingTime;

    /** 制单人名称 */
    private String creatorName;

    // ═══ 第三方/来源 ═══

    /** 第三方单号 */
    private String thirdPartyOrderNo;

    /** 商品品牌 */
    private String productBrand;

    /** 所属行业类别 */
    private String industryCategory;

    // ═══ 补单/履约 ═══

    /** 补单状态 */
    private String supplementStatus;

    /** 已发订单编号 */
    private String shippedOrderNo;

    /** 原单金额 */
    private BigDecimal originalAmount;

    /** 剩余未发金额 */
    private BigDecimal remainingUnshippedAmount;

    /** 原单优惠 */
    private BigDecimal originalDiscount;

    /** 原单商品项数 */
    private Integer originalItemCount;

    /** 未发商品项数 */
    private Integer unshippedItemCount;

    /** 原单商品数量 */
    private BigDecimal originalQuantity;

    /** 未发商品数量 */
    private BigDecimal unshippedQuantityItems;

    /** 履约率 */
    private BigDecimal fulfillmentRate;

    // ═══ 拣货仓库/集货位 ═══

    /** 拣货仓库 */
    private String pickingWarehouse;

    /** 集货位 */
    private String collectionLocation;

    /** 已拣货数量汇总（= Σ 明细 picked_quantity，拣货作业回写） */
    private BigDecimal pickedQuantity;

    /** 排序（拣货顺序序号） */
    private Integer sortOrder;

    /** 排序值（拣货顺序二级权重） */
    private Integer sortValue;

    // ═══ 系统字段 ═══

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
