package com.aiready.erp.pricing.service;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格审批统计 DTO
 * 用于展示审批概览数据
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PriceApprovalStatistics {

    /**
     * 总申请数
     */
    private long totalCount;

    /**
     * 待审批数
     */
    private long pendingCount;

    /**
     * 已通过数
     */
    private long approvedCount;

    /**
     * 已拒绝数
     */
    private long rejectedCount;

    /**
     * 已通过的总涨价金额
     */
    private BigDecimal totalPriceIncrease;

    /**
     * 已通过的总降价金额
     */
    private BigDecimal totalPriceDecrease;
}
