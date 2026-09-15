package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 路线规划请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanRequest {

    /** 起点坐标（纬度,经度） */
    private Coordinate origin;

    /** 途径点坐标列表（可选） */
    private List<Coordinate> waypoints;

    /**
     * 目的地坐标列表（与 waypoints 同义）
     *
     * 历史前端使用 destinations 字段，后端只解析 waypoints 导致「目的地被静默忽略」；
     * 现统一由 RoutePlanService 归一化合并两者，任一字段名都可正常规划。
     */
    private List<Coordinate> destinations;

    /** 终点坐标（取货场景可设为仓库） */
    private Coordinate destination;

    /** 导航策略：0-速度优先 1-距离优先 2-避免收费 3-避免拥堵 */
    private Integer strategy;

    /** 出行方式：DRIVING（默认）/CYCLING/WALKING */
    private String direction;

    /** 是否按最近邻算法重排途经点顺序（默认 true，多点规划时生效） */
    private Boolean optimizeOrder;

    /** 坐标体系：WGS84/GCJ02（默认）/BD09，入库前统一转换为 GCJ-02 */
    private String coordSystem;

    /** 已访问点数（重规划时用于点位序号续编，默认 0） */
    private Integer visitedCount;

    // ==================== VRP 基础约束（容量 / 时间窗） ====================

    /** 车辆载重（或载量）上限；填写后按需求量对各点**贪婪分批**（一车一批） */
    private java.math.BigDecimal vehicleCapacity;

    /** 可用车辆数（批次超出时给出告警，不阻断规划） */
    private Integer vehicleCount;

    /** 出发时间 "HH:mm"（默认 08:00），用于逐点 ETA 与时间窗校核 */
    private String departureTime;

    /** 是否启用约束（容量/时间窗）；留空时按是否填写了容量或时间窗自动判定 */
    private Boolean enableConstraints;

    /** 多起点（多仓/多出发点）；留空时用 origin 单起点 */
    private List<Coordinate> origins;

    /** 多车型（容量不同、可用数量有限）；留空时用 vehicleCapacity 构造「默认车辆」 */
    private List<VehicleSpec> vehicles;

    /** 硬时间窗：true 时晚于时间窗的点**移出排程**并进入 unassigned（默认为软约束，仅告警） */
    private Boolean hardTimeWindow;

    /**
     * 车型定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleSpec {
        /** 车型名称（如 4.2 米厢货） */
        private String name;
        /** 单车载重上限 */
        private java.math.BigDecimal capacity;
        /** 可用数量（默认不限） */
        private Integer count;
    }

    /** 车辆类型：0-小车 1-货车 */
    private Integer vehicleType;

    /** 车牌号（货车限行） */
    private String plateNo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinate {
        private double lat;
        private double lng;
        /** 地址描述（可选） */
        private String address;
        /** 此点的停留时间（秒，配送场景） */
        private Integer stayDuration;
        /** 需求量（重量/件数，用于容量约束分批） */
        private java.math.BigDecimal demand;
        /** 时间窗开始 "HH:mm"（可选） */
        private String timeWindowStart;
        /** 时间窗结束 "HH:mm"（可选，决定时间窗优先级与晚到告警） */
        private String timeWindowEnd;
    }
}
