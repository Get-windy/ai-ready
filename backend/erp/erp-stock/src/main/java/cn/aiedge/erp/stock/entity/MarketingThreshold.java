package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_marketing_threshold")
public class MarketingThreshold {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ruleId;
    private String thresholdType;
    private BigDecimal thresholdValue;
    private String benefitType;
    private BigDecimal benefitValue;
    private Integer isMultiGrade;
    private Long nextRuleId;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
