package cn.aiedge.erp.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_delivery_route")
public class DeliveryRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String routeCode;

    private String deliveryPersonId;

    private String deliveryPersonName;

    private Integer totalPoints;

    private Integer completedPoints;

    private String startPoint;

    private String startLatitude;

    private String startLongitude;

    private String endPoint;

    private String endLatitude;

    private String endLongitude;

    private Double totalDistance;

    private Integer totalDuration;

    private String routeData;

    private String optimizedRouteData;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private String remark;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}