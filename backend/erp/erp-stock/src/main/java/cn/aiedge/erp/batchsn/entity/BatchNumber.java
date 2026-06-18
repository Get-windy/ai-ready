package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次号主表
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Data
@TableName("batch_number")
public class BatchNumber {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 批次号，唯一标识一个批次
     */
    @TableField("batch_no")
    private String batchNo;
    
    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;
    
    /**
     * 产品编码
     */
    @TableField("product_code")
    private String productCode;
    
    /**
     * 产品名称
     */
    @TableField("product_name")
    private String productName;
    
    /**
     * 产品规格
     */
    @TableField("specification")
    private String specification;
    
    /**
     * 单位
     */
    @TableField("unit")
    private String unit;
    
    /**
     * 生产日期
     */
    @TableField("production_date")
    private LocalDate productionDate;
    
    /**
     * 有效期至
     */
    @TableField("expiration_date")
    private LocalDate expirationDate;
    
    /**
     * 批次状态：ACTIVE-活跃中, EXPIRED-已过期, QUARANTINED-隔离中, CANCELLED-已取消
     */
    @TableField("batch_status")
    private String batchStatus;
    
    /**
     * 总数量
     */
    @TableField("total_quantity")
    private BigDecimal totalQuantity;
    
    /**
     * 可用数量（可用于出库）
     */
    @TableField("available_quantity")
    private BigDecimal availableQuantity;
    
    /**
     * 预留数量（销售预留/生产占用）
     */
    @TableField("reserved_quantity")
    private BigDecimal reservedQuantity;
    
    /**
     * 来源类型：PURCHASE-采购入库, PRODUCTION-生产入库, SALE_RETURN-销售退货
     */
    @TableField("source_type")
    private String sourceType;
    
    /**
     * 来源单据ID
     */
    @TableField("source_ref_id")
    private Long sourceRefId;
    
    /**
     * 来源单据号
     */
    @TableField("source_ref_no")
    private String sourceRefNo;
    
    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;
    
    /**
     * 仓库名称
     */
    @TableField("warehouse_name")
    private String warehouseName;
    
    /**
     * 库位ID
     */
    @TableField("location_id")
    private Long locationId;
    
    /**
     * 质量状态：NORMAL-正常, QUARANTINED-待检, DEFECTIVE-不合格
     */
    @TableField("quality_status")
    private String qualityStatus;
    
    /**
     * 质检员ID
     */
    @TableField("quality_inspector_id")
    private String qualityInspectorId;
    
    /**
     * 质检员姓名
     */
    @TableField("quality_inspector_name")
    private String qualityInspectorName;
    
    /**
     * 质检日期
     */
    @TableField("quality_inspection_date")
    private LocalDateTime qualityInspectionDate;
    
    /**
     * 批次规则ID
     */
    @TableField("batch_rule_id")
    private Long batchRuleId;
    
    /**
     * 批次规则名称
     */
    @TableField("batch_rule_name")
    private String batchRuleName;
    
    /**
     * 创建人ID
     */
    @TableField("created_by")
    private String createdBy;
    
    /**
     * 创建人姓名
     */
    @TableField("created_by_name")
    private String createdByName;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private String updatedBy;
    
    /**
     * 更新人姓名
     */
    @TableField("updated_by_name")
    private String updatedByName;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 删除时间（软删除）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 乐观锁版本
     */
    @TableField("version")
    @Version
    private Integer version;
    
    /**
     * 逻辑删除标记
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
