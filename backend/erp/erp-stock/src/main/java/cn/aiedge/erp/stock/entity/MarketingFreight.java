package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_marketing_freight")
public class MarketingFreight {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ruleId;
    private String freightType;
    private BigDecimal freightValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxReduceAmount;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
