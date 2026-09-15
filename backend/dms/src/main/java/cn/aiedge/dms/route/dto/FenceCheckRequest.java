package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 电子围栏校验请求
 *
 * 三种用法（优先级：fenceId > 内联几何）：
 * 1. 按围栏档案：{lat, lng, fenceId}
 * 2. 内联圆形：{lat, lng, centerLat, centerLng, radiusMeters}
 * 3. 内联多边形：{lat, lng, points:"lng,lat;lng,lat;…"}
 *
 * 同时支持多点评位：{points: [{lat,lng}, …], fenceId/内联几何}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FenceCheckRequest {

    /** 待校验点纬度 */
    private Double lat;

    /** 待校验点经度 */
    private Double lng;

    /** 待校验点列表（批量校验时使用，与 lat/lng 二选一） */
    private List<Point> points;

    /** 已保存的围栏 ID */
    private Long fenceId;

    /** 围栏类型：CIRCLE / POLYGON（内联几何时必填） */
    private String fenceType;

    /** 圆心纬度 */
    private Double centerLat;

    /** 圆心经度 */
    private Double centerLng;

    /** 半径（米） */
    private Double radiusMeters;

    /** 多边形顶点："lng,lat;lng,lat;…" */
    private String polygon;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private double lat;
        private double lng;
        private String address;
    }
}
