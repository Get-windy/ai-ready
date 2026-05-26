package cn.aiedge.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_contract")
public class Contract {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String contractNo;

    private String contractName;

    private Integer contractType;

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

    private Integer signMethod;

    private BigDecimal contractAmount;

    private BigDecimal paidAmount;

    private BigDecimal pendingAmount;

    private String currency;

    private BigDecimal exchangeRate;

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

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}