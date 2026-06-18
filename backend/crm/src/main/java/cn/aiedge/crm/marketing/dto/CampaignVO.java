package cn.aiedge.crm.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CampaignVO {

    private Long id;

    private String campaignCode;

    private String campaignName;

    private Integer campaignType;

    private String campaignTypeDesc;

    private Integer campaignCategory;

    private String description;

    private String objective;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private String statusDesc;

    private BigDecimal budget;

    private BigDecimal actualCost;

    private BigDecimal totalCost;

    private BigDecimal expectedRevenue;

    private BigDecimal actualRevenue;

    private Integer expectedLeads;

    private Integer actualLeads;

    private Integer expectedOpportunities;

    private Integer actualOpportunities;

    private Integer expectedOrders;

    private Integer actualOrders;

    private Integer targetCustomerCount;

    private Integer reachedCustomerCount;

    private Integer respondedCustomerCount;

    private Integer convertedCustomerCount;

    private String targetAudience;

    private String targetRegion;

    private Integer targetIndustry;

    private Integer targetCustomerLevel;

    private String targetProductCategory;

    private Long ownerId;

    private String ownerName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long startedBy;

    private LocalDateTime startedTime;

    private Long completedBy;

    private LocalDateTime completedTime;

    private String completedNote;

    private Integer roi;

    private Integer roiPercentage;

    private Integer conversionRate;

    private Integer responseRate;

    private String remark;

    private String internalNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Boolean isRunning;

    private Boolean isEnded;
}