package cn.aiedge.erp.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_delivery_route")
public class DeliveryRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 路线编号，号段 PSXL-YYYYMMDD-序号（同租户唯一） */
    private String routeCode;

    /** 线路档案ID（erp_route，主数据引用；执行单与档案严格分表，见《配送路线单开发文档》） */
    private Long routeId;

    /** 线路名称快照 */
    private String routeName;

    /** 线路类型快照：SELF-自配 / LOGISTICS-物流 */
    private String routeType;

    private String deliveryPersonId;

    private String deliveryPersonName;

    /** 车辆ID（dms_vehicle） */
    private Long vehicleId;

    /** 车牌号快照 */
    private String vehicleNo;

    /** 绑定的电子围栏ID（dms_geo_fence；档案在《路线规划》维护，本页只绑定引用） */
    private Long fenceId;

    /** 围栏名称快照 */
    private String fenceName;

    /** 是否参与「围栏自动归集」1=参与 0=仅手工维护 */
    private Integer autoCollect;

    /** 计划配送日期 */
    private LocalDate planDate;

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

    /** 实际配送时长（分钟，完成时按 完成时间 − 开始时间 计算） */
    private Integer actualDuration;

    /** 配送失败点位数（按点位状态派生） */
    private Integer failedPoints;

    private String routeData;

    private String optimizedRouteData;

    /** 状态：PLANNING-规划中 READY-待出发 IN_PROGRESS-配送中 COMPLETED-已完成 CANCELLED-已取消 */
    private String status;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    /** 创建人姓名快照（审计列，sys_user.username） */
    private String createByName;

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