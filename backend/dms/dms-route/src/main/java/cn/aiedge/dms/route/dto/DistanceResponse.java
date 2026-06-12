package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 距离计算响应（批量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceResponse {

    private boolean success;
    private String message;

    private List<DistanceItem> distances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistanceItem {
        private int index;
        /** 距离（米） */
        private Long distance;
        /** 预计时长（秒） */
        private Long duration;
        /** 起点坐标 */
        private double originLat;
        private double originLng;
        /** 终点坐标 */
        private double destLat;
        private double destLng;
    }
}
