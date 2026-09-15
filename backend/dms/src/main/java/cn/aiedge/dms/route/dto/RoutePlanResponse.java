package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 路线规划响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanResponse {

    /** 是否成功 */
    private boolean success;

    /** 错误信息（成功时可能是降级提示，如未配置地图 Key） */
    private String message;

    /** 实际生效的地图服务商：amap/tencent/baidu/local */
    private String provider;

    /** 是否为降级结果（直线模式） */
    private boolean degraded;

    /** 出行方式：DRIVING/CYCLING/WALKING */
    private String travelMode;

    /** 坐标体系（统一 GCJ-02） */
    private String coordSystem;

    /** 总距离（米） */
    private Long totalDistance;

    /** 预计时长（秒） */
    private Long totalDuration;

    /** 预计费用（元，打车场景） */
    private Double totalToll;

    /** 路线坐标点串（简化后，用于地图绘制） */
    private List<RoutePoint> routePoints;

    /** 分段导航信息 */
    private List<RouteStep> steps;

    /** 优化后的途经点顺序（按输入 stops 的下标） */
    private List<Integer> optimizedOrder;

    /** 规划结果点位列表（含起点/途经点，按访问顺序） */
    private List<RouteStop> stops;

    /** 车辆分批结果（启用容量/时间窗约束时有值；一车一批） */
    private List<RouteBatch> batches;

    /** 约束告警（晚于时间窗、超出载重、车辆数不足等，不阻断规划） */
    private List<String> warnings;

    /** 未能排入的点位（硬时间窗超窗 / 无合适车型 / 超出车辆数） */
    private List<RouteStop> unassigned;

    /** 分段时长来源：MAP-地图服务路网时长 ESTIMATE-按平均时速估算 */
    private String durationSource;

    /** 原始地图服务返回的完整JSON */
    private String rawResponse;

    /**
     * 车辆批次（VRP 基础约束）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteBatch {
        /** 批次号，从 1 开始（即第几辆车） */
        private int batchNo;
        /** 本批载量合计 */
        private java.math.BigDecimal load;
        /** 本批距离（米） */
        private Long totalDistance;
        /** 本批时长（秒） */
        private Long totalDuration;
        /** 起点下标（多起点时指明从哪个出发点出发） */
        private Integer originIndex;
        /** 起点地址快照 */
        private String originAddress;
        /** 分配车型名称 */
        private String vehicleName;
        /** 该车型载重上限 */
        private java.math.BigDecimal capacity;
        /** 本批点位（含批内 ETA） */
        private List<RouteStop> stops;
        /** 本批路线坐标（绘图用） */
        private List<RoutePoint> routePoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePoint {
        private double lat;
        private double lng;
        /** 地址描述（降级直线模式下由入参带回，便于前端落图标注） */
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStep {
        private int fromIndex;
        private int toIndex;
        private String instruction;
        private Long distance;
        private Long duration;
        private String roadName;
        private List<RoutePoint> points;
    }

    /**
     * 规划结果点位（前端表格 / 落图 / 一键生成配送路线单点位明细均以此为准）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStop {
        /** 序号，从 1 开始 */
        private int index;
        /** start-起点 waypoint-途经点 destination-终点 */
        private String type;
        private String address;
        private double lat;
        private double lng;
        /** 距上一点距离（米） */
        private Long distanceFromPrev;
        /** 距上一点时长（秒） */
        private Long durationFromPrev;
        /** 原始输入下标（优化排序前的下标） */
        private Integer sourceIndex;
        /** 所属批次（第几辆车，启用约束时有值） */
        private Integer batchNo;
        /** 需求量 */
        private java.math.BigDecimal demand;
        /** 时间窗开始 "HH:mm" */
        private String timeWindowStart;
        /** 时间窗结束 "HH:mm" */
        private String timeWindowEnd;
        /** 预计到达时刻 "HH:mm"（启用约束时有值） */
        private String eta;
    }
}
