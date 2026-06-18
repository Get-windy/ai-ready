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

    /** 终点坐标（取货场景可设为仓库） */
    private Coordinate destination;

    /** 导航策略：0-速度优先 1-距离优先 2-避免收费 3-避免拥堵 */
    private Integer strategy;

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
    }
}
