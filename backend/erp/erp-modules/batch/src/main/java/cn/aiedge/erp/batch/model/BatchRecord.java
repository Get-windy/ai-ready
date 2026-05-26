package cn.aiedge.erp.batch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 批次记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "batch_record", indexes = {
    @Index(name = "idx_batch_number", columnList = "batch_number", unique = true),
    @Index(name = "idx_product_id_batch_number", columnList = "product_id, batch_number"),
    @Index(name = "idx_expiry_date_status", columnList = "expiry_date, status"),
    @Index(name = "idx_created_at_tenant", columnList = "created_at, tenant_id")
})
public class BatchRecord extends BaseEntity {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 批次号（唯一标识）
     */
    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;
    
    /**
     * 产品ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    /**
     * 生产日期
     */
    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;
    
    /**
     * 有效期至
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
    
    /**
     * 批次数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    /**
     * 剩余数量
     */
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;
    
    /**
     * 批次状态
     * ACTIVE: 有效批次
     * EXPIRED: 已过期
     * QUARANTINED: 隔离批次（质量问题）
     * CANCELLED: 已取消
     * CONSUMED: 已消耗完
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status = BatchStatus.ACTIVE;
    
    /**
     * 质量等级
     * QUALIFIED: 合格
     * REJECTED: 不合格
     * PENDING_INSPECTION: 待检验
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", nullable = false, length = 30)
    private QualityGrade qualityGrade = QualityGrade.PENDING_INSPECTION;
    
    /**
     * 供应商ID
     */
    @Column(name = "supplier_id")
    private Long supplierId;
    
    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;
    
    /**
     * 库位
     */
    @Column(name = "location", length = 100)
    private String location;
    
    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
    
    /**
     * 批次属性JSON（存储扩展属性）
     */
    @Column(name = "attributes", columnDefinition = "json")
    private String attributes;
    
    /**
     * 批次状态枚举
     */
    public enum BatchStatus {
        ACTIVE,         // 有效批次
        EXPIRED,        // 已过期
        QUARANTINED,    // 隔离批次（质量问题）
        CANCELLED,      // 已取消
        CONSUMED        // 已消耗完
    }
    
    /**
     * 质量等级枚举
     */
    public enum QualityGrade {
        QUALIFIED,              // 合格
        REJECTED,               // 不合格
        PENDING_INSPECTION,     // 待检验
        CONDITIONAL_ACCEPTANCE  // 有条件接收
    }
}