package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_marketing_tiered")
public class MarketingTiered {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ruleId;
    private String tierType;
    private BigDecimal tierMin;
    private BigDecimal tierMax;
    private String priceType;
    private BigDecimal priceValue;
    private Integer sortOrder;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
