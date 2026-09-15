package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.route.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地离线地图服务（降级兜底）
 *
 * 未配置任何地图服务商 Key、或外部地图服务调用失败时启用：
 * - 路线规划 / 距离计算：按 Haversine 球面直线距离 + 出行方式平均时速估算；
 * - 电子围栏：纯几何判定（支持圆形与多边形）；
 * - 地理编码 / 逆地理编码：需要地图厂商 POI 库，降级为明确失败并给出配置指引，不返回假坐标。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component("localMapService")
public class LocalMapService extends AbstractMapService {

    /** 平均时速（km/h）：城市配送场景经验值 */
    private static final double SPEED_DRIVING = 30d;
    private static final double SPEED_CYCLING = 15d;
    private static final double SPEED_WALKING = 5d;

    public LocalMapService() {
        super("");
    }

    @Override
    public String getProvider() {
        return "local";
    }

    /** 本地模式永远可用 */
    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    protected String doGet(String url) {
        throw new UnsupportedOperationException("本地模式不发起外部请求");
    }

    @Override
    public RoutePlanResponse planDrivingRoute(RoutePlanRequest request) {
        return buildStraightLinePlan(request, "DRIVING", SPEED_DRIVING);
    }

    @Override
    public RoutePlanResponse planCyclingRoute(RoutePlanRequest request) {
        return buildStraightLinePlan(request, "CYCLING", SPEED_CYCLING);
    }

    @Override
    public RoutePlanResponse planWalkingRoute(RoutePlanRequest request) {
        return buildStraightLinePlan(request, "WALKING", SPEED_WALKING);
    }

    @Override
    public GeocodeResponse geocode(GeocodeRequest request) {
        return GeocodeResponse.builder()
                .success(false)
                .provider(getProvider())
                .degraded(true)
                .message("未配置地图服务 Key，无法进行地址编码；请在《配送参数》中配置 amap.api-key 后重试")
                .build();
    }

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        return ReverseGeocodeResponse.builder()
                .success(false)
                .provider(getProvider())
                .degraded(true)
                .lat(request.getLat())
                .lng(request.getLng())
                .message("未配置地图服务 Key，无法进行逆地理编码；请在《配送参数》中配置 amap.api-key 后重试")
                .build();
    }

    @Override
    public DistanceResponse calculateDistance(DistanceRequest request) {
        return buildHaversineDistances(request);
    }

    @Override
    public boolean isWithinFence(double lat, double lng, String fenceParams) {
        return checkCircleFence(lat, lng, fenceParams);
    }
}
