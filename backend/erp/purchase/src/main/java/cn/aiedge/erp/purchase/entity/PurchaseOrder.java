package cn.aiedge.erp.purchase.entity;

import cn.aiedge.erp.purchase.enums.OrderStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * 订单状态
     */
    private OrderStatus status;

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
     * 履行百分比
     */
    private BigDecimal fulfillmentPercent;

    /**
     * 合同ID
     */
    private Long contractId;

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

    /**
     * 供应商是否确认
     */
    private Boolean supplierConfirmed;

    /**
     * 供应商确认时间
     */
    private LocalDateTime supplierConfirmTime;

    /**
     * 是否已发货
     */
    private Boolean shipped;

    /**
     * 发货时间
     */
    private LocalDateTime shipTime;

    /**
     * 物流跟踪号
     */
    private String trackingNumber;

    /**
     * 预计到达时间
     */
    private LocalDateTime estimatedArrivalTime;

    /**
     * 是否已收货
     */
    private Boolean received;

    /**
     * 收货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 收货数量
     */
    private BigDecimal receivedQuantity;

    /**
     * 质量检查结果
     */
    private String qualityCheckResult;

    /**
     * 发票是否已提交
     */
    private Boolean invoiceSubmitted;

    /**
     * 发票号
     */
    private String invoiceNumber;

    /**
     * 发票金额
     */
    private BigDecimal invoiceAmount;

    /**
     * 发票日期
     */
    private LocalDate invoiceDate;

    /**
     * 总数量
     */
    private BigDecimal totalQuantity;
}