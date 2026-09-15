package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.route.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德地图服务实现（默认首选）
 *
 * 高德开放平台 API 文档：https://lbs.amap.com/api/webservice/guide/api
 * 需在 application.yml 中配置：dms.map.amap.api-key
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component("amapMapService")
@Primary
public class AmapMapService extends AbstractMapService {

    private static final String BASE_URL = "https://restapi.amap.com/v3";
    private static final String DRIVING_URL = BASE_URL + "/direction/driving";
    private static final String CYCLING_URL = BASE_URL + "/direction/cycling";
    private static final String WALKING_URL = BASE_URL + "/direction/walking";
    private static final String GEOCODE_URL = BASE_URL + "/geocode/geo";
    private static final String REGEOCODE_URL = BASE_URL + "/geocode/regeo";
    private static final String DISTANCE_URL = BASE_URL + "/distance";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AmapMapService(
            @Value("${dms.map.amap.api-key:}") String apiKey,
            @Qualifier("dmsRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(apiKey);
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return "amap";
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
                    .append("&origin=").append(request.getOrigin().getLng()).append(",").append(request.getOrigin().getLat())
                    .append("&destination=").append(request.getDestination().getLng()).append(",").append(request.getDestination().getLat())
                    .append("&extensions=base");

            if (request.getStrategy() != null) {
                url.append("&strategy=").append(request.getStrategy());
            }

            // 途径点
            if (request.getWaypoints() != null && !request.getWaypoints().isEmpty()) {
                StringBuilder waypoints = new StringBuilder();
                for (RoutePlanRequest.Coordinate w : request.getWaypoints()) {
                    if (waypoints.length() > 0) waypoints.append(";");
                    waypoints.append(w.getLng()).append(",").append(w.getLat());
                }
                url.append("&waypoints=").append(waypoints);
            }

            String response = doGet(url.toString());
            RoutePlanResponse plan = parseDrivingResponse(response, request);
            plan.setTravelMode("DRIVING");
            return plan;
        } catch (Exception e) {
            log.error("[Amap] 驾车路线规划失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).provider(getProvider())
                    .message("路线规划失败: " + e.getMessage()).build();
        }
    }

    @Override
    public RoutePlanResponse planCyclingRoute(RoutePlanRequest request) {
        try {
            StringBuilder url = new StringBuilder(CYCLING_URL)
                    .append("?key=").append(apiKey)
                    .append("&origin=").append(request.getOrigin().getLng()).append(",").append(request.getOrigin().getLat())
                    .append("&destination=").append(request.getDestination().getLng()).append(",").append(request.getDestination().getLat());

            String response = doGet(url.toString());
            RoutePlanResponse plan = parseDrivingResponse(response, request);
            plan.setTravelMode("CYCLING");
            return plan;
        } catch (Exception e) {
            log.error("[Amap] 骑行路线规划失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).provider(getProvider())
                    .message("骑行路线规划失败: " + e.getMessage()).build();
        }
    }

    @Override
    public RoutePlanResponse planWalkingRoute(RoutePlanRequest request) {
        try {
            StringBuilder url = new StringBuilder(WALKING_URL)
                    .append("?key=").append(apiKey)
                    .append("&origin=").append(request.getOrigin().getLng()).append(",").append(request.getOrigin().getLat())
                    .append("&destination=").append(request.getDestination().getLng()).append(",").append(request.getDestination().getLat());

            String response = doGet(url.toString());
            RoutePlanResponse plan = parseDrivingResponse(response, request);
            plan.setTravelMode("WALKING");
            return plan;
        } catch (Exception e) {
            log.error("[Amap] 步行路线规划失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).provider(getProvider())
                    .message("步行路线规划失败: " + e.getMessage()).build();
        }
    }

    @Override
    public GeocodeResponse geocode(GeocodeRequest request) {
        try {
            StringBuilder url = new StringBuilder(GEOCODE_URL)
                    .append("?key=").append(apiKey)
                    .append("&address=").append(java.net.URLEncoder.encode(request.getAddress(), "UTF-8"));
            if (request.getCity() != null) {
                url.append("&city=").append(java.net.URLEncoder.encode(request.getCity(), "UTF-8"));
            }

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (!"1".equals(root.get("status").asText())) {
                return GeocodeResponse.builder().success(false)
                        .message(root.get("info").asText()).build();
            }

            JsonNode geocodes = root.get("geocodes");
            if (geocodes == null || !geocodes.isArray() || geocodes.isEmpty()) {
                return GeocodeResponse.builder().success(false).provider(getProvider())
                        .message("未找到地址坐标").build();
            }

            // 金标准：返回全部候选（模糊搜索可选），首条同时平铺为单结果字段
            List<GeocodeResponse.GeocodeCandidate> candidates = new ArrayList<>();
            for (JsonNode node : geocodes) {
                String location = getText(node, "location");
                if (location == null || !location.contains(",")) {
                    continue;
                }
                String[] loc = location.split(",");
                candidates.add(GeocodeResponse.GeocodeCandidate.builder()
                        .lng(Double.parseDouble(loc[0]))
                        .lat(Double.parseDouble(loc[1]))
                        .formattedAddress(getText(node, "formatted_address"))
                        .province(getText(node, "province"))
                        .city(getText(node, "city"))
                        .district(getText(node, "district"))
                        .adcode(getText(node, "adcode"))
                        .level(getText(node, "level"))
                        .build());
            }
            if (candidates.isEmpty()) {
                return GeocodeResponse.builder().success(false).provider(getProvider())
                        .message("未找到地址坐标").build();
            }

            GeocodeResponse.GeocodeCandidate first = candidates.get(0);
            return GeocodeResponse.builder()
                    .success(true)
                    .provider(getProvider())
                    .lng(first.getLng())
                    .lat(first.getLat())
                    .formattedAddress(first.getFormattedAddress())
                    .province(first.getProvince())
                    .city(first.getCity())
                    .district(first.getDistrict())
                    .candidates(candidates)
                    .build();
        } catch (Exception e) {
            log.error("[Amap] 地理编码失败: {}", e.getMessage());
            return GeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        try {
            StringBuilder url = new StringBuilder(REGEOCODE_URL)
                    .append("?key=").append(apiKey)
                    .append("&location=").append(request.getLng()).append(",").append(request.getLat())
                    .append("&extensions=base");

            if (request.getPoiRadius() != null) {
                url.append("&radius=").append(request.getPoiRadius());
            }

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (!"1".equals(root.get("status").asText())) {
                return ReverseGeocodeResponse.builder().success(false).provider(getProvider())
                        .message(root.get("info").asText()).build();
            }

            JsonNode regeocode = root.get("regeocode");
            if (regeocode == null) {
                return ReverseGeocodeResponse.builder().success(false).provider(getProvider())
                        .lat(request.getLat()).lng(request.getLng())
                        .message("未找到地址信息").build();
            }

            ReverseGeocodeResponse.ReverseGeocodeResponseBuilder builder = ReverseGeocodeResponse.builder()
                    .success(true)
                    .provider(getProvider())
                    .lat(request.getLat())
                    .lng(request.getLng())
                    .formattedAddress(getText(regeocode, "formatted_address"));

            JsonNode addressComponent = regeocode.get("addressComponent");
            if (addressComponent != null) {
                builder.province(getText(addressComponent, "province"))
                        .city(getText(addressComponent, "city"))
                        .district(getText(addressComponent, "district"))
                        .street(getText(addressComponent, "streetNumber", "street"))
                        .streetNumber(getText(addressComponent, "streetNumber", "number"));
            }

            return builder.build();
        } catch (Exception e) {
            log.error("[Amap] 逆地理编码失败: {}", e.getMessage());
            return ReverseGeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public DistanceResponse calculateDistance(DistanceRequest request) {
        try {
            StringBuilder url = new StringBuilder(DISTANCE_URL)
                    .append("?key=").append(apiKey)
                    .append("&type=").append(request.getType() != null ? request.getType() : 1)
                    .append("&origins=")
                    .append(request.getOrigin().getLng()).append(",").append(request.getOrigin().getLat());

            if (request.getDestinations() != null && !request.getDestinations().isEmpty()) {
                url.append("&destination=");
                StringBuilder destStr = new StringBuilder();
                for (RoutePlanRequest.Coordinate d : request.getDestinations()) {
                    if (destStr.length() > 0) destStr.append(";");
                    destStr.append(d.getLng()).append(",").append(d.getLat());
                }
                url.append(destStr);
            }

            String response = doGet(url.toString());
            JsonNode root = objectMapper.readTree(response);

            if (!"1".equals(root.get("status").asText())) {
                return DistanceResponse.builder().success(false)
                        .message(root.get("info").asText()).build();
            }

            List<DistanceResponse.DistanceItem> items = new ArrayList<>();
            JsonNode results = root.get("results");
            if (results != null && results.isArray()) {
                int idx = 0;
                for (JsonNode r : results) {
                    // 高德按入参顺序返回，回填起终点便于前端核对
                    RoutePlanRequest.Coordinate dest = null;
                    if (request.getDestinations() != null && idx < request.getDestinations().size()) {
                        dest = request.getDestinations().get(idx);
                    }
                    items.add(DistanceResponse.DistanceItem.builder()
                            .index(idx++)
                            .distance(r.has("distance") ? r.get("distance").asLong() : 0)
                            .duration(r.has("duration") ? r.get("duration").asLong() : 0)
                            .originLat(request.getOrigin().getLat())
                            .originLng(request.getOrigin().getLng())
                            .destLat(dest != null ? dest.getLat() : 0d)
                            .destLng(dest != null ? dest.getLng() : 0d)
                            .build());
                }
            }

            return DistanceResponse.builder().success(true).provider(getProvider()).distances(items).build();
        } catch (Exception e) {
            log.error("[Amap] 距离计算失败: {}", e.getMessage());
            return DistanceResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public boolean isWithinFence(double lat, double lng, String fenceParams) {
        // 电子围栏：fenceParams 格式为 "centerLat,centerLng,radius(m)"
        return checkCircleFence(lat, lng, fenceParams);
    }

    // ==================== 私有方法 ====================

    /**
     * 解析驾车路线响应
     */
    private RoutePlanResponse parseDrivingResponse(String response, RoutePlanRequest request) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.get("status").asText())) {
                return RoutePlanResponse.builder().success(false).provider(getProvider())
                        .message(root.get("info").asText()).build();
            }

            JsonNode route = root.get("route");
            if (route == null || !route.has("paths") || !route.get("paths").isArray()
                    || route.get("paths").isEmpty()) {
                return RoutePlanResponse.builder().success(false).provider(getProvider())
                        .message("未找到路线").build();
            }

            JsonNode path = route.get("paths").get(0);

            RoutePlanResponse.RoutePlanResponseBuilder builder = RoutePlanResponse.builder()
                    .success(true)
                    .provider(getProvider())
                    .totalDistance(path.has("distance") ? path.get("distance").asLong() : 0)
                    .totalDuration(path.has("duration") ? path.get("duration").asLong() : 0)
                    .totalToll(path.has("tolls") ? path.get("tolls").asDouble() : 0)
                    .rawResponse(response);

            // 解析步骤
            List<RoutePlanResponse.RouteStep> steps = new ArrayList<>();
            List<RoutePlanResponse.RoutePoint> allPoints = new ArrayList<>();

            if (path.has("steps") && path.get("steps").isArray()) {
                for (JsonNode stepNode : path.get("steps")) {
                    RoutePlanResponse.RouteStep.RouteStepBuilder stepBuilder = RoutePlanResponse.RouteStep.builder()
                            .instruction(getText(stepNode, "instruction"))
                            .distance(stepNode.has("distance") ? stepNode.get("distance").asLong() : 0)
                            .duration(stepNode.has("duration") ? stepNode.get("duration").asLong() : 0)
                            .roadName(getText(stepNode, "road_name"));

                    // 解析轨迹点
                    List<RoutePlanResponse.RoutePoint> stepPoints = new ArrayList<>();
                    if (stepNode.has("polyline")) {
                        String[] points = stepNode.get("polyline").asText().split(";");
                        for (String p : points) {
                            String[] coord = p.split(",");
                            if (coord.length == 2) {
                                RoutePlanResponse.RoutePoint rp = RoutePlanResponse.RoutePoint.builder()
                                        .lng(Double.parseDouble(coord[0]))
                                        .lat(Double.parseDouble(coord[1]))
                                        .build();
                                stepPoints.add(rp);
                                allPoints.add(rp);
                            }
                        }
                    }
                    stepBuilder.points(stepPoints);

                    RoutePlanResponse.RouteStep step = stepBuilder.build();
                    step.setFromIndex(steps.size());
                    step.setToIndex(steps.size() + 1);
                    steps.add(step);
                }
            }

            builder.steps(steps);
            builder.routePoints(allPoints);

            return builder.build();
        } catch (Exception e) {
            log.error("[Amap] 解析路线响应失败: {}", e.getMessage());
            return RoutePlanResponse.builder().success(false).provider(getProvider())
                    .message("解析路线失败: " + e.getMessage()).build();
        }
    }

    /**
     * 安全获取 JSON 节点文本
     */
    private String getText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String getText(JsonNode node, String parent, String child) {
        if (node.has(parent)) {
            return getText(node.get(parent), child);
        }
        return null;
    }
}
