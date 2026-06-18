package cn.aiedge.dms.orderpool.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单大厅实体
 */
@Data
@TableName("dms_order_pool")
public class DmsOrderPool {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long taskId;

    private BigDecimal deliveryFee;

    /** 0-否 1-是 */
    private Integer bidEnabled;

    private BigDecimal bidStartPrice;

    private BigDecimal bidCurrentPrice;

    private LocalDateTime bidStartTime;

    private LocalDateTime bidEndTime;

    private Integer bidCount;

    /** 0-待抢单 1-竞价中 2-已接单 3-已过期 4-已下架 */
    private Integer poolStatus;

    private LocalDateTime publishedTime;

    // === Audit fields ===

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
    private Integer version;
}
