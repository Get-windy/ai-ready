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
@TableName("erp_supplier_level")
public class SupplierLevelEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("level_code")
    private String levelCode;

    @TableField("level_name")
    private String levelName;

    @TableField("level_description")
    private String levelDescription;

    @TableField("level_icon")
    private String levelIcon;

    @TableField("level_color")
    private String levelColor;

    @TableField("min_score")
    private BigDecimal minScore;

    @TableField("max_score")
    private BigDecimal maxScore;

    @TableField("evaluation_period")
    private String evaluationPeriod;

    @TableField("benefit_count")
    private Integer benefitCount;

    @TableField("max_discount_rate")
    private BigDecimal maxDiscountRate;

    @TableField("priority_level")
    private Integer priorityLevel;

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