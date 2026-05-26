package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetCreateDTO {
    
    @NotBlank(message = "预算名称不能为空")
    private String budgetName;
    
    @NotNull(message = "预算类型不能为空")
    private Integer budgetType;
    
    @NotNull(message = "期间不能为空")
    private String period;
    
    private Integer year;
    
    private Integer quarter;
    
    private Integer month;
    
    private Long departmentId;
    
    private String departmentName;
    
    private Long projectId;
    
    private String projectName;
    
    @NotNull(message = "预算总额不能为空")
    private BigDecimal totalAmount;
    
    private String remark;
    
    @NotNull(message = "预算明细不能为空")
    private List<BudgetItemDTO> items;
    
    @Data
    public static class BudgetItemDTO {
        
        @NotNull(message = "控制类型不能为空")
        private Integer itemType;
        
        @NotNull(message = "科目不能为空")
        private Long subjectId;
        
        private String subjectCode;
        
        private String subjectName;
        
        private Long departmentId;
        
        private String departmentName;
        
        private Long projectId;
        
        private String projectName;
        
        @NotNull(message = "预算金额不能为空")
        private BigDecimal budgetAmount;
        
        private BigDecimal controlRate;
        
        private Integer controlLevel;
        
        private Integer alertThreshold;
        
        private String remark;
    }
}