package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order")
public class PurchaseOrder {

    /**
     * 订单ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 订单号（唯一）
     */
    private String orderNo;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 订单日期
     */
    private LocalDateTime orderDate;

    /**
     * 预计到货日期
     */
    private LocalDateTime expectedDate;

    /**
     * 订单状态（0-草稿 1-待审批 2-已审批 3-部分入库 4-完成 5-取消）
     */
    private Integer status;

    /**
     * 订单金额（不含税）
     */
    private BigDecimal totalAmount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 订单金额（含税）
     */
    private BigDecimal totalAmountWithTax;

    /**
     * 已入库金额
     */
    private BigDecimal receivedAmount;

    /**
     * 采购员ID
     */
    private Long purchaserId;

    /**
     * 采购员名称
     */
    private String purchaserName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展信息（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedTime;
}