package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_group_buy_activity")
public class GroupBuyActivity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String activityCode;
    private String activityName;
    private Long productId;
    private BigDecimal originalPrice;
    private BigDecimal groupPrice;
    private Integer minGroupSize;
    private Integer maxGroupSize;
    private Integer timeLimitMinutes;
    private Integer quantityLimit;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
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
