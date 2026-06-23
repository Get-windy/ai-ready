package cn.aiedge.erp.purchase.entity;

import cn.aiedge.erp.purchase.enums.ContractStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购合同实体
 */
@Data
@Accessors(chain = true)
public class PurchaseContract {

    private Long id;

    private String contractNo;

    private Long inquiryId;

    private Long quoteId;

    private Long supplierId;

    private String supplierName;

    private String contractTitle;

    private String contractType;

    private ContractStatus contractStatus;
    
    /**
     * 为测试代码提供兼容方法
     */
    public ContractStatus getStatus() {
        return contractStatus;
    }

    private BigDecimal totalAmount;

    private BigDecimal executedAmount;

    private BigDecimal executedPercent;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String paymentTerms;

    private String deliveryTerms;

    private String qualityStandard;

    private String warrantyPeriod;

    private LocalDateTime submitTime;

    private Long approverId;

    private LocalDateTime approvalTime;

    private String approvalComment;

    private LocalDateTime activationTime;

    private LocalDateTime completionTime;

    private LocalDateTime terminationTime;

    private String terminationReason;

    private String archiveNo;

    private LocalDateTime archiveTime;

    private String modificationNo;

    private String modificationReason;

    private String contractFileUrl;

    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}