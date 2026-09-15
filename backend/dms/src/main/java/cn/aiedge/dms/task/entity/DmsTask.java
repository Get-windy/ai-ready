package cn.aiedge.dms.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /** 来源单据编号（与订单号区分：任务内单据可一对多） */
    private String sourceBillNo;

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

    // === 配送单台账列（配送单[历史] 列表；对齐《配送查询》口径） ===

    /** 指定配送日期（计划配送日） */
    private LocalDate deliveryDate;

    /** 司机名称快照（司机与送货员是两个角色） */
    private String riderName;

    /** 配送车辆ID（dms_vehicle.id） */
    private Long vehicleId;

    /** 配送车辆快照（车牌号） */
    private String vehicleName;

    /** 送货员ID（dms_rider.id；与司机同为配送员档案） */
    private Long deliverymanId;

    /** 送货员名称快照 */
    private String deliverymanName;

    /** 配送线路ID（erp_route.id 线路主数据） */
    private Long routeId;

    /** 配送区域（线路档案配送区域快照） */
    private String routeArea;

    /** 配送单量（本任务聚合的配送单据数） */
    private Integer orderCount;

    /** 订金金额 */
    private BigDecimal depositAmount;

    /** 退货单量 */
    private Integer returnOrderCount;

    /** 退货数量 */
    private BigDecimal returnQuantity;

    /** 退货金额 */
    private BigDecimal returnAmount;

    /** 装箱数量 */
    private BigDecimal boxQuantity;

    /** 打印次数 */
    private Integer printCount;

    /** 制单人名称快照 */
    private String creatorName;

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

    // === 非持久化展示字段（《调度任务开发文档》§3.2 调度工作台列，由调度查询联查/聚合填充） ===

    /** 配送员电话（联查 dms_rider 补全，任务无快照列） */
    @TableField(exist = false)
    private String riderPhone;

    /** 配送员当前在途单数（负载） */
    @TableField(exist = false)
    private Integer activeTaskCount;

    /** 距目的地直线距离（km，取配送员当前位置 → 收货地址坐标） */
    @TableField(exist = false)
    private BigDecimal distanceKm;

    /** 是否超时（在途任务已过要求送达时间） */
    @TableField(exist = false)
    private Boolean overdue;

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
