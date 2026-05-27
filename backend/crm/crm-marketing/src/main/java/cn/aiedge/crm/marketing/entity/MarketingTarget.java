package cn.aiedge.crm.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_marketing_target")
public class MarketingTarget {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long campaignId;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private Integer customerLevel;

    private String customerIndustry;

    private String customerRegion;

    private Integer targetPriority;

    private Integer targetStatus;

    private Integer reachStatus;

    private LocalDateTime reachTime;

    private Integer reachChannel;

    private Integer responseStatus;

    private LocalDateTime responseTime;

    private String responseContent;

    private Integer conversionStatus;

    private LocalDateTime conversionTime;

    private Long leadId;

    private Long opportunityId;

    private Long orderId;

    private BigDecimal orderAmount;

    private Integer score;

    private String tags;

    private String remark;

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
}