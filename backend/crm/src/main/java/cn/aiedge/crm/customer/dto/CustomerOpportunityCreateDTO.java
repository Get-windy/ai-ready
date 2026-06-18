package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CustomerOpportunityCreateDTO {

    private String opportunityName;

    private Long customerId;

    private Long leadId;

    private Integer opportunityStage;

    private BigDecimal estimatedAmount;

    private Integer probability;

    private Integer opportunityType;

    private Integer opportunitySource;

    private String productInterest;

    private String requirement;

    private String competitor;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private LocalDate expectedCloseDate;

    private String remark;
}