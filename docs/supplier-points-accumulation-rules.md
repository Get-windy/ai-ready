package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分累计规则实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_points_rule")
@Entity
@Table(name = "erp_supplier_points_rule", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_rule_code", columnList = "rule_code"),
    @Index(name = "idx_rule_type", columnList = "rule_type"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
public class SupplierPointsRuleEntity {
    
    /**
     * 主键ID
     */
    @Id
    @TableId(value = "id", type = IdType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 租户ID，用于多租户数据隔离
     */
    @TableField("tenant_id")
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;
    
    /**
     * 规则编码，唯一标识
     */
    @TableField("rule_code")
    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    private String ruleCode;
    
    /**
     * 规则名称
     */
    @TableField("rule_name")
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    /**
     * 规则描述
     */
    @TableField("rule_description")
    @Column(name = "rule_description", length = 500)
    private String ruleDescription;
    
    /**
     * 规则类型：ORDER-订单相关，QUALITY-质量相关，DELIVERY-交付相关，SERVICE-服务相关，TECHNOLOGY-技术相关
     */
    @TableField("rule_type")
    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType;
    
    /**
     * 计算方式：FIXED-固定值，PERCENTAGE-百分比，CONDITIONAL-条件计算
     */
    @TableField("calculation_method")
    @Column(name = "calculation_method", nullable = false, length = 20)
    private String calculationMethod;
    
    /**
     * 基础积分值
     */
    @TableField("base_points")
    @Column(name = "base_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePoints;
    
    /**
     * 积分系数
     */
    @TableField("points_factor")
    @Column(name = "points_factor", precision = 10, scale = 4)
    private BigDecimal pointsFactor;
    
    /**
     * 触发条件类型：AMOUNT-金额，QUANTITY-数量，COUNT-次数，SCORE-分数
     */
    @TableField("trigger_condition_type")
    @Column(name = "trigger_condition_type", length = 20)
    private String triggerConditionType;
    
    /**
     * 触发条件值
     */
    @TableField("trigger_condition_value")
    @Column(name = "trigger_condition_value", precision = 10, scale = 2)
    private BigDecimal triggerConditionValue;
    
    /**
     * 适用等级：ALL-所有等级，SPECIFIC-特定等级
     */
    @TableField("applicable_level_type")
    @Column(name = "applicable_level_type", length = 20)
    private String applicableLevelType;
    
    /**
     * 适用等级编码列表（JSON数组）
     */
    @TableField("applicable_level_codes")
    @Column(name = "applicable_level_codes", length = 500)
    private String applicableLevelCodes;
    
    /**
     * 适用供应商类型：ALL-所有类型，SPECIFIC-特定类型
     */
    @TableField("applicable_supplier_type")
    @Column(name = "applicable_supplier_type", length = 20)
    private String applicableSupplierType;
    
    /**
     * 适用供应商类型列表（JSON数组）
     */
    @TableField("applicable_supplier_types")
    @Column(name = "applicable_supplier_types", length = 500)
    private String applicableSupplierTypes;
    
    /**
     * 生效时间
     */
    @TableField("effective_start_time")
    @Column(name = "effective_start_time")
    private LocalDateTime effectiveStartTime;
    
    /**
     * 失效时间
     */
    @TableField("effective_end_time")
    @Column(name = "effective_end_time")
    private LocalDateTime effectiveEndTime;
    
    /**
     * 每日上限
     */
    @TableField("daily_limit")
    @Column(name = "daily_limit", precision = 10, scale = 2)
    private BigDecimal dailyLimit;
    
    /**
     * 每月上限
     */
    @TableField("monthly_limit")
    @Column(name = "monthly_limit", precision = 10, scale = 2)
    private BigDecimal monthlyLimit;
    
    /**
     * 每年上限
     */
    @TableField("yearly_limit")
    @Column(name = "yearly_limit", precision = 10, scale = 2)
    private BigDecimal yearlyLimit;
    
    /**
     * 是否启用
     */
    @TableField("is_active")
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * 优先级
     */
    @TableField("priority")
    @Column(name = "priority")
    private Integer priority;
    
    /**
     * 创建人
     */
    @TableField("created_by")
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 创建时间
     */
    @TableField("created_time")
    @Column(name = "created_time")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 更新人
     */
    @TableField("updated_by")
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    /**
     * 更新时间
     */
    @TableField("updated_time")
    @Column(name = "updated_time")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    /**
     * 版本号，用于乐观锁
     */
    @Version
    @TableField("version")
    @Column(name = "version")
    private Integer version;
    
    /**
     * 逻辑删除标识
     */
    @TableLogic
    @TableField("deleted")
    @Column(name = "deleted")
    private Integer deleted;
    
    /**
     * 扩展字段（JSON格式）
     */
    @TableField("extend_info")
    @Column(name = "extend_info", columnDefinition = "TEXT")
    private String extendInfo;
}