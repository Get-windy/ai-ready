package cn.aiedge.erp.batch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 批次库存实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "batch_inventory", indexes = {
    @Index(name = "idx_batch_id_warehouse", columnList = "batch_id, warehouse_id"),
    @Index(name = "idx_product_id_warehouse_batch", columnList = "product_id, warehouse_id, batch_id"),
    @Index(name = "idx_location_batch", columnList = "location, batch_id"),
    @Index(name = "idx_status_expiry_date", columnList = "inventory_status, expiry_date")
})
public class BatchInventory extends BaseEntity {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 批次ID
     */
    @Column(name = "batch_id", nullable = false)
    private Long batchId;
    
    /**
     * 产品ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    
    /**
     * 库位
     */
    @Column(name = "location", length = 100)
    private String location;
    
    /**
     * 库存数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    /**
     * 预留数量（已分配但未出库）
     */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;
    
    /**
     * 可用数量 = quantity - reservedQuantity
     */
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;
    
    /**
     * 库存状态
     * AVAILABLE: 可用库存
     * RESERVED: 已预留
     * LOCKED: 已锁定（盘点、调整）
     * QUALITY_CHECK: 质量检查中
     * QUARANTINE: 隔离库存
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 30)
    private InventoryStatus inventoryStatus = InventoryStatus.AVAILABLE;
    
    /**
     * 入库时间
     */
    @Column(name = "stock_in_date", nullable = false)
    private LocalDateTime stockInDate;
    
    /**
     * 批次有效期
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    /**
     * 最近盘点时间
     */
    @Column(name = "last_inventory_date")
    private LocalDateTime lastInventoryDate;
    
    /**
     * 最近出库时间
     */
    @Column(name = "last_outbound_date")
    private LocalDateTime lastOutboundDate;
    
    /**
     * 库存预警阈值
     */
    @Column(name = "warning_threshold")
    private Integer warningThreshold;
    
    /**
     * 是否临期（距离过期小于30天）
     */
    @Column(name = "is_near_expiry", nullable = false)
    private Boolean isNearExpiry = false;
    
    /**
     * 是否过期
     */
    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired = false;
    
    /**
     * 库存成本
     */
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private Double unitCost;
    
    /**
     * 总成本 = unitCost * quantity
     */
    @Column(name = "total_cost", precision = 10, scale = 2)
    private Double totalCost;
    
    /**
     * 库存属性JSON（存储扩展属性）
     */
    @Column(name = "attributes", columnDefinition = "json")
    private String attributes;
    
    /**
     * 库存状态枚举
     */
    public enum InventoryStatus {
        AVAILABLE,          // 可用库存
        RESERVED,           // 已预留
        LOCKED,             // 已锁定（盘点、调整）
        QUALITY_CHECK,      // 质量检查中
        QUARANTINE,         // 隔离库存
        IN_TRANSIT          // 在途库存
    }
    
    /**
     * 计算可用数量
     */
    @PrePersist
    @PreUpdate
    private void calculateAvailableQuantity() {
        this.availableQuantity = this.quantity - this.reservedQuantity;
        if (this.availableQuantity < 0) {
            this.availableQuantity = 0;
        }
    }
}