package com.aiready.finance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 账簿查询请求DTO
 */
@Data
public class LedgerQueryRequest {
    
    /**
     * 账簿类型（1：明细账 2：总账）
     */
    private Integer ledgerType;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 会计期间
     */
    private String accountingPeriod;
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
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
     * 当前页码
     */
    private Long current = 1L;
    
    /**
     * 每页大小
     */
    private Long size = 20L;
}
