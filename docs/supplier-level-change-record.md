package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 等级变更记录实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_level_record")
@Entity
@Table(name = "erp_supplier_level_record", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_supplier_id", columnList = "supplier_id"),
    @Index(name = "idx_change_type", columnList = "change_type"),
    @Index(name = "idx_change_date", columnList = "change_date")
})
public class SupplierLevelRecordEntity {
    
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
     * 供应商ID
     */
    @TableField("supplier_id")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
    
    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;
    
    /**
     * 变更前等级编码
     */
    @TableField("before_level_code")
    @Column(name = "before_level_code", nullable = false, length = 20)
    private String beforeLevelCode;
    
    /**
     * 变更后等级编码
     */
    @TableField("after_level_code")
    @Column(name = "after_level_code", nullable = false, length = 20)
    private String afterLevelCode;
    
    /**
     * 变更类型：UPGRADE-升级，DOWNGRADE-降级，MANUAL-手动调整，REVIEW-定期评估
     */
    @TableField("change_type")
    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;
    
    /**
     * 变更原因
     */
    @TableField("change_reason")
    @Column(name = "change_reason", length = 500)
    private String changeReason;
    
    /**
     * 变更描述
     */
    @TableField("change_description")
    @Column(name = "change_description", length = 1000)
    private String changeDescription;
    
    /**
     * 变更时间
     */
    @TableField("change_date")
    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;
    
    /**
     * 评估周期
     */
    @TableField("evaluation_period")
    @Column(name = "evaluation_period", length = 20)
    private String evaluationPeriod;
    
    /**
     * 综合评分
     */
    @TableField("comprehensive_score")
    @Column(name = "comprehensive_score", precision = 10, scale = 2)
    private BigDecimal comprehensiveScore;
    
    /**
     * 累计积分
     */
    @TableField("total_points")
    @Column(name = "total_points", precision = 10, scale = 2)
    private BigDecimal totalPoints;
    
    /**
     * 质量评分
     */
    @TableField("quality_score")
    @Column(name = "quality_score", precision = 10, scale = 2)
    private BigDecimal qualityScore;
    
    /**
     * 交付评分
     */
    @TableField("delivery_score")
    @Column(name = "delivery_score", precision = 10, scale = 2)
    private BigDecimal deliveryScore;
    
    /**
     * 价格评分
     */
    @TableField("price_score")
    @Column(name = "price_score", precision = 10, scale = 2)
    private BigDecimal priceScore;
    
    /**
     * 服务评分
     */
    @TableField("service_score")
    @Column(name = "service_score", precision = 10, scale = 2)
    private BigDecimal serviceScore;
    
    /**
     * 技术评分
     */
    @TableField("technology_score")
    @Column(name = "technology_score", precision = 10, scale = 2)
    private BigDecimal technologyScore;
    
    /**
     * 响应速度评分
     */
    @TableField("response_score")
    @Column(name = "response_score", precision = 10, scale = 2)
    private BigDecimal responseScore;
    
    /**
     * 合规性评分
     */
    @TableField("compliance_score")
    @Column(name = "compliance_score", precision = 10, scale = 2)
    private BigDecimal complianceScore;
    
    /**
     * 触发规则编码
     */
    @TableField("trigger_rule_code")
    @Column(name = "trigger_rule_code", length = 50)
    private String triggerRuleCode;
    
    /**
     * 操作人ID
     */
    @TableField("operator_id")
    @Column(name = "operator_id", length = 50)
    private String operatorId;
    
    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    @Column(name = "operator_name", length = 50)
    private String operatorName;
    
    /**
     * 审批状态：PENDING-待审批，APPROVED-已审批，REJECTED-已拒绝
     */
    @TableField("approval_status")
    @Column(name = "approval_status", length = 20)
    private String approvalStatus;
    
    /**
     * 审批人ID
     */
    @TableField("approver_id")
    @Column(name = "approver_id", length = 50)
    private String approverId;
    
    /**
     * 审批人姓名
     */
    @TableField("approver_name")
    @Column(name = "approver_name", length = 50)
    private String approverName;
    
    /**
     * 审批时间
     */
    @TableField("approval_time")
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;
    
    /**
     * 审批意见
     */
    @TableField("approval_comment")
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;
    
    /**
     * 有效期开始时间
     */
    @TableField("valid_start_time")
    @Column(name = "valid_start_time")
    private LocalDateTime validStartTime;
    
    /**
     * 有效期结束时间
     */
    @TableField("valid_end_time")
    @Column(name = "valid_end_time")
    private LocalDateTime validEndTime;
    
    /**
     * 附件信息（JSON格式）
     */
    @TableField("attachment_info")
    @Column(name = "attachment_info", length = 1000)
    private String attachmentInfo;
    
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