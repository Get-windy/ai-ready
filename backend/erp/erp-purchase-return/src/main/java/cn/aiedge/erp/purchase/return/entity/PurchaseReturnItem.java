package cn.aiedge.erp.purchase.return.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购换货单明细实体类
 * 
 * 对应数据库表: purchase_return_order_item
 * 功能: 存储采购换货单的物料明细信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_return_order_item")
public class PurchaseReturnItem {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 换货单ID
     */
    @TableField("return_order_id")
    private Long returnOrderId;
    
    /**
     * 换货单号
     */
    @TableField("return_code")
    private String returnCode;
    
    /**
     * 原采购订单明细ID
     */
    @TableField("purchase_order_item_id")
    private Long purchaseOrderItemId;
    
    /**
     * 物料ID
     */
    @TableField("material_id")
    private Long materialId;
    
    /**
     * 物料编码
     */
    @TableField("material_code")
    private String materialCode;
    
    /**
     * 物料名称
     */
    @TableField("material_name")
    private String materialName;
    
    /**
     * 物料规格
     */
    @TableField("material_specification")
    private String materialSpecification;
    
    /**
     * 物料型号
     */
    @TableField("material_model")
    private String materialModel;
    
    /**
     * 物料单位
     */
    @TableField("material_unit")
    private String materialUnit;
    
    /**
     * 原采购数量
     */
    @TableField("original_purchase_quantity")
    private BigDecimal originalPurchaseQuantity;
    
    /**
     * 原采购单价
     */
    @TableField("original_purchase_price")
    private BigDecimal originalPurchasePrice;
    
    /**
     * 原采购金额
     */
    @TableField("original_purchase_amount")
    private BigDecimal originalPurchaseAmount;
    
    /**
     * 已收货数量
     */
    @TableField("received_quantity")
    private BigDecimal receivedQuantity;
    
    /**
     * 待退货数量
     */
    @TableField("return_quantity")
    private BigDecimal returnQuantity;
    
    /**
     * 已退货数量
     */
    @TableField("returned_quantity")
    private BigDecimal returnedQuantity;
    
    /**
     * 待换货数量
     */
    @TableField("replacement_quantity")
    private BigDecimal replacementQuantity;
    
    /**
     * 已换货数量
     */
    @TableField("replaced_quantity")
    private BigDecimal replacedQuantity;
    
    /**
     * 换货单价 (如果与原来不同)
     */
    @TableField("replacement_price")
    private BigDecimal replacementPrice;
    
    /**
     * 换货金额
     */
    @TableField("replacement_amount")
    private BigDecimal replacementAmount;
    
    /**
     * 批次号
     */
    @TableField("batch_number")
    private String batchNumber;
    
    /**
     * 生产日期
     */
    @TableField("production_date")
    private LocalDateTime productionDate;
    
    /**
     * 有效期至
     */
    @TableField("expiry_date")
    private LocalDateTime expiryDate;
    
    /**
     * 库存位置 (仓库/库位)
     */
    @TableField("inventory_location")
    private String inventoryLocation;
    
    /**
     * 质量问题类型编码 (仅质量换货时使用)
     */
    @TableField("quality_issue_code")
    private String qualityIssueCode;
    
    /**
     * 质量问题描述 (仅质量换货时使用)
     */
    @TableField("quality_issue_description")
    private String qualityIssueDescription;
    
    /**
     * 问题等级 (minor - 轻微, moderate - 中等, severe - 严重, critical - 致命)
     */
    @TableField("issue_level")
    private String issueLevel;
    
    /**
     * 质量检测报告路径 (针对此物料)
     */
    @TableField("quality_report_path")
    private String qualityReportPath;
    
    /**
     * 换货原因 (物料级具体原因)
     */
    @TableField("item_return_reason")
    private String itemReturnReason;
    
    /**
     * 供应商确认状态 (pending - 待确认, confirmed - 已确认, rejected - 已拒绝)
     */
    @TableField("supplier_confirmation_status")
    private String supplierConfirmationStatus;
    
    /**
     * 供应商确认备注 (物料级)
     */
    @TableField("supplier_confirmation_note")
    private String supplierConfirmationNote;
    
    /**
     * 退货状态 (pending - 待退货, packaging - 打包中, shipped - 已发货, returned - 已退货)
     */
    @TableField("return_status")
    private String returnStatus;
    
    /**
     * 换货状态 (pending - 待换货, processing - 处理中, shipped - 已发货, received - 已收货, completed - 已完成)
     */
    @TableField("replacement_status")
    private String replacementStatus;
    
    /**
     * 退货物流单号 (物料级)
     */
    @TableField("return_tracking_number")
    private String returnTrackingNumber;
    
    /**
     * 换货物流单号 (物料级)
     */
    @TableField("replacement_tracking_number")
    private String replacementTrackingNumber;
    
    /**
     * 预计退货日期
     */
    @TableField("expected_return_date")
    private LocalDateTime expectedReturnDate;
    
    /**
     * 实际退货日期
     */
    @TableField("actual_return_date")
    private LocalDateTime actualReturnDate;
    
    /**
     * 预计换货收货日期
     */
    @TableField("expected_replacement_date")
    private LocalDateTime expectedReplacementDate;
    
    /**
     * 实际换货收货日期
     */
    @TableField("actual_replacement_date")
    private LocalDateTime actualReplacementDate;
    
    /**
     * 检验人ID
     */
    @TableField("inspector_id")
    private Long inspectorId;
    
    /**
     * 检验人姓名
     */
    @TableField("inspector_name")
    private String inspectorName;
    
    /**
     * 检验日期
     */
    @TableField("inspection_date")
    private LocalDateTime inspectionDate;
    
    /**
     * 检验结果 (qualified - 合格, unqualified - 不合格, conditional - 有条件接受)
     */
    @TableField("inspection_result")
    private String inspectionResult;
    
    /**
     * 检验备注
     */
    @TableField("inspection_note")
    private String inspectionNote;
    
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
    
    /**
     * 数据版本号 (用于乐观锁)
     */
    @Version
    @TableField("version")
    private Integer version;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 创建人ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;
    
    /**
     * 创建人姓名
     */
    @TableField(value = "created_by_name", fill = FieldFill.INSERT)
    private String createdByName;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 更新人ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    
    /**
     * 更新人姓名
     */
    @TableField(value = "updated_by_name", fill = FieldFill.INSERT_UPDATE)
    private String updatedByName;
    
    /**
     * 删除标识 (0: 未删除, 1: 已删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    
    /**
     * 租户ID (多租户支持)
     */
    @TableField("tenant_id")
    private String tenantId;
}