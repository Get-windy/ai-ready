package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 等级升降级规则实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_level_rule")
@Entity
@Table(name = "erp_supplier_level_rule", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_rule_code", columnList = "rule_code"),
    @Index(name = "idx_rule_type", columnList = "rule_type"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
public class SupplierLevelRuleEntity {
    
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
     * 规则类型：UPGRADE-升级，DOWNGRADE-降级，MAINTAIN-维持
     */
    @TableField("rule_type")
    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType;
    
    /**
     * 源等级编码
     */
    @TableField("source_level_code")
    @Column(name = "source_level_code", nullable = false, length = 20)
    private String sourceLevelCode;
    
    /**
     * 目标等级编码
     */
    @TableField("target_level_code")
    @Column(name = "target_level_code", nullable = false, length = 20)
    private String targetLevelCode;
    
    /**
     * 评估维度：SCORE-综合评分，POINTS-累计积分，QUALITY-质量，DELIVERY-交付
     */
    @TableField("evaluation_dimension")
    @Column(name = "evaluation_dimension", length = 20)
    private String evaluationDimension;
    
    /**
     * 比较操作符：GE-大于等于，LE-小于等于，GT-大于，LT-小于，EQ-等于
     */
    @TableField("comparison_operator")
    @Column(name = "comparison_operator", length = 10)
    private String comparisonOperator;
    
    /**
     * 阈值
     */
    @TableField("threshold_value")
    @Column(name = "threshold_value", precision = 10, scale = 2)
    private BigDecimal thresholdValue;
    
    /**
     * 持续时间类型：NONE-无时间要求，DAYS-天数，MONTHS-月数，YEARS-年数
     */
    @TableField("duration_type")
    @Column(name = "duration_type", length = 20)
    private String durationType;
    
    /**
     * 持续时间值
     */
    @TableField("duration_value")
    @Column(name = "duration_value")
    private Integer durationValue;
    
    /**
     * 评估周期：MONTHLY-月度，QUARTERLY-季度，YEARLY-年度
     */
    @TableField("evaluation_period")
    @Column(name = "evaluation_period", length = 20)
    private String evaluationPeriod;
    
    /**
     * 是否需要审批
     */
    @TableField("require_approval")
    @Column(name = "require_approval")
    private Boolean requireApproval;
    
    /**
     * 审批人角色
     */
    @TableField("approval_role")
    @Column(name = "approval_role", length = 50)
    private String approvalRole;
    
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
     * 条件表达式（JSON格式）
     */
    @TableField("condition_expression")
    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;
    
    /**
     * 动作配置（JSON格式）
     */
    @TableField("action_config")
    @Column(name = "action_config", columnDefinition = "TEXT")
    private String actionConfig;
    
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