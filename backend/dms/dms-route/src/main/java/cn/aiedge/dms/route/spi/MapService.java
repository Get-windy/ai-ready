package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.route.dto.*;

import java.util.List;

/**
 * 地图服务统一接口（SPI）
 *
 * 支持高德（优先）、腾讯、百度地图服务。
 * 每个地图服务提供商独立实现此接口，通过配置选择启用。
 *
 * @author AI-Ready Team
 */
public interface MapService {

    /** 地图服务提供商编码 */
    String getProvider();

    /** 驾车路线规划（多点最优） */
    RoutePlanResponse planDrivingRoute(RoutePlanRequest request);

    /** 骑行路线规划 */
    RoutePlanResponse planCyclingRoute(RoutePlanRequest request);

    /** 步行路线规划 */
    RoutePlanResponse planWalkingRoute(RoutePlanRequest request);

    /** 地理编码：地址 → 坐标 */
    GeocodeResponse geocode(GeocodeRequest request);

    /** 逆地理编码：坐标 → 地址 */
    ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request);

    /** 坐标距离计算（批量） */
    DistanceResponse calculateDistance(DistanceRequest request);

    /** 电子围栏：判断坐标是否在围栏内 */
    boolean isWithinFence(double lat, double lng, String fenceParams);
}
