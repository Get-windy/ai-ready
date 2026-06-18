package cn.aiedge.crm.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CampaignCreateDTO {

    private String campaignName;

    private Integer campaignType;

    private Integer campaignCategory;

    private String description;

    private String objective;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal budget;

    private BigDecimal expectedRevenue;

    private Integer expectedLeads;

    private Integer expectedOpportunities;

    private Integer expectedOrders;

    private String targetAudience;

    private String targetRegion;

    private Integer targetIndustry;

    private Integer targetCustomerLevel;

    private String targetProductCategory;

    private Long ownerId;

    private String ownerName;

    private Long departmentId;

    private String departmentName;

    private String remark;

    private String internalNote;

    private List<Long> targetCustomerIds;
}