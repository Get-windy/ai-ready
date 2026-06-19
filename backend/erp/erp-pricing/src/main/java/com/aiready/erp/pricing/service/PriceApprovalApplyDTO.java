package com.aiready.erp.pricing.service;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格审批申请 DTO
 * 用于提交价格变更申请
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PriceApprovalApplyDTO {

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 客户ID（可选，为空时表示通用价格变更）
     */
    private Long customerId;

    /**
     * 新价格
     */
    private BigDecimal newPrice;

    /**
     * 审批类型（如 standard / urgent / override）
     */
    private String approvalType;

    /**
     * 申请原因
     */
    private String approvalReason;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 生效开始时间（毫秒时间戳）
     */
    private Long effectiveStartTime;

    /**
     * 生效结束时间（毫秒时间戳）
     */
    private Long effectiveEndTime;
}
