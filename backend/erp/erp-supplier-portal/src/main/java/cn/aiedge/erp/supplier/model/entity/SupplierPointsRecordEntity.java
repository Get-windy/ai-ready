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
@TableName("erp_supplier_points_record")
public class SupplierPointsRecordEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("record_type")
    private String recordType;

    @TableField("points_amount")
    private BigDecimal pointsAmount;

    @TableField("record_description")
    private String recordDescription;

    @TableField("related_business_type")
    private String relatedBusinessType;

    @TableField("related_business_id")
    private String relatedBusinessId;

    @TableField("related_business_code")
    private String relatedBusinessCode;

    @TableField("business_time")
    private LocalDateTime businessTime;

    @TableField("record_date")
    private LocalDateTime recordDate;

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("current_balance")
    private BigDecimal currentBalance;

    @TableField("before_level_code")
    private String beforeLevelCode;

    @TableField("after_level_code")
    private String afterLevelCode;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("approval_status")
    private String approvalStatus;

    @TableField("approver_id")
    private String approverId;

    @TableField("approver_name")
    private String approverName;

    @TableField("approval_time")
    private LocalDateTime approvalTime;

    @TableField("approval_comment")
    private String approvalComment;

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