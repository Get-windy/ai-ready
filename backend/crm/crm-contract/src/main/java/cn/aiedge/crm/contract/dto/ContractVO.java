package cn.aiedge.crm.contract.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContractVO {

    private Long id;

    private String contractNo;

    private String contractName;

    private Integer contractType;

    private String contractTypeDesc;

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private String opportunityName;

    private Long quotationId;

    private String quotationNo;

    private Long contactId;

    private String contactName;

    private LocalDate signDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private String statusDesc;

    private Integer signMethod;

    private String signMethodDesc;

    private BigDecimal contractAmount;

    private BigDecimal paidAmount;

    private BigDecimal pendingAmount;

    private BigDecimal paidPercentage;

    private String currency;

    private String paymentTerms;

    private Integer paymentDays;

    private Integer paymentMethod;

    private String deliveryTerms;

    private Integer deliveryDays;

    private String warrantyTerms;

    private Integer warrantyMonths;

    private String serviceTerms;

    private Integer serviceMonths;

    private String title;

    private String description;

    private String remark;

    private String internalNote;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long signedBy;

    private LocalDateTime signedTime;

    private String signedLocation;

    private Long effectiveBy;

    private LocalDateTime effectiveTime;

    private Long terminatedBy;

    private LocalDateTime terminatedTime;

    private String terminatedReason;

    private Long renewedBy;

    private LocalDateTime renewedTime;

    private Long renewedContractId;

    private String renewedContractNo;

    private Integer renewalCount;

    private Boolean autoRenewal;

    private Integer renewalNoticeDays;

    private Boolean needReview;

    private Long reviewedBy;

    private LocalDateTime reviewedTime;

    private String reviewedNote;

    private Integer executionProgress;

    private BigDecimal executionAmount;

    private Integer riskLevel;

    private String riskNote;

    private String archiveLocation;

    private LocalDateTime archiveTime;

    private Long archivedBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Integer daysRemaining;

    private Boolean isExpiring;

    private Boolean isExpired;
}