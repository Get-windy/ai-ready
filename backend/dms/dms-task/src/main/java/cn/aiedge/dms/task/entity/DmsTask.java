package cn.aiedge.dms.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送任务实体
 */
@Data
@TableName("dms_task")
public class DmsTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    @NotBlank(message = "任务编号不能为空")
    private String taskNo;

    private Long orderId;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** 1-销售配送 2-调拨 3-退货 */
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    private Long channelId;

    private Long riderId;

    /** 1-自动 2-手动 3-抢单 4-竞价 */
    private Integer dispatchType;

    private Long sourceWarehouseId;

    private String sourceAddress;

    private BigDecimal sourceLat;

    private BigDecimal sourceLng;

    private Long customerId;

    private String customerName;

    private String customerPhone;

    private String customerAddress;

    private BigDecimal customerLat;

    private BigDecimal customerLng;

    private Integer totalItems;

    private BigDecimal totalQuantity;

    private BigDecimal totalWeight;

    private BigDecimal totalVolume;

    private BigDecimal goodsAmount;

    private BigDecimal deliveryFee;

    private BigDecimal collectOnDelivery;

    private BigDecimal estimatedDistance;

    /** 1-普通 2-紧急 3-加急 */
    private Integer priority;

    /** 0-待分配 1-已分配 2-已接单 3-取货中 4-配送中 5-已签收 6-已完成 7-已取消 8-异常 */
    private Integer status;

    private Integer urgeCount;

    private LocalDateTime urgeTime;

    private LocalDateTime loadTime;

    private LocalDateTime dispatchTime;

    private LocalDateTime pickupTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime completedTime;

    private LocalDateTime deadlineTime;

    private String remark;

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
