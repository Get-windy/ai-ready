package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.common.util.GeoUtils;
import cn.aiedge.dms.route.dto.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图服务抽象基类
 * 提供公共方法，子类只需实现特定地图服务的 API 调用逻辑。
 */
@Slf4j
public abstract class AbstractMapService implements MapService {

    /** 当前生效的 Key（支持热更新：来源可为环境变量 / 配置文件 / 配置中心） */
    protected volatile String apiKey;

    protected AbstractMapService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public void applyApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    /**
     * 执行 HTTP GET 请求并返回字符串响应
     */
    protected abstract String doGet(String url);

    // ==================== 降级 / 离线公共实现 ====================

    /**
     * 直线模式路线（未配置地图 Key、或地图服务调用失败时的降级结果）
     *
     * 按「起点 → 各途径点 → 终点」顺序连线，距离用 Haversine 累加，
     * 时长按出行方式平均时速估算；配置 Key 后自动回到真实路网结果。
     *
     * @param speedKmh 平均时速（驾车 30 / 骑行 15 / 步行 5）
     */
    protected RoutePlanResponse buildStraightLinePlan(RoutePlanRequest request, String travelMode, double speedKmh) {
        List<RoutePlanRequest.Coordinate> path = new ArrayList<>();
        if (request.getOrigin() != null) {
            path.add(request.getOrigin());
        }
        if (request.getWaypoints() != null) {
            path.addAll(request.getWaypoints());
        }
        if (request.getDestination() != null) {
            path.add(request.getDestination());
        }
        if (path.size() < 2) {
            return RoutePlanResponse.builder()
                    .success(false).degraded(true).provider(getProvider())
                    .message("起点与终点至少需要两个坐标点").build();
        }

        List<RoutePlanResponse.RoutePoint> routePoints = new ArrayList<>();
        List<RoutePlanResponse.RouteStep> steps = new ArrayList<>();
        long totalDistance = 0L;
        long totalDuration = 0L;

        for (int i = 0; i < path.size(); i++) {
            RoutePlanRequest.Coordinate c = path.get(i);
            routePoints.add(RoutePlanResponse.RoutePoint.builder()
                    .lat(c.getLat()).lng(c.getLng()).address(c.getAddress()).build());
            if (i == 0) {
                continue;
            }
            RoutePlanRequest.Coordinate prev = path.get(i - 1);
            long meters = Math.round(GeoUtils.distanceMeters(prev.getLat(), prev.getLng(), c.getLat(), c.getLng()));
            long seconds = speedKmh <= 0 ? 0L : Math.round(meters / (speedKmh * 1000d / 3600d));
            totalDistance += meters;
            totalDuration += seconds;
            steps.add(RoutePlanResponse.RouteStep.builder()
                    .fromIndex(i - 1).toIndex(i)
                    .instruction("直线前往第 " + (i + 1) + " 个点")
                    .distance(meters).duration(seconds)
                    .points(List.of(
                            RoutePlanResponse.RoutePoint.builder().lat(prev.getLat()).lng(prev.getLng()).build(),
                            RoutePlanResponse.RoutePoint.builder().lat(c.getLat()).lng(c.getLng()).build()))
                    .build());
        }

        return RoutePlanResponse.builder()
                .success(true)
                .provider(getProvider())
                .degraded(true)
                .travelMode(travelMode)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .routePoints(routePoints)
                .steps(steps)
                .message("未配置地图服务 Key，已按直线距离降级估算")
                .build();
    }

    /**
     * 球面距离批量计算（type=1 直线；type=2 驾车、type=3 骑行在降级时同样按球面距离 + 平均时速估算）
     */
    protected DistanceResponse buildHaversineDistances(DistanceRequest request) {
        if (request.getOrigin() == null || request.getDestinations() == null || request.getDestinations().isEmpty()) {
            return DistanceResponse.builder().success(false).degraded(true).provider(getProvider())
                    .message("起点或目标点为空").build();
        }
        List<DistanceResponse.DistanceItem> items = new ArrayList<>();
        int index = 0;
        double speedKmh = switch (request.getType() == null ? 1 : request.getType()) {
            case 2 -> 30d;
            case 3 -> 15d;
            default -> 0d;
        };
        for (RoutePlanRequest.Coordinate dest : request.getDestinations()) {
            long meters = Math.round(GeoUtils.distanceMeters(
                    request.getOrigin().getLat(), request.getOrigin().getLng(), dest.getLat(), dest.getLng()));
            items.add(DistanceResponse.DistanceItem.builder()
                    .index(index++)
                    .distance(meters)
                    .duration(speedKmh <= 0 ? null : Math.round(meters / (speedKmh * 1000d / 3600d)))
                    .originLat(request.getOrigin().getLat())
                    .originLng(request.getOrigin().getLng())
                    .destLat(dest.getLat())
                    .destLng(dest.getLng())
                    .build());
        }
        return DistanceResponse.builder().success(true).degraded(true).provider(getProvider())
                .distances(items).build();
    }

    /**
     * 圆形围栏判定（"centerLat,centerLng,radiusMeters"）
     */
    protected boolean checkCircleFence(double lat, double lng, String fenceParams) {
        try {
            String[] parts = fenceParams.split(",");
            return GeoUtils.inCircle(lat, lng,
                    Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        } catch (Exception e) {
            log.warn("[{}] 围栏校验失败: {}", getProvider(), e.getMessage());
            return false;
        }
    }

    /**
     * 安全的地理编码（异常时返回空结果，不影响主流程）
     */
    protected GeocodeResponse safeGeocode(GeocodeRequest request) {
        try {
            return geocode(request);
        } catch (Exception e) {
            log.warn("[{}] 地理编码失败: address={}, error={}", getProvider(), request.getAddress(), e.getMessage());
            return GeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    /**
     * 安全的逆地理编码
     */
    protected ReverseGeocodeResponse safeReverseGeocode(ReverseGeocodeRequest request) {
        try {
            return reverseGeocode(request);
        } catch (Exception e) {
            log.warn("[{}] 逆地理编码失败: lat={},lng={}, error={}",
                    getProvider(), request.getLat(), request.getLng(), e.getMessage());
            return ReverseGeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }
}
