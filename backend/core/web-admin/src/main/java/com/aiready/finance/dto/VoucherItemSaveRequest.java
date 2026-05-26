package com.aiready.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 凭证分录保存请求DTO
 */
@Data
public class VoucherItemSaveRequest {
    
    /**
     * 分录序号
     */
    private Integer itemNo;
    
    /**
     * 科目ID
     */
    @NotNull(message = "科目不能为空")
    private Long subjectId;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 借方金额
     */
    private BigDecimal debitAmount;
    
    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;
    
    /**
     * 辅助核算-客户ID
     */
    private Long customerId;
    
    /**
     * 辅助核算-供应商ID
     */
    private Long supplierId;
    
    /**
     * 辅助核算-部门ID
     */
    private Long departmentId;
    
    /**
     * 辅助核算-项目ID
     */
    private Long projectId;
    
    /**
     * 辅助核算-员工ID
     */
    private Long employeeId;
    
    /**
     * 外币币种
     */
    private String currency;
    
    /**
     * 外币金额
     */
    private BigDecimal foreignAmount;
    
    /**
     * 汇率
     */
    private BigDecimal exchangeRate;
}
