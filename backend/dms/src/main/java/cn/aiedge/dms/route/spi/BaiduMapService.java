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

/**
 * 百度地图服务实现
 *
 * 百度地图 Web API 文档：https://lbsyun.baidu.com/index.php?title=webapi
 * 需在 application.yml 中配置：dms.map.baidu.api-key
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component("baiduMapService")
@ConditionalOnProperty(name = "dms.map.baidu.api-key")
public class BaiduMapService extends AbstractMapService {

    private static final String BASE_URL = "https://api.map.baidu.com";
    private static final String DIRECTION_URL = BASE_URL + "/directionlite/v1/driving";
    private static final String GEOCODE_URL = BASE_URL + "/geocoding/v3";
    private static final String REGEOCODE_URL = BASE_URL + "/reverse_geocoding/v3";
    private static final String DISTANCE_URL = BASE_URL + "/routematrix/v2/driving";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BaiduMapService(
            @Value("${dms.map.baidu.api-key:}") String apiKey,
            @Qualifier("dmsRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(apiKey);
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return "baidu";
    }

    @Override
    protected String doGet(String url) {
        return restTemplate.getForObject(URI.create(url), String.class);
    }

    @Override
    public RoutePlanResponse planDrivingRoute(RoutePlanRequest request) {
        // 百度行驶路线规划
        try {
            StringBuilder url = new StringBuilder(DIRECTION_URL)
                    .append("?ak=").append(apiKey)
                    .append("&origin=").append(request.getOrigin().getLat()).append(",").append(request.getOrigin().getLng())
                    .append("&destination=").append(request.getDestination().getLat()).append(",").append(request.getDestination().getLng());

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (root.get("status").asInt() != 0) {
                return RoutePlanResponse.builder().success(false)
                        .message("百度路线规划失败: " + root.get("message").asText()).build();
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
            log.error("[Baidu] 驾车路线规划失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).message("路线规划失败: " + e.getMessage()).build();
        }
    }

    @Override
    public RoutePlanResponse planCyclingRoute(RoutePlanRequest request) {
        return RoutePlanResponse.builder().success(false).message("百度地图骑行路线待实现").build();
    }

    @Override
    public RoutePlanResponse planWalkingRoute(RoutePlanRequest request) {
        return RoutePlanResponse.builder().success(false).message("百度地图步行路线待实现").build();
    }

    @Override
    public GeocodeResponse geocode(GeocodeRequest request) {
        try {
            StringBuilder url = new StringBuilder(GEOCODE_URL)
                    .append("?ak=").append(apiKey)
                    .append("&address=").append(java.net.URLEncoder.encode(request.getAddress(), "UTF-8"))
                    .append("&output=json");

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (root.get("status").asInt() != 0) {
                return GeocodeResponse.builder().success(false)
                        .message("百度地理编码失败: " + root.get("message").asText()).build();
            }

            JsonNode result = root.get("result");
            JsonNode location = result.get("location");

            return GeocodeResponse.builder()
                    .success(true)
                    .lat(location.get("lat").asDouble())
                    .lng(location.get("lng").asDouble())
                    .formattedAddress(result.has("formatted_address") ? result.get("formatted_address").asText() : null)
                    .build();
        } catch (Exception e) {
            log.error("[Baidu] 地理编码失败: {}", e.getMessage());
            return GeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        try {
            StringBuilder url = new StringBuilder(REGEOCODE_URL)
                    .append("?ak=").append(apiKey)
                    .append("&location=").append(request.getLat()).append(",").append(request.getLng())
                    .append("&output=json&extensions_poi=").append(request.getPoiRadius() != null ? "1" : "0");

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (root.get("status").asInt() != 0) {
                return ReverseGeocodeResponse.builder().success(false)
                        .message("百度逆地理编码失败").build();
            }

            JsonNode result = root.get("result");
            JsonNode addressComponent = result.get("addressComponent");

            return ReverseGeocodeResponse.builder()
                    .success(true)
                    .formattedAddress(result.has("formatted_address") ? result.get("formatted_address").asText() : null)
                    .province(getText(addressComponent, "province"))
                    .city(getText(addressComponent, "city"))
                    .district(getText(addressComponent, "district"))
                    .build();
        } catch (Exception e) {
            log.error("[Baidu] 逆地理编码失败: {}", e.getMessage());
            return ReverseGeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public DistanceResponse calculateDistance(DistanceRequest request) {
        try {
            StringBuilder url = new StringBuilder(DISTANCE_URL)
                    .append("?ak=").append(apiKey)
                    .append("&origins=").append(request.getOrigin().getLat()).append(",").append(request.getOrigin().getLng());

            if (request.getDestinations() != null && !request.getDestinations().isEmpty()) {
                url.append("&destinations=");
                StringBuilder destStr = new StringBuilder();
                for (RoutePlanRequest.Coordinate d : request.getDestinations()) {
                    if (destStr.length() > 0) destStr.append("|");
                    destStr.append(d.getLat()).append(",").append(d.getLng());
                }
                url.append(destStr);
            }

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (root.get("status").asInt() != 0) {
                return DistanceResponse.builder().success(false).message("距离计算失败").build();
            }

            return DistanceResponse.builder().success(true).build();
        } catch (Exception e) {
            log.error("[Baidu] 距离计算失败: {}", e.getMessage());
            return DistanceResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public boolean isWithinFence(double lat, double lng, String fenceParams) {
        return checkCircleFence(lat, lng, fenceParams);
    }

    private String getText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
