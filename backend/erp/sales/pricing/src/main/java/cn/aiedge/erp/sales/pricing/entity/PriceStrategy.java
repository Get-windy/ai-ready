package cn.aiedge.erp.sales.pricing.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格策略实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "erp_pricing_strategy")
@TableName("erp_pricing_strategy")
public class PriceStrategy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @Column(name = "tenant_id")
    private Long tenantId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "strategy_type", length = 50)
    private String strategyType;
    
    @Column(name = "customer_level", length = 50)
    private String customerLevel;
    
    @Column(name = "region_code", length = 50)
    private String regionCode;
    
    @Column(name = "product_category_id")
    private Long productCategoryId;
    
    @Column(name = "base_price", precision = 20, scale = 6)
    private BigDecimal basePrice;
    
    @Column(name = "price_factor", precision = 10, scale = 4)
    private BigDecimal priceFactor;
    
    @Column(name = "discount_rate", precision = 10, scale = 4)
    private BigDecimal discountRate;
    
    @Column(name = "discount_amount", precision = 20, scale = 6)
    private BigDecimal discountAmount;
    
    @Column(name = "min_quantity")
    private Integer minQuantity;
    
    @Column(name = "formula_config", columnDefinition = "json")
    private String formulaConfig;
    
    @Column(name = "effective_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveStartTime;
    
    @Column(name = "effective_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveEndTime;
    
    @Column(name = "priority")
    private Integer priority = 0;
    
    @Column(name = "status", length = 20)
    private String status = "draft";
    
    @Column(name = "deleted")
    @TableLogic
    private Integer deleted = 0;
    
    @Column(name = "create_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @Column(name = "create_by")
    private Long createBy;
    
    @Column(name = "update_by")
    private Long updateBy;
}