package cn.aiedge.erp.sale.saleexchange.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售换货单
 *
 * <p>字段按业务实体分组（字段实体溯源）：</p>
 * <ul>
 *   <li>客户快照（来源: erp_customer）：customerId, customerName, customerCode, customerLevel,
 *       contactName, contactPhone, contactAddress, bankName, bankAccount, taxNo</li>
 *   <li>仓库快照（来源: erp_warehouse）：inWarehouseId, inWarehouseName, outWarehouseId, outWarehouseName</li>
 *   <li>职员/部门快照（来源: sys_user/sys_dept）：handlerId, handlerName, deptId, deptName,
 *       bookkeeperName, bookkeepingTime, creatorId, creatorName</li>
 *   <li>收款/信用：paymentAccount, receivedAmount, prevAdvance, useAdvance, availableAdvance,
 *       advanceBalance, receivableIncrease, creditLimit, availableCredit, prevDebt, currentDebt,
 *       debtBalance, collectionDeadline</li>
 *   <li>金额计算链：productAmount, totalAmount, discountAmount, settledAmount</li>
 *   <li>数量汇总：inQuantityTotal, outQuantityTotal</li>
 *   <li>物理属性汇总：totalWeight, totalVolume</li>
 *   <li>源单关联：originalOrderId, originalOrderNo</li>
 *   <li>结算：settleStatus, settledAmount</li>
 *   <li>自定义字段（表头）：extNum1-5, extText1-5</li>
 * </ul>
 */
@Data
@TableName("erp_sale_exchange")
public class SaleExchange {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    // ========== 单据基本信息 ==========
    private String exchangeNo;

    /** 单据日期：兼容 "2026-09-10" / "2026-09-10T00:00:00" 两种前端写法 */
    @JsonDeserialize(using = cn.aiedge.erp.sale.saleexchange.support.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime exchangeDate;

    private String salesType;
    private Integer status;
    private String settleStatus;
    private Integer printCount;
    private String attachment;
    private String summary;

    // ========== 客户快照（来源: erp_customer） ==========
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerLevel;
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

    // ========== 收款/信用 ==========
    private String paymentAccount;

    /** 更多账户（对标：表单「收款 → 更多账户」，逗号分隔的账户名列表） */
    private String moreAccounts;
    private BigDecimal receivedAmount;
    private BigDecimal prevAdvance;
    private BigDecimal useAdvance;
    private BigDecimal availableAdvance;
    private BigDecimal advanceBalance;
    private BigDecimal receivableIncrease;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal prevDebt;
    private BigDecimal currentDebt;
    private BigDecimal debtBalance;
    private String collectionDeadline;

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
    // 列名与销售域其它单据（销售退货单/退货申请/销售单据查询）保持一致：creator_id / creator_name
    private Long creatorId;
    private String creatorName;

    // ========== 标准审计字段 ==========
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    // ========== 非数据库字段 ==========
    @TableField(exist = false)
    private List<SaleExchangeItem> items;
}
