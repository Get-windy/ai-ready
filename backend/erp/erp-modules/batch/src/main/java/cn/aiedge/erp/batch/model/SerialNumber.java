package cn.aiedge.erp.batch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 序列号实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "serial_number", indexes = {
    @Index(name = "idx_serial_number", columnList = "serial_number", unique = true),
    @Index(name = "idx_batch_id_status", columnList = "batch_id, status"),
    @Index(name = "idx_product_id_serial_number", columnList = "product_id, serial_number"),
    @Index(name = "idx_status_created_at", columnList = "status, created_at")
})
public class SerialNumber extends BaseEntity {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 序列号（唯一标识）
     */
    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;
    
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
     * 序列号状态
     * AVAILABLE: 可用
     * IN_USE: 使用中
     * MAINTAINED: 维护中
     * SCRAP: 已报废
     * LOST: 丢失
     * SOLD: 已销售
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SerialNumberStatus status = SerialNumberStatus.AVAILABLE;
    
    /**
     * 序列号类型
     * PRODUCTION: 生产序列号
     * REPAIR: 维修序列号
     * REFURBISHED: 翻新序列号
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "serial_type", nullable = false, length = 20)
    private SerialNumberType serialType = SerialNumberType.PRODUCTION;
    
    /**
     * 启用时间
     */
    @Column(name = "activation_date")
    private LocalDateTime activationDate;
    
    /**
     * 停用时间
     */
    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;
    
    /**
     * 最近扫描时间
     */
    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;
    
    /**
     * 扫描次数
     */
    @Column(name = "scan_count")
    private Integer scanCount = 0;
    
    /**
     * 关联订单ID
     */
    @Column(name = "order_id")
    private Long orderId;
    
    /**
     * 关联客户ID
     */
    @Column(name = "customer_id")
    private Long customerId;
    
    /**
     * 位置信息
     */
    @Column(name = "location", length = 200)
    private String location;
    
    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
    
    /**
     * 序列号属性JSON（存储扩展属性）
     */
    @Column(name = "attributes", columnDefinition = "json")
    private String attributes;
    
    /**
     * 序列号状态枚举
     */
    public enum SerialNumberStatus {
        AVAILABLE,      // 可用
        IN_USE,         // 使用中
        MAINTAINED,     // 维护中
        SCRAP,          // 已报废
        LOST,           // 丢失
        SOLD            // 已销售
    }
    
    /**
     * 序列号类型枚举
     */
    public enum SerialNumberType {
        PRODUCTION,     // 生产序列号
        REPAIR,         // 维修序列号
        REFURBISHED,    // 翻新序列号
        TRIAL           // 试用序列号
    }
}