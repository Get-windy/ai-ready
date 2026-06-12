package cn.aiedge.dms.route.service;

import cn.aiedge.dms.route.dto.*;
import cn.aiedge.dms.route.spi.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 路线规划服务
 *
 * 提供配送场景下的高级路线规划功能：
 * - 多点最优路线排序（TSP 问题简化版）
 * - 取货/送货多段路线
 * - 电子围栏服务
 * - 坐标解析服务
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    /** 默认使用高德地图服务 */
    private final MapService mapService;

    /**
     * 规划配送路线（起点→各配送点→终点最优顺序）
     *
     * @param origin     起点坐标（仓库/出货点）
     * @param stops      配送点列表（客户坐标）
     * @param strategy   策略
     * @return 优化后的路线
     */
    public RoutePlanResponse planDeliveryRoute(
            RoutePlanRequest.Coordinate origin,
            List<RoutePlanRequest.Coordinate> stops,
            Integer strategy) {

        if (stops == null || stops.isEmpty()) {
            return RoutePlanResponse.builder().success(false).message("配送点列表为空").build();
        }

        // 单点配送，直接规划
        if (stops.size() == 1) {
            RoutePlanRequest request = RoutePlanRequest.builder()
                    .origin(origin)
                    .destination(stops.get(0))
                    .strategy(strategy)
                    .build();
            return mapService.planDrivingRoute(request);
        }

        // 多点配送：使用贪心最近邻算法（TSP 简化版）
        List<Integer> optimizedOrder = greedyNearestNeighbor(origin, stops);

        // 按最优顺序重新排列途径点
        // 第一个配送点作为第一段终点，其余作为途径点
        RoutePlanRequest.Coordinate firstDest = stops.get(optimizedOrder.get(0));

        List<RoutePlanRequest.Coordinate> waypoints = new java.util.ArrayList<>();
        for (int i = 1; i < optimizedOrder.size() - 1; i++) {
            waypoints.add(stops.get(optimizedOrder.get(i)));
        }

        RoutePlanRequest.Coordinate finalDest;
        if (optimizedOrder.size() > 1) {
            finalDest = stops.get(optimizedOrder.get(optimizedOrder.size() - 1));
        } else {
            finalDest = firstDest;
        }

        RoutePlanRequest request = RoutePlanRequest.builder()
                .origin(origin)
                .destination(finalDest)
                .waypoints(waypoints)
                .strategy(strategy)
                .build();

        RoutePlanResponse response = mapService.planDrivingRoute(request);
        response.setOptimizedOrder(optimizedOrder);
        return response;
    }

    /**
     * 地理编码：地址转坐标
     */
    public GeocodeResponse geocode(String address) {
        GeocodeRequest request = GeocodeRequest.builder().address(address).build();
        return mapService.geocode(request);
    }

    /**
     * 逆地理编码：坐标转地址
     */
    public ReverseGeocodeResponse reverseGeocode(double lat, double lng) {
        ReverseGeocodeRequest request = ReverseGeocodeRequest.builder()
                .lat(lat).lng(lng).build();
        return mapService.reverseGeocode(request);
    }

    /**
     * 计算到多个点的距离
     */
    public DistanceResponse calculateDistances(
            RoutePlanRequest.Coordinate origin,
            List<RoutePlanRequest.Coordinate> destinations) {
        DistanceRequest request = DistanceRequest.builder()
                .origin(origin)
                .destinations(destinations)
                .type(2) // 驾车距离
                .build();
        return mapService.calculateDistance(request);
    }

    /**
     * 检查坐标是否在服务围栏内
     */
    public boolean checkFence(double lat, double lng, double centerLat, double centerLng, double radiusMeters) {
        String fenceParams = centerLat + "," + centerLng + "," + radiusMeters;
        return mapService.isWithinFence(lat, lng, fenceParams);
    }

    // ==================== 私有方法 ====================

    /**
     * 贪心最近邻算法（TSP 简化版）
     * 从起点开始，每次选择最近的未访问配送点
     */
    private List<Integer> greedyNearestNeighbor(
            RoutePlanRequest.Coordinate origin,
            List<RoutePlanRequest.Coordinate> stops) {

        int n = stops.size();
        boolean[] visited = new boolean[n];
        List<Integer> order = new java.util.ArrayList<>();

        double currentLat = origin.getLat();
        double currentLng = origin.getLng();

        for (int i = 0; i < n; i++) {
            int nearestIdx = -1;
            double minDist = Double.MAX_VALUE;

            for (int j = 0; j < n; j++) {
                if (visited[j]) continue;
                double dist = haversineDistance(currentLat, currentLng,
                        stops.get(j).getLat(), stops.get(j).getLng());
                if (dist < minDist) {
                    minDist = dist;
                    nearestIdx = j;
                }
            }

            if (nearestIdx >= 0) {
                visited[nearestIdx] = true;
                order.add(nearestIdx);
                currentLat = stops.get(nearestIdx).getLat();
                currentLng = stops.get(nearestIdx).getLng();
            }
        }

        return order;
    }

    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
