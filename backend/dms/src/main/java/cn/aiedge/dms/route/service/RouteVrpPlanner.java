package cn.aiedge.dms.route.service;

import cn.aiedge.dms.common.util.GeoUtils;
import cn.aiedge.dms.route.dto.*;
import cn.aiedge.dms.route.spi.LocalMapService;
import cn.aiedge.dms.route.spi.MapService;
import cn.aiedge.dms.route.spi.MapServiceRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * VRP 编排（容量 / 时间窗 / 多起点 / 多车型）——《路线规划》的约束规划内核
 *
 * 能力（在「最近邻 + 地图算路」之上）：
 * 1. **时间窗优先**：窗口结束早的点先送，无窗点按最近邻接续（软约束时晚到仅告警）；
 * 2. **硬时间窗**（`hardTimeWindow=true`）：晚到的点**移出排程**进入 `unassigned`（并重算该批）；
 * 3. **容量分批**：按载重上限贪婪分批，一车一批；单点超载独占一批并告警；
 * 4. **多车型**：按「能装下的最小车型」分配（车辆数量有限，不足则整批进入 `unassigned`）；
 * 5. **多起点**：每批就近选择出发点（`origins`），未配置时用单起点；
 * 6. **时长来源**：优先用地图服务路网时长（`durationSource=MAP`），不可用/超上限时按平均时速估算（`ESTIMATE`）。
 *
 * 抽取为独立组件，便于与《智能调度》共用同一套约束口径（不在各页重复实现）。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteVrpPlanner {

    /** 路网时长单批最大取点数（超过则整体按估算，避免地图配额被排程打满） */
    private static final int MAP_LEG_LIMIT = 10;

    /** 估算时速（km/h） */
    private static final double ESTIMATE_SPEED_KMH = 30d;

    private final MapServiceRouter mapServiceRouter;
    private final LocalMapService localMapService;

    /** 规划结果（供 RouteService 组装响应） */
    public record VrpResult(
            List<RoutePlanResponse.RouteBatch> batches,
            List<RoutePlanResponse.RouteStop> stops,
            List<RoutePlanResponse.RouteStop> unassigned,
            List<RoutePlanResponse.RoutePoint> routePoints,
            List<String> warnings,
            long totalDistance,
            long totalDuration,
            boolean degraded,
            String provider,
            String durationSource) {
    }

    /**
     * 带约束的规划入口
     *
     * @param req   规划请求（含 origins/vehicles/vehicleCapacity/departureTime/hardTimeWindow）
     * @param stops 已按时间窗优先排序的配送点
     */
    public VrpResult plan(RoutePlanRequest req, List<RoutePlanRequest.Coordinate> stops) {
        List<String> warnings = new ArrayList<>();
        List<RoutePlanResponse.RouteStop> unassigned = new ArrayList<>();
        boolean hard = Boolean.TRUE.equals(req.getHardTimeWindow());

        List<RoutePlanRequest.Coordinate> origins = (req.getOrigins() != null && !req.getOrigins().isEmpty())
                ? req.getOrigins() : List.of(req.getOrigin());
        int departureSeconds = parseTimeToSeconds(req.getDepartureTime(), 8 * 60);

        // 车型池：显式 vehicles 优先，否则用 vehicleCapacity 构造「默认车辆」
        List<Vehicle> vehiclePool = buildVehiclePool(req, warnings);

        // 硬时间窗预筛：按「最早可到达」估算，明显送不到的点直接移出排程
        List<RoutePlanRequest.Coordinate> feasible = new ArrayList<>();
        for (RoutePlanRequest.Coordinate c : stops) {
            if (hard && !feasibleByEstimate(origins.get(0), c, departureSeconds, req)) {
                unassigned.add(toStop(c, null, null, null));
                warnings.add(labelOf(c, -1) + " 最早预计到达已晚于时间窗 " + c.getTimeWindowEnd() + "，已从排程移出（硬时间窗）");
                continue;
            }
            feasible.add(c);
        }

        // 容量分批（按可用车型的最大载重切分，随后再逐批选车）
        BigDecimal capacity = effectiveCapacity(req, vehiclePool);
        List<List<RoutePlanRequest.Coordinate>> batches = splitByCapacity(feasible, capacity, warnings);

        MapService service = mapServiceRouter.current();
        String direction = req.getDirection();
        List<RoutePlanResponse.RouteBatch> batchResults = new ArrayList<>();
        List<RoutePlanResponse.RouteStop> allStops = new ArrayList<>();
        List<RoutePlanResponse.RoutePoint> allPoints = new ArrayList<>();
        long totalDistance = 0L;
        long totalDuration = 0L;
        boolean degraded = false;
        boolean mapDurations = false;
        String provider = service == null ? "local" : service.getProvider();
        int index = (req.getVisitedCount() == null ? 0 : Math.max(req.getVisitedCount(), 0)) + 1;

        for (int b = 0; b < batches.size(); b++) {
            List<RoutePlanRequest.Coordinate> batchStops = new ArrayList<>(batches.get(b));
            int originIndex = nearestOriginIndex(origins, batchStops.get(0));
            RoutePlanRequest.Coordinate origin = origins.get(originIndex);

            // 逐批算路（含外部服务失败降级）
            RoutePlanRequest planRequest = RoutePlanRequest.builder()
                    .origin(origin)
                    .waypoints(batchStops.size() > 1 ? batchStops.subList(0, batchStops.size() - 1) : null)
                    .destination(batchStops.get(batchStops.size() - 1))
                    .strategy(req.getStrategy())
                    .direction(direction)
                    .vehicleType(req.getVehicleType())
                    .plateNo(req.getPlateNo())
                    .build();
            RoutePlanResponse leg = invoke(service, planRequest, direction);
            if (!leg.isSuccess() && !"local".equals(provider)) {
                String reason = leg.getMessage();
                leg = invoke(localMapService, planRequest, direction);
                leg.setMessage((reason == null ? "" : reason + "；") + "已降级为直线距离估算");
            }
            degraded = degraded || leg.isDegraded();
            provider = leg.getProvider() == null ? provider : leg.getProvider();

            // 路网时长（可用时）：优先 MAP
            long[] legSeconds = fetchLegDurations(service, provider, origin, batchStops);
            if (legSeconds != null) {
                mapDurations = true;
            }

            // 组装批内点位与 ETA；硬时间窗下把晚到的点移出并重算一次
            boolean replanned = false;
            List<RoutePlanResponse.RouteStop> batchOut = new ArrayList<>();
            int etaSeconds = departureSeconds;
            long prevStay = 0L;
            for (int i = 0; i < batchStops.size(); i++) {
                RoutePlanRequest.Coordinate c = batchStops.get(i);
                long seconds = legSeconds != null ? legSeconds[i]
                        : estimateSeconds(origin, c, i == 0 ? null : batchStops.get(i - 1));
                etaSeconds += prevStay + seconds;
                String eta = formatSeconds(etaSeconds);

                int windowEnd = parseTimeToSeconds(c.getTimeWindowEnd(), -1);
                boolean late = windowEnd >= 0 && etaSeconds > windowEnd;
                if (late && hard && !replanned) {
                    replanned = true;
                    batchStops.remove(i);
                    unassigned.add(toStop(c, eta, null, c.getDemand()));
                    warnings.add(labelOf(c, i) + " 预计 " + eta + " 到达，晚于时间窗 " + c.getTimeWindowEnd()
                            + "，已从该批移出（硬时间窗）");
                    i--;
                    // 重算该批（剔除后重新算路与 ETA）
                    planRequest = RoutePlanRequest.builder()
                            .origin(origin)
                            .waypoints(batchStops.size() > 1 ? batchStops.subList(0, batchStops.size() - 1) : null)
                            .destination(batchStops.isEmpty() ? origin : batchStops.get(batchStops.size() - 1))
                            .strategy(req.getStrategy()).direction(direction)
                            .vehicleType(req.getVehicleType()).plateNo(req.getPlateNo())
                            .build();
                    leg = invoke(service, planRequest, direction);
                    if (!leg.isSuccess() && !"local".equals(provider)) {
                        leg = invoke(localMapService, planRequest, direction);
                    }
                    legSeconds = fetchLegDurations(service, provider, origin, batchStops);
                    batchOut.clear();
                    etaSeconds = departureSeconds;
                    prevStay = 0L;
                    i = -1;
                    if (batchStops.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (late) {
                    warnings.add("第 " + (b + 1) + " 车 · " + labelOf(c, i)
                            + " 预计 " + eta + " 到达，晚于时间窗 " + c.getTimeWindowEnd());
                }
                batchOut.add(toStop(c, eta, seconds, c.getDemand()));
                prevStay = c.getStayDuration() == null ? 0L : c.getStayDuration();
            }
            if (batchStops.isEmpty()) {
                continue;
            }

            // 选车：能装下的最小车型；单点超载时用最大车型兜底（单独成批 + 告警，不丢单）
            BigDecimal load = sumDemand(batchStops);
            int batchNo = batchResults.size() + 1;
            Vehicle vehicle = pickVehicle(vehiclePool, load);
            if (vehicle == null && batchStops.size() == 1) {
                vehicle = takeLargest(vehiclePool);
                if (vehicle != null) {
                    warnings.add("第 " + batchNo + " 批为单点且载重 " + load + " 超过最大车型载重 "
                            + (vehicle.capacity() == null ? "不限" : vehicle.capacity()) + "，已按最大车型单点派车，请核实");
                }
            }
            if (vehicle == null) {
                for (RoutePlanRequest.Coordinate c : batchStops) {
                    unassigned.add(toStop(c, null, null, c.getDemand()));
                }
                warnings.add("第 " + batchNo + " 批（载重 " + load + "）没有可用车型（**可用车辆数**不足或载重不够），已整批移出排程");
                continue;
            }

            long batchDistance = leg.getTotalDistance() == null ? 0L : leg.getTotalDistance();
            long batchDuration = leg.getTotalDuration() == null ? 0L : leg.getTotalDuration();
            totalDistance += batchDistance;
            totalDuration += batchDuration;

            // 起点行只输出一次（多起点时每批各自输出，便于识别从哪个仓出发）
            List<RoutePlanResponse.RouteStop> batchRows = new ArrayList<>();
            for (RoutePlanResponse.RouteStop row : batchOut) {
                row.setBatchNo(batchNo);
            }
            batchRows.add(RoutePlanResponse.RouteStop.builder()
                    .index(index++)
                    .type("start")
                    .batchNo(batchNo)
                    .address(origin.getAddress())
                    .lat(origin.getLat())
                    .lng(origin.getLng())
                    .build());
            batchRows.addAll(batchOut);

            List<RoutePlanResponse.RoutePoint> batchPoints = leg.getRoutePoints() == null
                    ? new ArrayList<>() : new ArrayList<>(leg.getRoutePoints());
            allPoints.addAll(batchPoints);
            allStops.addAll(batchRows);

            batchResults.add(RoutePlanResponse.RouteBatch.builder()
                    .batchNo(batchNo)
                    .load(load)
                    .totalDistance(batchDistance)
                    .totalDuration(batchDuration)
                    .originIndex(originIndex)
                    .originAddress(origin.getAddress())
                    .vehicleName(vehicle.name())
                    .capacity(vehicle.capacity())
                    .stops(batchRows)
                    .routePoints(batchPoints)
                    .build());
        }

        return new VrpResult(batchResults, allStops, unassigned, allPoints, warnings,
                totalDistance, totalDuration, degraded, provider, mapDurations ? "MAP" : "ESTIMATE");
    }

    // ==================== 车型 ====================

    /** 车型（含可用数量，线程内可变） */
    private record Vehicle(String name, BigDecimal capacity, int remaining) {
    }

    private List<Vehicle> buildVehiclePool(RoutePlanRequest req, List<String> warnings) {
        List<Vehicle> pool = new ArrayList<>();
        if (req.getVehicles() != null && !req.getVehicles().isEmpty()) {
            for (RoutePlanRequest.VehicleSpec spec : req.getVehicles()) {
                if (spec == null) {
                    continue;
                }
                String name = StringUtils.hasText(spec.getName()) ? spec.getName().trim() : "车型";
                int count = spec.getCount() == null || spec.getCount() <= 0 ? Integer.MAX_VALUE : spec.getCount();
                pool.add(new Vehicle(name, spec.getCapacity(), count));
            }
            if (pool.isEmpty()) {
                warnings.add("vehicles 配置为空，已按『不限载重』排程");
            }
        }
        if (pool.isEmpty() && req.getVehicleCapacity() != null && req.getVehicleCapacity().signum() > 0) {
            int count = req.getVehicleCount() == null || req.getVehicleCount() <= 0 ? Integer.MAX_VALUE : req.getVehicleCount();
            pool.add(new Vehicle("默认车辆", req.getVehicleCapacity(), count));
        }
        if (pool.isEmpty()) {
            pool.add(new Vehicle("不限车型", null, Integer.MAX_VALUE));
        }
        pool.sort(Comparator.comparing(Vehicle::capacity, Comparator.nullsLast(Comparator.naturalOrder())));
        return pool;
    }

    /** 分批依据：显式 vehicleCapacity 优先，否则取可用车型的最大载重 */
    private BigDecimal effectiveCapacity(RoutePlanRequest req, List<Vehicle> pool) {
        if (req.getVehicleCapacity() != null && req.getVehicleCapacity().signum() > 0) {
            return req.getVehicleCapacity();
        }
        BigDecimal max = null;
        for (Vehicle v : pool) {
            if (v.capacity() != null && (max == null || v.capacity().compareTo(max) > 0)) {
                max = v.capacity();
            }
        }
        return max;
    }

    /** 选车：能装下该批载重的**最小**车型；无可用则返回 null */
    private Vehicle pickVehicle(List<Vehicle> pool, BigDecimal load) {
        for (int i = 0; i < pool.size(); i++) {
            Vehicle v = pool.get(i);
            boolean fits = v.capacity() == null || v.capacity().compareTo(load) >= 0;
            if (fits && v.remaining() > 0) {
                pool.set(i, new Vehicle(v.name(), v.capacity(), v.remaining() == Integer.MAX_VALUE ? Integer.MAX_VALUE : v.remaining() - 1));
                return v;
            }
        }
        return null;
    }

    /** 取载重最大的车型（单点超载兜底用） */
    private Vehicle takeLargest(List<Vehicle> pool) {
        Vehicle best = null;
        int bestIdx = -1;
        for (int i = 0; i < pool.size(); i++) {
            Vehicle v = pool.get(i);
            if (v.remaining() <= 0) {
                continue;
            }
            if (best == null || (v.capacity() != null
                    && (best.capacity() == null || v.capacity().compareTo(best.capacity()) > 0))) {
                best = v;
                bestIdx = i;
            }
        }
        if (best == null) {
            return null;
        }
        pool.set(bestIdx, new Vehicle(best.name(), best.capacity(),
                best.remaining() == Integer.MAX_VALUE ? Integer.MAX_VALUE : best.remaining() - 1));
        return best;
    }

    // ==================== 时长 ====================

    /**
     * 逐段时长：优先地图服务路网时长（每段单独请求），失败/超上限返回 null（调用方按估算）
     */
    private long[] fetchLegDurations(MapService service, String provider,
                                     RoutePlanRequest.Coordinate origin,
                                     List<RoutePlanRequest.Coordinate> stops) {
        if (service == null || "local".equals(provider) || stops.isEmpty() || stops.size() > MAP_LEG_LIMIT) {
            return null;
        }
        long[] seconds = new long[stops.size()];
        RoutePlanRequest.Coordinate from = origin;
        try {
            for (int i = 0; i < stops.size(); i++) {
                DistanceRequest dr = DistanceRequest.builder()
                        .origin(from)
                        .destinations(List.of(stops.get(i)))
                        .type(2)
                        .build();
                DistanceResponse resp = service.calculateDistance(dr);
                if (resp == null || !resp.isSuccess() || resp.getDistances() == null || resp.getDistances().isEmpty()
                        || resp.getDistances().get(0).getDuration() == null) {
                    return null;
                }
                seconds[i] = resp.getDistances().get(0).getDuration();
                from = stops.get(i);
            }
            return seconds;
        } catch (Exception e) {
            log.warn("[RouteVrpPlanner] 路网时长获取失败，回退估算: {}", e.getMessage());
            return null;
        }
    }

    private long estimateSeconds(RoutePlanRequest.Coordinate origin,
                                 RoutePlanRequest.Coordinate current,
                                 RoutePlanRequest.Coordinate prev) {
        RoutePlanRequest.Coordinate from = prev == null ? origin : prev;
        double meters = GeoUtils.distanceMeters(from.getLat(), from.getLng(), current.getLat(), current.getLng());
        return Math.round(meters / (ESTIMATE_SPEED_KMH * 1000d / 3600d));
    }

    /** 硬时间窗预筛：从最近起点出发、按平均时速估算的最早到达是否已超窗 */
    private boolean feasibleByEstimate(RoutePlanRequest.Coordinate origin,
                                       RoutePlanRequest.Coordinate c,
                                       int departureSeconds,
                                       RoutePlanRequest req) {
        int windowEnd = parseTimeToSeconds(c.getTimeWindowEnd(), -1);
        if (windowEnd < 0) {
            return true;
        }
        long seconds = estimateSeconds(origin, c, null);
        return departureSeconds + seconds <= windowEnd;
    }

    // ==================== 通用小工具 ====================

    private List<List<RoutePlanRequest.Coordinate>> splitByCapacity(List<RoutePlanRequest.Coordinate> ordered,
                                                                    BigDecimal capacity,
                                                                    List<String> warnings) {
        List<List<RoutePlanRequest.Coordinate>> batches = new ArrayList<>();
        if (capacity == null || capacity.signum() <= 0) {
            if (!ordered.isEmpty()) {
                batches.add(new ArrayList<>(ordered));
            }
            return batches;
        }
        List<RoutePlanRequest.Coordinate> current = new ArrayList<>();
        BigDecimal load = BigDecimal.ZERO;
        for (RoutePlanRequest.Coordinate c : ordered) {
            BigDecimal demand = c.getDemand() == null ? BigDecimal.ZERO : c.getDemand();
            if (demand.compareTo(capacity) > 0) {
                warnings.add(labelOf(c, -1) + " 需求量 " + demand + " 已超过单车载重 " + capacity + "，已单独成批，请拆分订单或换大车");
                if (!current.isEmpty()) {
                    batches.add(current);
                    current = new ArrayList<>();
                    load = BigDecimal.ZERO;
                }
            }
            if (load.add(demand).compareTo(capacity) > 0 && !current.isEmpty()) {
                batches.add(current);
                current = new ArrayList<>();
                load = BigDecimal.ZERO;
            }
            current.add(c);
            load = load.add(demand);
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private int nearestOriginIndex(List<RoutePlanRequest.Coordinate> origins, RoutePlanRequest.Coordinate target) {
        int best = 0;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < origins.size(); i++) {
            double d = GeoUtils.distanceMeters(origins.get(i).getLat(), origins.get(i).getLng(), target.getLat(), target.getLng());
            if (d < min) {
                min = d;
                best = i;
            }
        }
        return best;
    }

    private BigDecimal sumDemand(List<RoutePlanRequest.Coordinate> stops) {
        BigDecimal sum = BigDecimal.ZERO;
        for (RoutePlanRequest.Coordinate c : stops) {
            if (c.getDemand() != null) {
                sum = sum.add(c.getDemand());
            }
        }
        return sum;
    }

    private RoutePlanResponse.RouteStop toStop(RoutePlanRequest.Coordinate c, String eta, Long durationFromPrev, BigDecimal demand) {
        return RoutePlanResponse.RouteStop.builder()
                .type("waypoint")
                .address(c.getAddress())
                .lat(c.getLat()).lng(c.getLng())
                .demand(demand)
                .timeWindowStart(c.getTimeWindowStart())
                .timeWindowEnd(c.getTimeWindowEnd())
                .eta(eta)
                .durationFromPrev(durationFromPrev)
                .build();
    }

    private RoutePlanResponse invoke(MapService service, RoutePlanRequest request, String direction) {
        if (service == null) {
            return RoutePlanResponse.builder().success(false).provider("local").message("地图服务不可用").build();
        }
        String mode = direction == null ? "DRIVING" : direction.trim().toUpperCase();
        return switch (mode) {
            case "CYCLING" -> service.planCyclingRoute(request);
            case "WALKING" -> service.planWalkingRoute(request);
            default -> service.planDrivingRoute(request);
        };
    }

    private String labelOf(RoutePlanRequest.Coordinate c, int index) {
        if (StringUtils.hasText(c.getAddress())) {
            return c.getAddress();
        }
        return index >= 0 ? "第 " + (index + 1) + " 个点位" : "某点位";
    }

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

    private String formatSeconds(int seconds) {
        int normalized = seconds % (48 * 3600);
        return String.format("%02d:%02d", normalized / 3600, (normalized % 3600) / 60);
    }
}
