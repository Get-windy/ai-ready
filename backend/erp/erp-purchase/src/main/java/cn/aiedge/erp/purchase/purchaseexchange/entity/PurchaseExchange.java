package cn.aiedge.erp.purchase.purchaseexchange.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购换货单
 *
 * <p>字段按业务实体分组（字段实体溯源），对标生产级单据模型：</p>
 * <ul>
 *   <li>供应商快照（来源: erp_supplier）：supplierId, supplierName, supplierCode, supplierRemark,
 *       contactName, contactPhone, contactAddress, bankName, bankAccount, taxNo</li>
 *   <li>仓库快照（来源: erp_warehouse）：inWarehouseId, inWarehouseName, outWarehouseId, outWarehouseName</li>
 *   <li>职员/部门快照（来源: sys_user/sys_dept）：handlerId, handlerName, deptId, deptName,
 *       bookkeeperName, bookkeepingTime, creatorId, creatorName</li>
 *   <li>付款/结算：paymentAccount, paidAmount, moreAccounts, prevPrepaid, usePrepaid,
 *       prepaidBalance, prevDebt, currentDebt, debtBalance, paymentDeadline, settleStatus, settledAmount</li>
 *   <li>金额计算链：productAmount, totalAmount, discountAmount</li>
 *   <li>数量汇总：inQuantityTotal, outQuantityTotal</li>
 *   <li>物理属性汇总：totalWeight, totalVolume</li>
 *   <li>源单关联：originalOrderId, originalOrderNo</li>
 *   <li>自定义字段（表头）：extNum1-5, extText1-5</li>
 * </ul>
 */
@Data
@TableName("erp_purchase_exchange")
public class PurchaseExchange {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    // ========== 单据基本信息 ==========
    private String exchangeNo;
    private LocalDate exchangeDate;
    private Integer status;
    private String settleStatus;
    private Integer printCount;
    private String attachment;
    private String summary;

    // ========== 供应商快照（来源: erp_supplier） ==========
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private String supplierRemark;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
    private String bankName;
    private String bankAccount;
    private String taxNo;

    // ========== 仓库快照（来源: erp_warehouse） ==========
    private Long inWarehouseId;
    private String inWarehouseName;
    private Long outWarehouseId;
    private String outWarehouseName;

    // ========== 职员/部门快照 ==========
    private Long handlerId;
    private String handlerName;
    private Long deptId;
    private String deptName;
    private String bookkeeperName;
    private LocalDateTime bookkeepingTime;

    // ========== 付款/结算 ==========
    private String paymentAccount;
    private BigDecimal paidAmount;
    private String moreAccounts;
    private BigDecimal prevPrepaid;
    private BigDecimal usePrepaid;
    private BigDecimal prepaidBalance;
    private BigDecimal prevDebt;
    private BigDecimal currentDebt;
    private BigDecimal debtBalance;
    private String paymentDeadline;

    // ========== 金额计算链 ==========
    private BigDecimal productAmount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal settledAmount;

    // ========== 数量汇总 ==========
    private BigDecimal inQuantityTotal;
    private BigDecimal outQuantityTotal;

    // ========== 物理属性汇总 ==========
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    // ========== 源单关联 ==========
    private Long originalOrderId;
    private String originalOrderNo;

    // ========== 换货业务信息 ==========
    private String exchangeReason;
    private Integer exchangeType;
    private String remark;

    // ========== 自定义字段（表头-数字） ==========
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;

    // ========== 自定义字段（表头-文本） ==========
    private String extText1;
    private String extText2;
    private String extText3;
    private String extText4;
    private String extText5;

    // ========== 审批相关 ==========
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedTime;
    private LocalDateTime completedTime;

    // ========== 制单信息 ==========
    private Long createdBy;
    private String createdByName;

    // ========== 标准审计字段 ==========
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    // ========== 非数据库字段 ==========
    @TableField(exist = false)
    private List<PurchaseExchangeItem> items;
}
