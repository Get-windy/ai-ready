package cn.aiedge.erp.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_route_point")
public class RoutePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long routeId;

    private Integer pointOrder;

    private String orderId;

    private String orderNo;

    private String customerName;

    private String customerPhone;

    private String address;

    private String latitude;

    private String longitude;

    private Double distanceFromPrev;

    private Integer durationFromPrev;

    /** 点位状态：PENDING-待配送 IN_ROUTE-在途中 ARRIVED-已到达 DELIVERED-已送达 FAILED-配送失败 SKIPPED-已跳过 */
    private String status;

    private LocalDateTime arriveTime;

    private LocalDateTime leaveTime;

    /** 签收人（多点签收，逐点独立） */
    private String signee;

    /** 签收时间 */
    private LocalDateTime signTime;

    /** 配送失败原因（status=FAILED 时填写） */
    private String failReason;

    /** 预估到达时间（按剩余点位 + 地图能力估算，非承诺值） */
    private LocalDateTime etaTime;

    /** 是否被催单提前过：1=是（运营复盘用） */
    private Integer expedited;

    private String remark;

    /** 租户ID（明细表必须落租户列，多租户插件据此隔离） */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}