package cn.aiedge.erp.sales.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格特批申请请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceApprovalRequestDTO {
    
    /**
     * 申请标题
     */
    @NotBlank(message = "申请标题不能为空")
    private String title;
    
    /**
     * 申请描述/理由
     */
    @NotBlank(message = "申请描述不能为空")
    private String description;
    
    /**
     * 申请人ID
     */
    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;
    
    /**
     * 申请人部门ID
     */
    private Long applicantDepartmentId;
    
    /**
     * 申请人部门名称
     */
    private String applicantDepartmentName;
    
    /**
     * 产品ID
     */
    private Long productId;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品SKU
     */
    private String productSku;
    
    /**
     * 产品类别ID
     */
    private Long productCategoryId;
    
    /**
     * 产品类别名称
     */
    private String productCategoryName;
    
    /**
     * 客户ID
     */
    private Long customerId;
    
    /**
     * 客户名称
     */
    private String customerName;
    
    /**
     * 客户代码
     */
    private String customerCode;
    
    /**
     * 申请价格类型
     */
    private String priceType;
    
    /**
     * 标准价格/原价
     */
    private BigDecimal standardPrice;
    
    /**
     * 申请价格
     */
    @NotNull(message = "申请价格不能为空")
    private BigDecimal appliedPrice;
    
    /**
     * 折扣率
     */
    private BigDecimal discountRate;
    
    /**
     * 价格有效期开始时间
     */
    private LocalDateTime validFrom;
    
    /**
     * 价格有效期结束时间
     */
    private LocalDateTime validTo;
    
    /**
     * 采购数量
     */
    private BigDecimal purchaseQuantity;
    
    /**
     * 采购总额
     */
    private BigDecimal purchaseAmount;
    
    /**
     * 申请紧急程度（1-5）
     */
    @NotNull(message = "紧急程度不能为空")
    private Integer urgencyLevel;
    
    /**
     * 申请优先级（1-3）
     */
    private Integer priorityLevel;
    
    /**
     * 需要审批的总级别数
     */
    @NotNull(message = "审批级别数不能为空")
    private Integer totalApprovalLevels;
    
    /**
     * 一级审批人ID
     */
    private Long level1ApproverId;
    
    /**
     * 一级审批人姓名
     */
    private String level1ApproverName;
    
    /**
     * 二级审批人ID
     */
    private Long level2ApproverId;
    
    /**
     * 二级审批人姓名
     */
    private String level2ApproverName;
    
    /**
     * 三级审批人ID
     */
    private Long level3ApproverId;
    
    /**
     * 三级审批人姓名
     */
    private String level3ApproverName;
    
    /**
     * 最终审批人ID
     */
    private Long finalApproverId;
    
    /**
     * 最终审批人姓名
     */
    private String finalApproverName;
    
    /**
     * 预计完成时间
     */
    private LocalDateTime expectedCompletionTime;
    
    /**
     * 附件信息列表
     */
    private List<AttachmentDTO> attachments;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    // ========== 验证方法 ==========
    
    /**
     * 验证申请数据的有效性
     */
    public boolean isValid() {
        if (appliedPrice == null || appliedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (urgencyLevel < 1 || urgencyLevel > 5) {
            return false;
        }
        
        if (priorityLevel != null && (priorityLevel < 1 || priorityLevel > 3)) {
            return false;
        }
        
        if (totalApprovalLevels < 1 || totalApprovalLevels > 4) {
            return false;
        }
        
        // 验证价格有效期
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            return false;
        }
        
        // 验证折扣率
        if (discountRate != null && (discountRate.compareTo(BigDecimal.ZERO) < 0 || 
                                     discountRate.compareTo(BigDecimal.ONE) > 0)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取价格差异
     */
    public BigDecimal getPriceDifference() {
        if (standardPrice == null) {
            return BigDecimal.ZERO;
        }
        return standardPrice.subtract(appliedPrice);
    }
    
    /**
     * 获取价格差异率
     */
    public BigDecimal getPriceDifferenceRate() {
        if (standardPrice == null || standardPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return standardPrice.subtract(appliedPrice)
                .divide(standardPrice, 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * 判断是否紧急
     */
    public boolean isUrgent() {
        return urgencyLevel != null && urgencyLevel <= 2;
    }
    
    /**
     * 判断是否高优先级
     */
    public boolean isHighPriority() {
        return priorityLevel != null && priorityLevel == 1;
    }
    
    /**
     * 是否需要多级审批
     */
    public boolean requiresMultiLevelApproval() {
        return totalApprovalLevels != null && totalApprovalLevels > 1;
    }
}