package com.aiready.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 凭证分录DTO
 */
@Data
public class VoucherItemDTO {
    
    private Long id;
    
    /**
     * 分录序号
     */
    private Integer itemNo;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 科目编码
     */
    private String subjectCode;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
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
     * 辅助核算-客户名称
     */
    private String customerName;
    
    /**
     * 辅助核算-供应商ID
     */
    private Long supplierId;
    
    /**
     * 辅助核算-供应商名称
     */
    private String supplierName;
    
    /**
     * 辅助核算-部门ID
     */
    private Long departmentId;
    
    /**
     * 辅助核算-部门名称
     */
    private String departmentName;
    
    /**
     * 辅助核算-项目ID
     */
    private Long projectId;
    
    /**
     * 辅助核算-项目名称
     */
    private String projectName;
    
    /**
     * 辅助核算-员工ID
     */
    private Long employeeId;
    
    /**
     * 辅助核算-员工姓名
     */
    private String employeeName;
    
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
