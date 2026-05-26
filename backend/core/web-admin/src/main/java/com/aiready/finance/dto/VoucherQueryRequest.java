package com.aiready.finance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 凭证查询请求DTO
 */
@Data
public class VoucherQueryRequest {
    
    /**
     * 凭证字号
     */
    private String voucherNo;
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 会计期间
     */
    private String accountingPeriod;
    
    /**
     * 凭证类型
     */
    private Integer voucherType;
    
    /**
     * 制单人ID
     */
    private Long creatorId;
    
    /**
     * 审核状态（0：未审核 1：已审核）
     */
    private Integer reviewStatus;
    
    /**
     * 记账状态（0：未记账 1：已记账）
     */
    private Integer bookkeepingStatus;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 来源单据类型
     */
    private String sourceType;
    
    /**
     * 摘要关键词
     */
    private String summaryKeyword;
    
    /**
     * 当前页码
     */
    private Long current = 1L;
    
    /**
     * 每页大小
     */
    private Long size = 20L;
}
