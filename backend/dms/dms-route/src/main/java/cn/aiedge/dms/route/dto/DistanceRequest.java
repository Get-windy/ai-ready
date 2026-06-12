package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 距离计算请求（批量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceRequest {

    /** 起点坐标 */
    private RoutePlanRequest.Coordinate origin;

    /** 目标坐标列表 */
    private List<RoutePlanRequest.Coordinate> destinations;

    /** 计算方式：1-直线距离 2-驾车距离 3-骑行距离 */
    private Integer type;
}
