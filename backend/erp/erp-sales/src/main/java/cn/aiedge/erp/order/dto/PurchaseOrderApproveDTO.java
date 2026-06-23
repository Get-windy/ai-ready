package cn.aiedge.erp.order.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单审批DTO
 */
@Data
public class PurchaseOrderApproveDTO {
    
    @NotNull(message = "审批结果不能为空")
    private Integer approvalResult;
    
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String approvalOpinion;
    
    @Size(max = 500, message = "审批备注不能超过500个字符")
    private String approvalRemark;
    
    private Long approverId;
    
    private String approverName;
    
    private LocalDateTime approvalTime;
    
    private Integer approvalLevel;
    
    private String nextApproverId;
    
    private String nextApproverName;
    
    /**
     * 审批结果枚举
     */
    public enum ApprovalResult {
        APPROVED(1, "批准"),
        REJECTED(2, "拒绝"),
        RETURNED(3, "退回"),
        PENDING(4, "待审批");
        
        private final int code;
        private final String description;
        
        ApprovalResult(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ApprovalResult fromCode(int code) {
            for (ApprovalResult result : values()) {
                if (result.code == code) {
                    return result;
                }
            }
            return PENDING;
        }
    }
    
    /**
     * 审批状态
     */
    public enum ApprovalStatus {
        PENDING(0, "待审批"),
        APPROVING(1, "审批中"),
        APPROVED(2, "已批准"),
        REJECTED(3, "已拒绝"),
        CANCELLED(4, "已取消");
        
        private final int code;
        private final String description;
        
        ApprovalStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}