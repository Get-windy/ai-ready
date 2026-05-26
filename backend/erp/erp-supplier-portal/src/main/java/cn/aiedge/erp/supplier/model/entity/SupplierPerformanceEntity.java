package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_performance")
public class SupplierPerformanceEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("evaluation_period")
    private String evaluationPeriod;

    @TableField("evaluation_type")
    private Integer evaluationType;

    @TableField("evaluation_date")
    private LocalDateTime evaluationDate;

    @TableField("evaluator_id")
    private String evaluatorId;

    @TableField("evaluator_name")
    private String evaluatorName;

    @TableField("quality_score")
    private Double qualityScore;

    @TableField("delivery_score")
    private Double deliveryScore;

    @TableField("price_score")
    private Double priceScore;

    @TableField("service_score")
    private Double serviceScore;

    @TableField("technology_score")
    private Double technologyScore;

    @TableField("response_score")
    private Double responseScore;

    @TableField("compliance_score")
    private Double complianceScore;

    @TableField("comprehensive_score")
    private Double comprehensiveScore;

    @TableField("performance_level")
    private String performanceLevel;

    @TableField("level_change")
    private Integer levelChange;

    @TableField("quality_deductions")
    private String qualityDeductions;

    @TableField("delivery_deductions")
    private String deliveryDeductions;

    @TableField("service_deductions")
    private String serviceDeductions;

    @TableField("strengths")
    private String strengths;

    @TableField("improvement_suggestions")
    private String improvementSuggestions;

    @TableField("evaluation_conclusion")
    private String evaluationConclusion;

    @TableField("attachment_info")
    private String attachmentInfo;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("extend_info")
    private String extendInfo;
}