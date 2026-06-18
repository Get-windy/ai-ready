package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerOpportunityVO {

    private Long id;

    private String opportunityCode;

    private String opportunityName;

    private Long customerId;

    private String customerName;

    private Long leadId;

    private Integer opportunityStage;

    private String opportunityStageDesc;

    private BigDecimal estimatedAmount;

    private BigDecimal actualAmount;

    private Integer probability;

    private Integer opportunityType;

    private String opportunityTypeDesc;

    private Integer opportunitySource;

    private String opportunitySourceDesc;

    private String productInterest;

    private String requirement;

    private String competitor;

    private String winReason;

    private String loseReason;

    private Integer status;

    private String statusDesc;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private LocalDate expectedCloseDate;

    private LocalDate actualCloseDate;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Integer version;
}