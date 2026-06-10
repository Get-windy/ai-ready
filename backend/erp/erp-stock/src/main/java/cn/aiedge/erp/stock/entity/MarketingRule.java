package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Accessors(chain = true)
@TableName("erp_marketing_rule")
public class MarketingRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String ruleSubtype;
    private Integer priority;
    private Integer isStackable;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String timeLimitType;
    private Integer weeklyBits;
    private LocalTime dailyStart;
    private LocalTime dailyEnd;
    private BigDecimal minOrderAmount;
    private BigDecimal maxOrderAmount;
    private Integer minQuantity;
    private Integer maxQuantity;
    private String applicablePartnerTypes;
    private String applicablePartnerGradeIds;
    private String applicableRegionIds;
    private Integer usageLimitTotal;
    private Integer usageLimitPerCustomer;
    private Integer useCount;
    private BigDecimal maxDiscountAmount;
    private String status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
