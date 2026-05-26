package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权益定义实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_benefit")
@Entity
@Table(name = "erp_supplier_benefit", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_benefit_code", columnList = "benefit_code"),
    @Index(name = "idx_benefit_type", columnList = "benefit_type"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
public class SupplierBenefitEntity {
    
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
     * 权益编码，唯一标识
     */
    @TableField("benefit_code")
    @Column(name = "benefit_code", nullable = false, unique = true, length = 50)
    private String benefitCode;
    
    /**
     * 权益名称
     */
    @TableField("benefit_name")
    @Column(name = "benefit_name", nullable = false, length = 100)
    private String benefitName;
    
    /**
     * 权益描述
     */
    @TableField("benefit_description")
    @Column(name = "benefit_description", length = 500)
    private String benefitDescription;
    
    /**
     * 权益类型：DISCOUNT-折扣，PRIORITY-优先权，RESOURCE-资源，SERVICE-服务，AWARD-奖励
     */
    @TableField("benefit_type")
    @Column(name = "benefit_type", nullable = false, length = 20)
    private String benefitType;
    
    /**
     * 权益子类型
     */
    @TableField("benefit_subtype")
    @Column(name = "benefit_subtype", length = 50)
    private String benefitSubtype;
    
    /**
     * 权益值
     */
    @TableField("benefit_value")
    @Column(name = "benefit_value", precision = 10, scale = 2)
    private BigDecimal benefitValue;
    
    /**
     * 权益单位：PERCENT-百分比，AMOUNT-金额，COUNT-次数，DAY-天数
     */
    @TableField("benefit_unit")
    @Column(name = "benefit_unit", length = 20)
    private String benefitUnit;
    
    /**
     * 适用等级编码列表（JSON数组）
     */
    @TableField("applicable_level_codes")
    @Column(name = "applicable_level_codes", length = 500)
    private String applicableLevelCodes;
    
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
     * 使用条件类型：NONE-无条件，POINTS-积分，AMOUNT-金额，COUNT-次数
     */
    @TableField("usage_condition_type")
    @Column(name = "usage_condition_type", length = 20)
    private String usageConditionType;
    
    /**
     * 使用条件值
     */
    @TableField("usage_condition_value")
    @Column(name = "usage_condition_value", precision = 10, scale = 2)
    private BigDecimal usageConditionValue;
    
    /**
     * 使用上限类型：NONE-无上限，DAILY-每日，MONTHLY-每月，YEARLY-每年，LIFETIME-终身
     */
    @TableField("usage_limit_type")
    @Column(name = "usage_limit_type", length = 20)
    private String usageLimitType;
    
    /**
     * 使用上限值
     */
    @TableField("usage_limit_value")
    @Column(name = "usage_limit_value", precision = 10, scale = 2)
    private BigDecimal usageLimitValue;
    
    /**
     * 兑换所需积分
     */
    @TableField("exchange_points")
    @Column(name = "exchange_points", precision = 10, scale = 2)
    private BigDecimal exchangePoints;
    
    /**
     * 是否自动发放
     */
    @TableField("auto_issue")
    @Column(name = "auto_issue")
    private Boolean autoIssue;
    
    /**
     * 发放时间类型：IMMEDIATE-立即，SCHEDULED-定时，CONDITIONAL-条件触发
     */
    @TableField("issue_time_type")
    @Column(name = "issue_time_type", length = 20)
    private String issueTimeType;
    
    /**
     * 发放时间表达式
     */
    @TableField("issue_time_expression")
    @Column(name = "issue_time_expression", length = 100)
    private String issueTimeExpression;
    
    /**
     * 是否启用
     */
    @TableField("is_active")
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * 排序序号
     */
    @TableField("sort_order")
    @Column(name = "sort_order")
    private Integer sortOrder;
    
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