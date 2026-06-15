package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.route.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯地图服务实现
 *
 * 腾讯位置服务 API 文档：https://lbs.qq.com/service/webService/webServiceGuide/webServiceRoute
 * 需在 application.yml 中配置：dms.map.tencent.api-key
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component("tencentMapService")
@ConditionalOnProperty(name = "dms.map.tencent.api-key")
public class TencentMapService extends AbstractMapService {

    private static final String BASE_URL = "https://apis.map.qq.com";
    private static final String DRIVING_URL = BASE_URL + "/ws/direction/v1/driving";
    private static final String GEOCODE_URL = BASE_URL + "/ws/geocoder/v1";
    private static final String DISTANCE_URL = BASE_URL + "/ws/distance/v1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TencentMapService(
            @Value("${dms.map.tencent.api-key:}") String apiKey,
            @Qualifier("dmsRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(apiKey);
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return "tencent";
    }

    @Override
    protected String doGet(String url) {
        return restTemplate.getForObject(URI.create(url), String.class);
    }

    @Override
    public RoutePlanResponse planDrivingRoute(RoutePlanRequest request) {
        try {
            StringBuilder url = new StringBuilder(DRIVING_URL)
                    .append("?key=").append(apiKey)
                    .append("&from=").append(request.getOrigin().getLat()).append(",").append(request.getOrigin().getLng())
                    .append("&to=").append(request.getDestination().getLat()).append(",").append(request.getDestination().getLng());

            if (request.getWaypoints() != null && !request.getWaypoints().isEmpty()) {
                StringBuilder wp = new StringBuilder();
                for (RoutePlanRequest.Coordinate w : request.getWaypoints()) {
                    if (wp.length() > 0) wp.append(";");
                    wp.append(w.getLat()).append(",").append(w.getLng());
                }
                url.append("&waypoints=").append(wp);
            }

            String response = doGet(url.toString());
            return parseResponse(response);
        } catch (Exception e) {
            log.error("[Tencent] 驾车路线规划失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).message("路线规划失败: " + e.getMessage()).build();
        }
    }

    @Override
    public RoutePlanResponse planCyclingRoute(RoutePlanRequest request) {
        return RoutePlanResponse.builder().success(false).message("腾讯地图暂不支持骑行路线规划").build();
    }

    @Override
    public RoutePlanResponse planWalkingRoute(RoutePlanRequest request) {
        return RoutePlanResponse.builder().success(false).message("腾讯地图暂不支持步行路线规划").build();
    }

    @Override
    public GeocodeResponse geocode(GeocodeRequest request) {
        try {
            StringBuilder url = new StringBuilder(GEOCODE_URL)
                    .append("?key=").append(apiKey)
                    .append("&address=").append(java.net.URLEncoder.encode(request.getAddress(), "UTF-8"));

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (root.get("status").asInt() != 0) {
                return GeocodeResponse.builder().success(false)
                        .message(root.has("message") ? root.get("message").asText() : "地理编码失败").build();
            }

            JsonNode result = root.get("result");
            JsonNode location = result.get("location");

            return GeocodeResponse.builder()
                    .success(true)
                    .lat(location.get("lat").asDouble())
                    .lng(location.get("lng").asDouble())
                    .formattedAddress(result.has("address") ? result.get("address").asText() : null)
                    .build();
        } catch (Exception e) {
            log.error("[Tencent] 地理编码失败: {}", e.getMessage());
            return GeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        return ReverseGeocodeResponse.builder().success(false).message("腾讯地图逆地理编码待实现").build();
    }

    @Override
    public DistanceResponse calculateDistance(DistanceRequest request) {
        try {
            StringBuilder url = new StringBuilder(DISTANCE_URL)
                    .append("?key=").append(apiKey)
                    .append("&from=").append(request.getOrigin().getLat()).append(",").append(request.getOrigin().getLng())
                    .append("&mode=driving");

            if (request.getDestinations() != null && !request.getDestinations().isEmpty()) {
                url.append("&to=");
                StringBuilder toStr = new StringBuilder();
                for (RoutePlanRequest.Coordinate d : request.getDestinations()) {
                    if (toStr.length() > 0) toStr.append(";");
                    toStr.append(d.getLat()).append(",").append(d.getLng());
                }
                url.append(toStr);
            }

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (root.get("status").asInt() != 0) {
                return DistanceResponse.builder().success(false).message("距离计算失败").build();
            }

            List<DistanceResponse.DistanceItem> items = new ArrayList<>();
            JsonNode result = root.get("result");
            JsonNode elements = result.get("elements");
            if (elements != null && elements.isArray()) {
                int idx = 0;
                for (JsonNode e : elements) {
                    items.add(DistanceResponse.DistanceItem.builder()
                            .index(idx++)
                            .distance(e.has("distance") ? e.get("distance").asLong() : 0)
                            .duration(e.has("duration") ? e.get("duration").asLong() : 0)
                            .build());
                }
            }

            return DistanceResponse.builder().success(true).distances(items).build();
        } catch (Exception e) {
            log.error("[Tencent] 距离计算失败: {}", e.getMessage());
            return DistanceResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public boolean isWithinFence(double lat, double lng, String fenceParams) {
        // 与高德逻辑相同，复用 Haversine 计算
        try {
            String[] parts = fenceParams.split(",");
            double centerLat = Double.parseDouble(parts[0]);
            double centerLng = Double.parseDouble(parts[1]);
            double radius = Double.parseDouble(parts[2]);
            double distance = haversineDistance(lat, lng, centerLat, centerLng);
            return distance <= radius;
        } catch (Exception e) {
            log.warn("[Tencent] 围栏校验失败: {}", e.getMessage());
            return false;
        }
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

    private RoutePlanResponse parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.get("status").asInt() != 0) {
                return RoutePlanResponse.builder().success(false)
                        .message(root.has("message") ? root.get("message").asText() : "规划失败").build();
            }

            JsonNode result = root.get("result");
            JsonNode routes = result.get("routes");
            if (routes == null || !routes.isArray() || routes.isEmpty()) {
                return RoutePlanResponse.builder().success(false).message("未找到路线").build();
            }

            JsonNode firstRoute = routes.get(0);

            return RoutePlanResponse.builder()
                    .success(true)
                    .totalDistance(firstRoute.has("distance") ? firstRoute.get("distance").asLong() : 0)
                    .totalDuration(firstRoute.has("duration") ? firstRoute.get("duration").asLong() : 0)
                    .rawResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("[Tencent] 解析路线响应失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).message("解析失败: " + e.getMessage()).build();
        }
    }
}
