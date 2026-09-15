package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 配送路线单（执行单）列表 / 详情 VO
 *
 * 红线：本 VO 描述的是**执行单**（谁跑、跑到哪、开始/完成/取消），
 *      与线路档案主数据（erp_route，资料 → 配送管理 → 线路）严格区分。
 */
@Data
public class DeliveryRouteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 路线编号 PSXL-YYYYMMDD-序号 */
    private String routeCode;

    private Long routeId;

    /** 线路名称（引用线路档案的快照） */
    private String routeName;

    /** SELF-自配 / LOGISTICS-物流 */
    private String routeType;

    private String routeTypeText;

    private String deliveryPersonId;

    private String deliveryPersonName;

    private Long vehicleId;

    private String vehicleNo;

    /** 绑定的电子围栏ID（dms_geo_fence） */
    private Long fenceId;

    private String fenceName;

    /** 是否参与围栏自动归集 1/0 */
    private Integer autoCollect;

    private LocalDate planDate;

    private Integer totalPoints;

    private Integer completedPoints;

    private Integer failedPoints;

    /** 完成进度（已送达 / 总点位），列表口径 */
    private String progress;

    private String startPoint;

    private String endPoint;

    private Double totalDistance;

    private Integer totalDuration;

    /** 实际时长（分钟） */
    private Integer actualDuration;

    private String status;

    private String statusText;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 点位明细（详情接口返回） */
    private List<RoutePointResult> points = new ArrayList<>();
}
