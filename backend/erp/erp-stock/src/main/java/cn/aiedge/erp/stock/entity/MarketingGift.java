package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_marketing_gift")
public class MarketingGift {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ruleId;
    private String giftType;
    private Long giftProductId;
    private Integer giftQuantity;
    private Integer maxGifts;
    private BigDecimal minOrderAmount;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
