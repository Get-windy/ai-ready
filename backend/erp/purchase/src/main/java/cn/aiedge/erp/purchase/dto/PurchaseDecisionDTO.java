package cn.aiedge.erp.purchase.dto;

import cn.aiedge.erp.purchase.enums.ApprovalStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购决策DTO
 */
@Data
public class PurchaseDecisionDTO {
    
    private Long inquiryId;
    
    private Long selectedQuoteId;
    
    private String decisionMaker;
    
    private LocalDateTime decisionDate;
    
    private ApprovalStatus approvalStatus;
    
    private String comments;
    
    private String nextSteps;
}
