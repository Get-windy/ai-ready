package cn.aiedge.erp.sales.pricing.entity;

import cn.aiedge.erp.sales.pricing.enums.PriceApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格特批申请实体类
 * 用于管理需要特殊审批的价格申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price_special_approval")
public class PriceSpecialApproval {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 申请编号（业务唯一标识）
     */
    @Column(name = "approval_code", nullable = false, unique = true, length = 64)
    private String approvalCode;
    
    /**
     * 申请标题
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    /**
     * 申请描述/理由
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 申请人ID
     */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    @Column(name = "applicant_name", nullable = false, length = 128)
    private String applicantName;
    
    /**
     * 申请人部门ID
     */
    @Column(name = "applicant_department_id")
    private Long applicantDepartmentId;
    
    /**
     * 申请人部门名称
     */
    @Column(name = "applicant_department_name", length = 128)
    private String applicantDepartmentName;
    
    /**
     * 产品ID
     */
    @Column(name = "product_id")
    private Long productId;
    
    /**
     * 产品名称
     */
    @Column(name = "product_name", length = 255)
    private String productName;
    
    /**
     * 产品SKU
     */
    @Column(name = "product_sku", length = 64)
    private String productSku;
    
    /**
     * 产品类别ID
     */
    @Column(name = "product_category_id")
    private Long productCategoryId;
    
    /**
     * 产品类别名称
     */
    @Column(name = "product_category_name", length = 128)
    private String productCategoryName;
    
    /**
     * 客户ID
     */
    @Column(name = "customer_id")
    private Long customerId;
    
    /**
     * 客户名称
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;
    
    /**
     * 客户代码
     */
    @Column(name = "customer_code", length = 64)
    private String customerCode;
    
    /**
     * 申请价格类型（原价、折扣价、固定价等）
     */
    @Column(name = "price_type", length = 32)
    private String priceType;
    
    /**
     * 标准价格/原价
     */
    @Column(name = "standard_price", precision = 18, scale = 4)
    private BigDecimal standardPrice;
    
