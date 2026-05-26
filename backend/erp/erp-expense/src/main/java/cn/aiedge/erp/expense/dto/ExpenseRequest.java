package cn.aiedge.erp.expense.dto;

import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import cn.aiedge.erp.expense.model.enumeration.PaymentMethod;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用申请请求DTO
 */
@Data
public class ExpenseRequest {
    
    @NotBlank(message = "申请人ID不能为空")
    private String applicantId;
    
    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;
    
    private String departmentId;
    
    private String departmentName;
    
    @NotNull(message = "申请日期不能为空")
    private LocalDate applyDate;
    
    @NotNull(message = "费用类型不能为空")
    private ExpenseType expenseType;
    
    @NotNull(message = "费用总金额不能为空")
    @Positive(message = "费用总金额必须大于0")
    private BigDecimal totalAmount;
    
    private String currency = "CNY";
    
    private String budgetSubjectId;
    
    private String budgetSubjectName;
    
    private BigDecimal budgetAmount;
    
    @NotBlank(message = "费用事由不能为空")
    private String purpose;
    
    private String description;
    
    private PaymentMethod paymentMethod;
    
    private String paymentAccount;
    
    private Boolean isUrgent = false;
    
    private String urgentReason;
    
    private LocalDate expectedCompletionDate;
    
    private String exceedReason;
    
    private List<ExpenseItemRequest> expenseItems;
    
    @Data
    public static class ExpenseItemRequest {
        @NotBlank(message = "费用项名称不能为空")
        private String itemName;
        
        private String description;
        
        @NotNull(message = "费用日期不能为空")
        private LocalDate expenseDate;
        
        @NotNull(message = "金额不能为空")
        @Positive(message = "金额必须大于0")
        private BigDecimal amount;
        
        private BigDecimal quantity = BigDecimal.ONE;
        
        private String unit;
        
        private String vendorName;
        
        private BigDecimal taxRate = BigDecimal.ZERO;
        
        private Boolean hasInvoice = false;
        
        private String invoiceNumber;
        
        private LocalDate invoiceDate;
        
        private String paymentMethod;
        
        private String accountCode;
        
        private String budgetCode;
        
        private String projectCode;
        
        private String costCenter;
        
        private Boolean isPersonal = false;
        
        private Boolean isReimbursable = true;
        
        private Boolean receiptRequired = true;
        
        private Boolean receiptAttached = false;
    }
}