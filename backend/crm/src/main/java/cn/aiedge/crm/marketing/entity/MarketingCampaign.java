package cn.aiedge.crm.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_marketing_campaign")
public class MarketingCampaign {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String campaignCode;

    private String campaignName;

    private Integer campaignType;

    private Integer campaignCategory;

    private String description;

    private String objective;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private BigDecimal budget;

    private BigDecimal actualCost;

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

    private Integer conversionRate;

    private Integer responseRate;

    private String remark;

    private String internalNote;

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