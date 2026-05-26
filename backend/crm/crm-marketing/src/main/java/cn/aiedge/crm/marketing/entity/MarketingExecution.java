package cn.aiedge.crm.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_marketing_execution")
public class MarketingExecution {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long campaignId;

    private Long targetId;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private Integer executionType;

    private Integer executionChannel;

    private String executionContent;

    private LocalDateTime executionTime;

    private Long executedBy;

    private String executedByName;

    private Integer executionResult;

    private String resultNote;

    private Integer responseStatus;

    private LocalDateTime responseTime;

    private String responseContent;

    private Long leadId;

    private Long opportunityId;

    private BigDecimal cost;

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