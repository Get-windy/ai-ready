package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_points_rule")
public class SupplierPointsRuleEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_description")
    private String ruleDescription;

    @TableField("rule_type")
    private String ruleType;

    @TableField("calculation_method")
    private String calculationMethod;

    @TableField("base_points")
    private BigDecimal basePoints;

    @TableField("points_factor")
    private BigDecimal pointsFactor;

    @TableField("trigger_condition_type")
    private String triggerConditionType;

    @TableField("trigger_condition_value")
    private BigDecimal triggerConditionValue;

    @TableField("applicable_level_type")
    private String applicableLevelType;

    @TableField("applicable_level_codes")
    private String applicableLevelCodes;

    @TableField("applicable_supplier_type")
    private String applicableSupplierType;

    @TableField("applicable_supplier_types")
    private String applicableSupplierTypes;

    @TableField("effective_start_time")
    private LocalDateTime effectiveStartTime;

    @TableField("effective_end_time")
    private LocalDateTime effectiveEndTime;

    @TableField("daily_limit")
    private BigDecimal dailyLimit;

    @TableField("monthly_limit")
    private BigDecimal monthlyLimit;

    @TableField("yearly_limit")
    private BigDecimal yearlyLimit;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("priority")
    private Integer priority;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField("created_by")
    private String createdBy;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField("updated_by")
    private String updatedBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("extend_info")
    private String extendInfo;
}