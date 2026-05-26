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
@TableName("erp_supplier_benefit")
public class SupplierBenefitEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("benefit_code")
    private String benefitCode;

    @TableField("benefit_name")
    private String benefitName;

    @TableField("benefit_description")
    private String benefitDescription;

    @TableField("benefit_type")
    private String benefitType;

    @TableField("benefit_subtype")
    private String benefitSubtype;

    @TableField("benefit_value")
    private BigDecimal benefitValue;

    @TableField("benefit_unit")
    private String benefitUnit;

    @TableField("applicable_level_codes")
    private String applicableLevelCodes;

    @TableField("applicable_supplier_types")
    private String applicableSupplierTypes;

    @TableField("effective_start_time")
    private LocalDateTime effectiveStartTime;

    @TableField("effective_end_time")
    private LocalDateTime effectiveEndTime;

    @TableField("usage_condition_type")
    private String usageConditionType;

    @TableField("usage_condition_value")
    private BigDecimal usageConditionValue;

    @TableField("usage_limit_type")
    private String usageLimitType;

    @TableField("usage_limit_value")
    private BigDecimal usageLimitValue;

    @TableField("exchange_points")
    private BigDecimal exchangePoints;

    @TableField("auto_issue")
    private Boolean autoIssue;

    @TableField("issue_time_type")
    private String issueTimeType;

    @TableField("issue_time_expression")
    private String issueTimeExpression;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("sort_order")
    private Integer sortOrder;

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