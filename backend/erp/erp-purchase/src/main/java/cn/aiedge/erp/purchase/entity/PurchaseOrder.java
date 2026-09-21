package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单主表实体（≤25 核心字段）
 * 对标 Odoo/SAP/金蝶/用友 生产级 ERP 规范
 *
 * 子表分离:
 * - 供应商快照: erp_purchase_order_partner_snapshot (1:1)
 * - 结算信息: erp_purchase_order_settlement (1:1)
 * - 物流信息: erp_purchase_order_logistics (1:N)
 * - 订金账户: erp_purchase_order_deposit (1:N)
 * - 审核流水: erp_purchase_order_audit_trail (1:N)
 * - 扩展信息: erp_purchase_order_ext_info (1:1)
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order")
public class PurchaseOrder {

    /** 订单ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 单据编号 */
    private String orderNo;

    /** 单据日期 */
    private LocalDateTime orderDate;

    /** 单据状态（0草稿/1待审批/2已审批/3已下达/4已取消/5履行中/6已完成） */
    private Integer status;

    // ═══ 外键关联 ═══
    /** 供应商ID */
    private Long supplierId;

    /** 仓库ID */
    private Long warehouseId;

    /** 经手人ID */
    private Long purchaserId;

    /** 部门ID */
    private Long deptId;

    // ═══ 金额汇总 ═══
    /** 商品金额 */
    private BigDecimal productAmount;

    /**
     * 订单总额。
     *
     * <p>表 {@code erp_purchase_order} 上的既有列（{@code total_amount}），本模块的业务写入不设置它，
     * 仅供采购统计沿用旧口径读取 —— 注意它与 {@link #billAmount}（本单金额 / {@code bill_amount}）
     * 是**两个不同的列**，不可互相替代，否则首页「今日采购」KPI 的数值会变。</p>
     */
    private BigDecimal totalAmount;

    /** 直接优惠金额 */
    private BigDecimal discountAmount;

    /** 本单金额 */
    private BigDecimal billAmount;

    /** 已结金额 */
    private BigDecimal settledAmount;

    // ═══ 数量汇总 ═══
    /** 订货总数量 */
    private BigDecimal totalQuantity;

    /** 已收金额 */
    private BigDecimal receivedAmount;

    // ═══ 业务信息 ═══
    /** 预计到货时间 */
    private LocalDateTime expectedReceiveTime;

    /** 采购类型（1正常/2紧急/3样品） */
    private Integer purchaseType;

    /** 源单编号 */
    private String sourceBillNo;

    /** 单据备注 */
    private String remark;

    // ═══ 审批快照 ═══
    /** 审批状态 */
    private Integer approvalStatus;

    /** 当前审核级别 */
    private Integer curCheckLevel;

    /** 提交人ID */
    private Long submitterId;

    /** 提交时间 */
    private LocalDateTime submitTime;

    // ═══ 系统字段 ═══
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
}
