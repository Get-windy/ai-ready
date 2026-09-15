package cn.aiedge.dms.route.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.util.GeoUtils;
import cn.aiedge.dms.route.dto.*;
import cn.aiedge.dms.route.spi.LocalMapService;
import cn.aiedge.dms.route.spi.MapService;
import cn.aiedge.dms.route.spi.MapServiceRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 路线规划 / 地理编码服务
 *
 * 对外提供《路线规划》页的四项地理能力：
 * 1. 路径规划（多途经点 + 最近邻排序 + 驾车/骑行/步行）；
 * 2. 地址编码（模糊候选列表 + 结果缓存）；
 * 3. 逆编码（结构化地址）；
 * 4. 电子围栏校验（委托 {@link GeoFenceService}，圆形/多边形单一口径）。
 *
 * 降级策略：所选地图服务商未配置 Key 或调用失败时，自动切换为
 * {@link LocalMapService} 的直线模式（Haversine 距离 + 平均时速估算）并在响应标记 degraded，
 * 保证配送规划链路不因外部服务不可用而整体中断。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final MapServiceRouter mapServiceRouter;
    private final MapKeyResolver mapKeyResolver;
    private final GeoFenceService geoFenceService;
    private final GeocodeCache geocodeCache;
    private final LocalMapService localMapService;
    private final RouteVrpPlanner vrpPlanner;

    /** 连通性自检样本地址（稳定存在的杭州地址，仅用于验证 Key 可用性） */
    private static final String VERIFY_SAMPLE_ADDRESS = "浙江省杭州市西湖区文三路";

    // ==================== 路径规划 ====================

    /**
     * 规划配送路线（起点 → 各配送点，支持最近邻重排）
     */
    public RoutePlanResponse planDeliveryRoute(RoutePlanRequest request) {
        if (request == null || request.getOrigin() == null) {
            throw BusinessException.badRequest("起点坐标不能为空");
        }
        RoutePlanRequest req = normalize(request);
        if (!GeoUtils.validCoord(req.getOrigin().getLat(), req.getOrigin().getLng())) {
            throw BusinessException.badRequest("起点坐标不合法");
        }

        List<RoutePlanRequest.Coordinate> stops = collectStops(req);
        if (stops.isEmpty()) {
            return RoutePlanResponse.builder()
                    .success(false)
                    .provider(mapServiceRouter.current().getProvider())
                    .coordSystem(GeoUtils.COORD_GCJ02)
                    .message("配送点列表为空")
                    .build();
        }

        // VRP 基础约束（容量 / 时间窗）：命中即走分批路径
        if (constraintsEnabled(req, stops)) {
            return planWithConstraints(req, stops);
        }

        // 最近邻排序（TSP 简化），默认开启
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            order.add(i);
        }
        boolean optimize = req.getOptimizeOrder() == null || req.getOptimizeOrder();
        if (optimize && stops.size() > 1) {
            order = greedyNearestNeighbor(req.getOrigin(), stops);
        }

        List<RoutePlanRequest.Coordinate> ordered = order.stream().map(stops::get).toList();
        RoutePlanRequest planRequest = RoutePlanRequest.builder()
                .origin(req.getOrigin())
                .waypoints(ordered.size() > 1 ? ordered.subList(0, ordered.size() - 1) : null)
                .destination(ordered.get(ordered.size() - 1))
                .strategy(req.getStrategy())
                .direction(req.getDirection())
                .vehicleType(req.getVehicleType())
                .plateNo(req.getPlateNo())
                .build();

        MapService service = mapServiceRouter.current();
        RoutePlanResponse response = invoke(service, planRequest, req.getDirection());

        // 外部服务失败 → 直线降级（不阻断业务）
        if (!response.isSuccess() && !"local".equals(service.getProvider())) {
            String reason = response.getMessage();
            log.warn("[RouteService] {} 规划失败，降级直线模式: {}", service.getProvider(), reason);
            response = invoke(localMapService, planRequest, req.getDirection());
            response.setMessage((reason == null ? "" : reason + "；") + "已降级为直线距离估算");
        }

        response.setOptimizedOrder(order);
        response.setCoordSystem(GeoUtils.COORD_GCJ02);
        response.setStops(buildStops(req, ordered, order, response, req.getVisitedCount()));
        return response;
    }

    /**
     * 重新规划：新增点位 / 路况变化时，从当前位置对剩余点位重新排序
     * （语义同规划，额外通过 visitedCount 续编点位序号）
     */
    public RoutePlanResponse reoptimize(RoutePlanRequest request) {
        if (request.getVisitedCount() == null) {
            request.setVisitedCount(0);
        }
        RoutePlanResponse response = planDeliveryRoute(request);
        if (response.isSuccess()) {
            response.setMessage("已按当前位置重新规划");
        }
        return response;
    }

    // ==================== 地理编码 ====================

    /**
     * 地址 → 坐标（带缓存，返回候选列表）
     */
    public GeocodeResponse geocode(String address, String city) {
        if (address == null || address.isBlank()) {
            throw BusinessException.badRequest("地址不能为空");
        }
        GeocodeResponse cached = geocodeCache.get(address, city);
        if (cached != null) {
            return cached;
        }
        MapService service = mapServiceRouter.current();
        GeocodeResponse response = service.geocode(GeocodeRequest.builder().address(address.trim()).city(city).build());
        if (response.getProvider() == null) {
            response.setProvider(service.getProvider());
        }
        response.setDegraded("local".equals(service.getProvider()));
        geocodeCache.put(address, city, response);
        return response;
    }

    /**
     * 坐标 → 地址（结构化）
     *
     * @param from 入参坐标体系，缺省 GCJ-02（地图服务统一口径）；传 WGS84 时自动转换
     */
    public ReverseGeocodeResponse reverseGeocode(double lat, double lng, String from) {
        if (!GeoUtils.validCoord(lat, lng)) {
            throw BusinessException.badRequest("坐标不合法");
        }
        double[] gcj = GeoUtils.toGcj02(lat, lng, from);
        MapService service = mapServiceRouter.current();
        ReverseGeocodeResponse response = service.reverseGeocode(ReverseGeocodeRequest.builder()
                .lat(gcj[0]).lng(gcj[1]).build());
        if (response.getProvider() == null) {
            response.setProvider(service.getProvider());
        }
        response.setDegraded("local".equals(service.getProvider()));
        response.setLat(gcj[0]);
        response.setLng(gcj[1]);
        return response;
    }

    // ==================== 距离 ====================

    /**
     * 批量距离/时长计算（type：1 直线 2 驾车 3 骑行）
     */
    public DistanceResponse calculateDistances(DistanceRequest request) {
        if (request == null || request.getOrigin() == null
                || request.getDestinations() == null || request.getDestinations().isEmpty()) {
            throw BusinessException.badRequest("起点与目标点不能为空");
        }
        MapService service = mapServiceRouter.current();
        DistanceResponse response = service.calculateDistance(request);
        if (!response.isSuccess() && !"local".equals(service.getProvider())) {
            response = localMapService.calculateDistance(request);
        }
        if (response.getProvider() == null) {
            response.setProvider(service.getProvider());
        }
        return response;
    }

    // ==================== 围栏 ====================

    /**
     * 电子围栏校验（圆形 / 多边形，单点 / 批量）
     */
    public FenceCheckResponse fenceCheck(FenceCheckRequest request) {
        return geoFenceService.check(request);
    }

    // ==================== 坐标转换 ====================

    /**
     * 坐标体系转换（WGS84 / GCJ02 / BD09 互转）
     */
    public CoordConvertResponse convert(CoordConvertRequest request) {
        if (request == null) {
            throw BusinessException.badRequest("转换参数不能为空");
        }
        String from = request.getFrom() == null ? GeoUtils.COORD_WGS84 : request.getFrom().trim().toUpperCase();
        String to = request.getTo() == null ? GeoUtils.COORD_GCJ02 : request.getTo().trim().toUpperCase();
        if (!List.of(GeoUtils.COORD_WGS84, GeoUtils.COORD_GCJ02, GeoUtils.COORD_BD09).contains(from)
                || !List.of(GeoUtils.COORD_WGS84, GeoUtils.COORD_GCJ02, GeoUtils.COORD_BD09).contains(to)) {
            throw BusinessException.badRequest("坐标体系仅支持 WGS84 / GCJ02 / BD09");
        }

        if (request.getPoints() != null && !request.getPoints().isEmpty()) {
            List<CoordConvertResponse.Item> items = new ArrayList<>();
            for (FenceCheckRequest.Point p : request.getPoints()) {
                double[] out = convert(p.getLat(), p.getLng(), from, to);
                items.add(CoordConvertResponse.Item.builder()
                        .lat(out[0]).lng(out[1]).address(p.getAddress()).build());
            }
            return CoordConvertResponse.builder().from(from).to(to).results(items).build();
        }

        if (request.getLat() == null || request.getLng() == null) {
            throw BusinessException.badRequest("缺少待转换坐标");
        }
        double[] out = convert(request.getLat(), request.getLng(), from, to);
        return CoordConvertResponse.builder().from(from).to(to).lat(out[0]).lng(out[1]).build();
    }

    // ==================== 配置状态 ====================

    /**
     * 地理能力配置状态（前端展示「Key 配在哪 / 是否降级」的诊断信息，不回显 Key 明文）
     */
    public RouteConfigResponse config() {
        MapService current = mapServiceRouter.current();
        boolean degraded = mapServiceRouter.degraded();
        String provider = current == null ? "local" : current.getProvider();
        // 降级时对外提示「默认服务商」的 Key 落位（local 无 Key 概念）
        String targetProvider = degraded ? mapKeyResolver.defaultProvider() : provider;
        MapKeyResolver.ProviderKey providerKey = mapKeyResolver.key(targetProvider);
        return RouteConfigResponse.builder()
                .provider(provider)
                .defaultProvider(mapKeyResolver.defaultProvider())
                .configured(!degraded)
                .degraded(degraded)
                .keySource(providerKey.source().name())
                .envVarName(mapKeyResolver.envVarName(targetProvider))
                .configKey(mapKeyResolver.configKeyName(targetProvider))
                .hint(buildConfigHint(targetProvider, providerKey))
                .jsKey(mapKeyResolver.jsKey())
                .jsKeyConfigured(StringUtils.hasText(mapKeyResolver.jsKey()))
                .providers(mapServiceRouter.providerStatus())
                .coordSystem(GeoUtils.COORD_GCJ02)
                .travelModes(List.of("DRIVING", "CYCLING", "WALKING"))
                .geocodeCacheSize(geocodeCache.size())
                .build();
    }

    /**
     * 连通性自检：用当前生效 Key 真实调用一次地理编码（**绕过缓存**），返回结论与耗时
     */
    public RouteVerifyResponse verify() {
        MapService service = mapServiceRouter.current();
        String provider = service == null ? "local" : service.getProvider();
        boolean degraded = mapServiceRouter.degraded();
        String targetProvider = degraded ? mapKeyResolver.defaultProvider() : provider;
        MapKeyResolver.ProviderKey providerKey = mapKeyResolver.key(targetProvider);

        RouteVerifyResponse.RouteVerifyResponseBuilder builder = RouteVerifyResponse.builder()
                .provider(provider)
                .keySource(providerKey.source().name())
                .envVarName(mapKeyResolver.envVarName(targetProvider))
                .configKey(mapKeyResolver.configKeyName(targetProvider))
                .degraded(degraded)
                .sampleAddress(VERIFY_SAMPLE_ADDRESS);

        if (degraded || service == null) {
            return builder.ok(false)
                    .message("当前为降级模式（未配置地图服务 Key），地理编码/逆编码不可用；"
                            + "请按《配送参数》" + mapKeyResolver.configKeyName(targetProvider)
                            + "或环境变量 " + mapKeyResolver.envVarName(targetProvider) + " 配置后重试")
                    .build();
        }

        long start = System.currentTimeMillis();
        GeocodeResponse response;
        try {
            response = service.geocode(GeocodeRequest.builder().address(VERIFY_SAMPLE_ADDRESS).build());
        } catch (Exception e) {
            log.warn("[RouteService] 连通性自检异常: {}", e.getMessage());
            response = GeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
        long latency = System.currentTimeMillis() - start;

        boolean ok = response != null && response.isSuccess() && response.getLat() != null;
        return builder.ok(ok)
                .lat(response == null ? null : response.getLat())
                .lng(response == null ? null : response.getLng())
                .formattedAddress(response == null ? null : response.getFormattedAddress())
                .latencyMs(latency)
                .message(ok
                        ? "地图服务连通正常（" + provider + "，耗时 " + latency + " ms）"
                        : "地图服务调用失败："
                        + (response == null || response.getMessage() == null ? "未知原因" : response.getMessage())
                        + "；请核对本次填写的 Key 是否有效、是否开通了 Web 服务（" + provider + "）")
                .build();
    }

    /** 按当前来源生成「下一步该去哪配」的指引 */
    private String buildConfigHint(String providerName, MapKeyResolver.ProviderKey providerKey) {
        String envVar = mapKeyResolver.envVarName(providerName);
        String configKey = mapKeyResolver.configKeyName(providerName);
        return switch (providerKey.source()) {
            case ENV -> "地图服务 Key 来源：环境变量 " + envVar + "（生产推荐，修改后需重启后端）";
            case SPRING -> "地图服务 Key 来源：Spring 配置（application.yml / 外部化配置文件 / 启动参数 --dms.map."
                    + providerName + ".api-key）";
            case CONFIG -> "地图服务 Key 来源：《配送参数》配置中心 " + configKey + "（页面保存后热生效）";
            case NONE -> "未配置地图服务 Key，路线规划/距离计算按直线距离降级估算，地址编码与逆编码不可用。"
                    + "配置方式（任一）：① 环境变量 " + envVar + "（生产推荐）；"
                    + "② 启动参数 --dms.map." + providerName + ".api-key=xxx 或外部化配置文件；"
                    + "③ 《配送参数》→ " + configKey + "（页面保存后热生效，无需重启）";
        };
    }

    // ==================== VRP 约束（容量 / 时间窗 / 多起点 / 多车型） ====================

    /** 是否启用约束：显式 enableConstraints，或填了载重/车型/任一点时间窗 */
    private boolean constraintsEnabled(RoutePlanRequest req, List<RoutePlanRequest.Coordinate> stops) {
        if (Boolean.FALSE.equals(req.getEnableConstraints())) {
            return false;
        }
        if (Boolean.TRUE.equals(req.getEnableConstraints())) {
            return true;
        }
        if (req.getVehicleCapacity() != null && req.getVehicleCapacity().signum() > 0) {
            return true;
        }
        if (req.getVehicles() != null && !req.getVehicles().isEmpty()) {
            return true;
        }
        return stops.stream().anyMatch(c -> StringUtils.hasText(c.getTimeWindowEnd()));
    }

    /** 时间窗优先 + 最近邻接续排序 */
    private List<RoutePlanRequest.Coordinate> orderByTimeWindow(RoutePlanRequest.Coordinate origin,
                                                               List<RoutePlanRequest.Coordinate> stops) {
        List<RoutePlanRequest.Coordinate> timed = new ArrayList<>();
        List<RoutePlanRequest.Coordinate> untimed = new ArrayList<>();
        for (RoutePlanRequest.Coordinate c : stops) {
            if (StringUtils.hasText(c.getTimeWindowEnd())) {
                timed.add(c);
            } else {
                untimed.add(c);
            }
        }
        timed.sort(Comparator.comparingInt(c -> parseTimeToSeconds(c.getTimeWindowEnd(), Integer.MAX_VALUE)));

        List<RoutePlanRequest.Coordinate> ordered = new ArrayList<>(timed);
        RoutePlanRequest.Coordinate cursor = ordered.isEmpty() ? origin : ordered.get(ordered.size() - 1);
        List<RoutePlanRequest.Coordinate> remaining = new ArrayList<>(untimed);
        while (!remaining.isEmpty()) {
            int nearest = 0;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                double d = GeoUtils.distanceMeters(cursor.getLat(), cursor.getLng(),
                        remaining.get(i).getLat(), remaining.get(i).getLng());
                if (d < min) {
                    min = d;
                    nearest = i;
                }
            }
            cursor = remaining.remove(nearest);
            ordered.add(cursor);
        }
        return ordered;
    }

    /**
     * 带约束的规划：排序 → 约束编排（{@link RouteVrpPlanner}）→ 组装响应
     */
    private RoutePlanResponse planWithConstraints(RoutePlanRequest req, List<RoutePlanRequest.Coordinate> stops) {
        RoutePlanRequest.Coordinate sortOrigin = (req.getOrigins() != null && !req.getOrigins().isEmpty())
                ? req.getOrigins().get(0) : req.getOrigin();
        List<RoutePlanRequest.Coordinate> ordered = orderByTimeWindow(sortOrigin, stops);

        RouteVrpPlanner.VrpResult vrp = vrpPlanner.plan(req, ordered);

        String message = vrp.warnings().isEmpty()
                ? "已按容量/时间窗约束规划（" + vrp.batches().size() + " 车）"
                : "已按约束规划（" + vrp.batches().size() + " 车），存在 " + vrp.warnings().size() + " 条告警"
                + (vrp.unassigned().isEmpty() ? "" : "，" + vrp.unassigned().size() + " 个点位未排入");

        return RoutePlanResponse.builder()
                .success(true)
                .provider(vrp.provider())
                .degraded(vrp.degraded())
                .travelMode(req.getDirection())
                .coordSystem(GeoUtils.COORD_GCJ02)
                .totalDistance(vrp.totalDistance())
                .totalDuration(vrp.totalDuration())
                .routePoints(vrp.routePoints())
                .stops(vrp.stops())
                .batches(vrp.batches())
                .unassigned(vrp.unassigned())
                .warnings(vrp.warnings())
                .durationSource(vrp.durationSource())
                .message(message)
                .build();
    }

    /** "HH:mm" → 当日秒数；解析失败返回默认值 */
    private int parseTimeToSeconds(String hhmm, int defaultValue) {
        if (!StringUtils.hasText(hhmm) || !hhmm.contains(":")) {
            return defaultValue;
        }
        String[] parts = hhmm.trim().split(":");
        try {
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (h < 0 || h > 47 || m < 0 || m > 59) {
                return defaultValue;
            }
            return h * 3600 + m * 60;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ==================== 私有方法 ====================

    /** 归一化：destinations 与 waypoints 合并、坐标体系转换、出行方式默认值 */
    private RoutePlanRequest normalize(RoutePlanRequest request) {
        String coordSystem = request.getCoordSystem();
        request.setOrigin(convertCoord(request.getOrigin(), coordSystem));
        if (request.getDestination() != null) {
            request.setDestination(convertCoord(request.getDestination(), coordSystem));
        }
        if (request.getWaypoints() != null) {
            request.getWaypoints().forEach(w -> convertCoordInPlace(w, coordSystem));
        }
        if (request.getDestinations() != null) {
            request.getDestinations().forEach(w -> convertCoordInPlace(w, coordSystem));
        }
        if (request.getDirection() == null || request.getDirection().isBlank()) {
            request.setDirection("DRIVING");
        } else {
            request.setDirection(request.getDirection().trim().toUpperCase());
        }
        return request;
    }

    private RoutePlanRequest.Coordinate convertCoord(RoutePlanRequest.Coordinate c, String coordSystem) {
        if (c == null || coordSystem == null || coordSystem.isBlank()) {
            return c;
        }
        double[] gcj = GeoUtils.toGcj02(c.getLat(), c.getLng(), coordSystem);
        c.setLat(gcj[0]);
        c.setLng(gcj[1]);
        return c;
    }

    private void convertCoordInPlace(RoutePlanRequest.Coordinate c, String coordSystem) {
        convertCoord(c, coordSystem);
    }

    /** 合并 waypoints + destinations + destination（历史前端字段名不一致导致目的地被忽略的问题在此彻底消除） */
    private List<RoutePlanRequest.Coordinate> collectStops(RoutePlanRequest request) {
        List<RoutePlanRequest.Coordinate> stops = new ArrayList<>();
        if (request.getWaypoints() != null) {
            stops.addAll(request.getWaypoints());
        }
        if (request.getDestinations() != null) {
            stops.addAll(request.getDestinations());
        }
        if (request.getDestination() != null) {
            RoutePlanRequest.Coordinate dest = request.getDestination();
            boolean sameAsLast = !stops.isEmpty()
                    && Math.abs(stops.get(stops.size() - 1).getLat() - dest.getLat()) < 1e-9
                    && Math.abs(stops.get(stops.size() - 1).getLng() - dest.getLng()) < 1e-9;
            if (!sameAsLast) {
                // 高德语义：origin + waypoints + destination，终点作为最后一个配送点
                stops.add(dest);
            }
        }
        stops.removeIf(c -> c == null || !GeoUtils.validCoord(c.getLat(), c.getLng()));
        return stops;
    }

    /** 按出行方式调用对应能力 */
    private RoutePlanResponse invoke(MapService service, RoutePlanRequest request, String direction) {
        String mode = direction == null ? "DRIVING" : direction.trim().toUpperCase();
        return switch (mode) {
            case "CYCLING" -> service.planCyclingRoute(request);
            case "WALKING" -> service.planWalkingRoute(request);
            default -> service.planDrivingRoute(request);
        };
    }

    /** 组装结果点位（含起点，序号从 visitedCount+1 开始，便于重规划续编） */
    private List<RoutePlanResponse.RouteStop> buildStops(RoutePlanRequest request,
                                                         List<RoutePlanRequest.Coordinate> ordered,
                                                         List<Integer> order,
                                                         RoutePlanResponse response,
                                                         Integer visitedCount) {
        int offset = visitedCount == null ? 0 : Math.max(visitedCount, 0);
        List<RoutePlanResponse.RouteStop> stops = new ArrayList<>();
        int index = offset + 1;

        stops.add(RoutePlanResponse.RouteStop.builder()
                .index(index++)
                .type("start")
                .address(request.getOrigin().getAddress())
                .lat(request.getOrigin().getLat())
                .lng(request.getOrigin().getLng())
                .sourceIndex(null)
                .build());

        // 逐段距离/时长：仅当服务返回的 steps 与点数一一对应时回填（真实路网结果的分段为导航步骤，不对应单个配送点）
        List<RoutePlanResponse.RouteStep> steps = response.getSteps();
        boolean legAligned = steps != null && steps.size() == ordered.size();

        for (int i = 0; i < ordered.size(); i++) {
            RoutePlanRequest.Coordinate c = ordered.get(i);
            RoutePlanResponse.RouteStop.RouteStopBuilder builder = RoutePlanResponse.RouteStop.builder()
                    .index(index++)
                    .type(i == ordered.size() - 1 ? "destination" : "waypoint")
                    .address(c.getAddress())
                    .lat(c.getLat())
                    .lng(c.getLng())
                    .sourceIndex(order.get(i));
            if (legAligned) {
                builder.distanceFromPrev(steps.get(i).getDistance())
                        .durationFromPrev(steps.get(i).getDuration());
            }
            stops.add(builder.build());
        }
        return stops;
    }

    /**
     * 贪心最近邻算法（TSP 简化版）：从起点开始，每次选择最近的未访问配送点
     */
    private List<Integer> greedyNearestNeighbor(RoutePlanRequest.Coordinate origin,
                                                List<RoutePlanRequest.Coordinate> stops) {
        int n = stops.size();
        boolean[] visited = new boolean[n];
        List<Integer> order = new ArrayList<>();

        double currentLat = origin.getLat();
        double currentLng = origin.getLng();

        for (int i = 0; i < n; i++) {
            int nearestIdx = -1;
            double minDist = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (visited[j]) {
                    continue;
                }
                double dist = GeoUtils.distanceMeters(currentLat, currentLng,
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

    /** 任意体系 → 目标体系（经 GCJ-02 中转） */
    private double[] convert(double lat, double lng, String from, String to) {
        if (from.equals(to)) {
            return new double[]{lat, lng};
        }
        double[] gcj = GeoUtils.toGcj02(lat, lng, from);
        return switch (to) {
            case GeoUtils.COORD_WGS84 -> GeoUtils.gcj02ToWgs84(gcj[0], gcj[1]);
            case GeoUtils.COORD_BD09 -> GeoUtils.gcj02ToBd09(gcj[0], gcj[1]);
            default -> gcj;
        };
    }
}
