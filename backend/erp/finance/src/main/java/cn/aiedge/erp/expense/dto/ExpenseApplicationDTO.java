package cn.aiedge.erp.expense.dto;

import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import cn.aiedge.erp.expense.model.enumeration.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用申请DTO
 */
@Data
public class ExpenseApplicationDTO {
    private Long id;
    private String applicationCode;
    private String applicantId;
    private String applicantName;
    private String departmentId;
    private String departmentName;
    private LocalDate applyDate;
    private ExpenseType expenseType;
    private String expenseTypeDesc;
    private BigDecimal totalAmount;
    private String currency;
    private String budgetSubjectId;
    private String budgetSubjectName;
    private BigDecimal budgetAmount;
    private BigDecimal usedBudgetAmount;
    private BigDecimal budgetUsageRate;
    private String purpose;
    private String description;
    private ExpenseStatus status;
    private String statusDesc;
    private String currentApproverId;
    private String currentApproverName;
    private Integer currentApprovalLevel;
    private Integer totalApprovalLevel;
    private String processInstanceId;
    private String processDefinitionId;
    private String taskId;
    private PaymentMethod paymentMethod;
    private String paymentAccount;
    private LocalDate paymentDate;
    private String paymentVoucherNo;
    private LocalDate reimbursementDate;
    private String reimbursementVoucherNo;
    private Integer attachmentCount;
    private Boolean isUrgent;
    private String urgentReason;
    private LocalDate expectedCompletionDate;
    private LocalDate actualCompletionDate;
    private Boolean exceedBudget;
    private BigDecimal exceedAmount;
    private String exceedReason;
    private String approvalComment;
    private String rejectReason;
    private String cancelReason;
    private String remark;
    
    private List<ExpenseItemDTO> expenseItems;
    
    @Data
    public static class ExpenseItemDTO {
        private Long id;
        private String itemName;
        private String description;
        private LocalDate expenseDate;
        private BigDecimal amount;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String unit;
        private String vendorName;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal totalAmountWithTax;
        private Boolean hasInvoice;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private String paymentMethod;
        private String accountCode;
        private String budgetCode;
        private String projectCode;
        private String costCenter;
        private Boolean isPersonal;
        private Boolean isReimbursable;
        private Boolean receiptRequired;
        private Boolean receiptAttached;
        private String attachmentId;
        private Boolean isVerified;
        private String verifiedBy;
        private LocalDate verifiedDate;
        private String verificationComment;
        private Integer sequenceNumber;
    }
}