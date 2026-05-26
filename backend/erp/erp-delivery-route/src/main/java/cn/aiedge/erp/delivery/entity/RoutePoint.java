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

    private String status;

    private LocalDateTime arriveTime;

    private LocalDateTime leaveTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}