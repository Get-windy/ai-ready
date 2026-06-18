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

    /** 错误信息 */
    private String message;

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

    /** 排序后的最优访问顺序（各点index） */
    private List<Integer> optimizedOrder;

    /** 原始地图服务返回的完整JSON */
    private String rawResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePoint {
        private double lat;
        private double lng;
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
}
