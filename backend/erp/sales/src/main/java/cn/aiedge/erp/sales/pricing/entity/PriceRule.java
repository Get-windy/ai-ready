package cn.aiedge.erp.sales.pricing.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格规则实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "erp_pricing_rule")
@TableName("erp_pricing_rule")
public class PriceRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @Column(name = "strategy_id", nullable = false)
    private Long strategyId;
    
    @Column(name = "rule_name", length = 100)
    private String ruleName;
    
    @Column(name = "rule_type", length = 50)
    private String ruleType;
    
    @Column(name = "condition_type", length = 50)
    private String conditionType;
    
    @Column(name = "condition_value", length = 500)
    private String conditionValue;
    
    @Column(name = "calculation_type", length = 50)
    private String calculationType;
    
    @Column(name = "price_factor", precision = 10, scale = 4)
    private BigDecimal priceFactor;
    
    @Column(name = "discount_rate", precision = 10, scale = 4)
    private BigDecimal discountRate;
    
    @Column(name = "discount_amount", precision = 20, scale = 6)
    private BigDecimal discountAmount;
    
    @Column(name = "min_quantity")
    private Integer minQuantity;
    
    @Column(name = "max_quantity")
    private Integer maxQuantity;
    
    @Column(name = "min_amount", precision = 20, scale = 6)
    private BigDecimal minAmount;
    
    @Column(name = "max_amount", precision = 20, scale = 6)
    private BigDecimal maxAmount;
    
    @Column(name = "priority")
    private Integer priority = 0;
    
    @Column(name = "status", length = 20)
    private String status = "active";
    
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
}