    /**
     * 申请价格
     */
    @Column(name = "applied_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal appliedPrice;
    
    /**
     * 折扣率（如果价格类型为折扣价）
     */
    @Column(name = "discount_rate", precision = 5, scale = 4)
    private BigDecimal discountRate;
    
    /**
     * 价格有效期开始时间
     */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    /**
     * 价格有效期结束时间
     */
    @Column(name = "valid_to")
    private LocalDateTime validTo;
    
    /**
     * 采购数量（如果适用）
     */
    @Column(name = "purchase_quantity", precision = 10, scale = 2)
    private BigDecimal purchaseQuantity;
    
    /**
     * 采购总额
     */
    @Column(name = "purchase_amount", precision = 18, scale = 4)
    private BigDecimal purchaseAmount;
    
    /**
     * 申请紧急程度（1-5，1为最紧急）
     */
    @Column(name = "urgency_level")
    private Integer urgencyLevel;
    
    /**
     * 申请优先级（1-3，1为最高优先级）
     */
    @Column(name = "priority_level")
    private Integer priorityLevel;
    
    /**
     * 审批状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PriceApprovalStatus status;
    
    /**
     * 当前审批级别（1-4）
     */
    @Column(name = "current_approval_level")
    private Integer currentApprovalLevel;
    
    /**
     * 需要审批的总级别数
     */
    @Column(name = "total_approval_levels")
    private Integer totalApprovalLevels;
    
    /**
     * 一级审批人ID
     */
    @Column(name = "level1_approver_id")
    private Long level1ApproverId;
    
    /**
     * 一级审批人姓名
     */
    @Column(name = "level1_approver_name", length = 128)
    private String level1ApproverName;
    
    /**
     * 二级审批人ID
     */
    @Column(name = "level2_approver_id")
    private Long level2ApproverId;
    
    /**
     * 二级审批人姓名
     */
    @Column(name = "level2_approver_name", length = 128)
    private String level2ApproverName;
    
    /**
     * 三级审批人ID
     */
    @Column(name = "level3_approver_id")
    private Long level3ApproverId;
    
    /**
     * 三级审批人姓名
     */
    @Column(name = "level3_approver_name", length = 128)
    private String level3ApproverName;
    
    /**
     * 最终审批人ID
     */
    @Column(name = "final_approver_id")
    private Long finalApproverId;
    
    /**
     * 最终审批人姓名
     */
    @Column(name = "final_approver_name", length = 128)
    private String finalApproverName;
    
    /**
     * 申请提交时间
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    /**
     * 一级审批时间
     */
    @Column(name = "level1_approved_at")
    private LocalDateTime level1ApprovedAt;
    
    /**
     * 二级审批时间
     */
    @Column(name = "level2_approved_at")
    private LocalDateTime level2ApprovedAt;
    
    /**
     * 三级审批时间
     */
    @Column(name = "level3_approved_at")
    private LocalDateTime level3ApprovedAt;
    
    /**
     * 最终审批时间
     */
    @Column(name = "final_approved_at")
    private LocalDateTime finalApprovedAt;
    
    /**
     * 审批拒绝时间
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    /**
     * 审批拒绝原因
     */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;
    
    /**
     * 审批撤回时间
     */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
    
    /**
     * 撤回原因
     */
    @Column(name = "withdraw_reason", columnDefinition = "TEXT")
    private String withdrawReason;
    
    /**
     * 审批过期时间
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    /**
     * 取消时间
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    /**
     * 取消原因
     */
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
    
    /**
     * 预计完成时间
     */
    @Column(name = "expected_completion_time")
    private LocalDateTime expectedCompletionTime;
    
    /**
     * 是否已通知申请人
     */
    @Column(name = "notified_applicant")
    private Boolean notifiedApplicant;
    
    /**
     * 是否已通知审批人
     */
    @Column(name = "notified_approvers")
    private Boolean notifiedApprovers;
    
    /**
     * 附件数量
     */
    @Column(name = "attachment_count")
    private Integer attachmentCount;
    
    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
    
    /**
     * 租户ID
     */
    @Column(name = "tenant_id")
    private Long tenantId;
    
    /**
     * 是否删除（0:未删除，1:已删除）
     */
    @Column(name = "deleted")
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 创建人ID
     */
    @Column(name = "create_by")
    private Long createBy;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    /**
     * 更新人ID
     */
    @Column(name = "update_by")
    private Long updateBy;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    // ========== 业务方法 ==========
    
    /**
     * 计算价格差异率
     */
    @Transient
    public BigDecimal getPriceDifferenceRate() {
        if (standardPrice == null || standardPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return standardPrice.subtract(appliedPrice)
                .divide(standardPrice, 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * 计算节约金额
     */
    @Transient
    public BigDecimal getSavingsAmount() {
        if (standardPrice == null) {
            return BigDecimal.ZERO;
        }
        return standardPrice.subtract(appliedPrice);
    }
    
    /**
     * 判断是否紧急
     */
    @Transient
    public boolean isUrgent() {
        return urgencyLevel != null && urgencyLevel <= 2;
    }
    
    /**
     * 判断是否高优先级
     */
    @Transient
    public boolean isHighPriority() {
        return priorityLevel != null && priorityLevel == 1;
    }
    
    /**
     * 判断是否需要多级审批
     */
    @Transient
    public boolean requiresMultiLevelApproval() {
        return totalApprovalLevels != null && totalApprovalLevels > 1;
    }
    
    /**
     * 判断审批是否超时
     */
    @Transient
    public boolean isApprovalTimeout() {
        if (submittedAt == null || expectedCompletionTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expectedCompletionTime) && 
               !status.isCompleted();
    }
    
    /**
     * 获取审批时长（分钟）
     */
    @Transient
    public Long getApprovalDurationMinutes() {
        if (submittedAt == null) {
            return 0L;
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        if (status.isCompleted()) {
            if (finalApprovedAt != null) {
                endTime = finalApprovedAt;
            } else if (rejectedAt != null) {
                endTime = rejectedAt;
            } else if (withdrawnAt != null) {
                endTime = withdrawnAt;
            } else if (cancelledAt != null) {
                endTime = cancelledAt;
            }
        }
        
        return java.time.Duration.between(submittedAt, endTime).toMinutes();
    }
    
    /**
     * 获取当前审批人的ID
     */
    @Transient
    public Long getCurrentApproverId() {
        if (currentApprovalLevel == null) {
            return null;
        }
        
        switch (currentApprovalLevel) {
            case 1:
                return level1ApproverId;
            case 2:
                return level2ApproverId;
            case 3:
                return level3ApproverId;
            case 4:
                return finalApproverId;
            default:
                return null;
        }
    }
    
    /**
     * 获取当前审批人姓名
     */
    @Transient
    public String getCurrentApproverName() {
        if (currentApprovalLevel == null) {
            return null;
        }
        
        switch (currentApprovalLevel) {
            case 1:
                return level1ApproverName;
            case 2:
                return level2ApproverName;
            case 3:
                return level3ApproverName;
            case 4:
                return finalApproverName;
            default:
                return null;
        }
    }
}