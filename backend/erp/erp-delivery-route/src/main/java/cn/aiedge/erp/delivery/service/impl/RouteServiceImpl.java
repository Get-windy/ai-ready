package cn.aiedge.erp.delivery.service.impl;

import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.entity.RoutePoint;
import cn.aiedge.erp.delivery.enums.PointStatus;
import cn.aiedge.erp.delivery.enums.RouteStatus;
import cn.aiedge.erp.delivery.mapper.DeliveryRouteMapper;
import cn.aiedge.erp.delivery.mapper.RoutePointMapper;
import cn.aiedge.erp.delivery.service.AmapService;
import cn.aiedge.erp.delivery.service.RouteService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final DeliveryRouteMapper routeMapper;
    private final RoutePointMapper pointMapper;
    private final AmapService amapService;

    @Override
    @Transactional
    public RoutePlanResult planRoute(RoutePlanRequest request) {
        DeliveryRoute route = new DeliveryRoute();
        route.setRouteCode("RTE" + IdUtil.fastSimpleUUID().substring(0, 8));
        route.setDeliveryPersonId(request.getDeliveryPersonId());
        route.setDeliveryPersonName(request.getDeliveryPersonName());
        route.setStartPoint(request.getStartAddress());
        route.setStartLatitude(request.getStartLatitude());
        route.setStartLongitude(request.getStartLongitude());
        route.setTotalPoints(request.getPoints().size());
        route.setCompletedPoints(0);
        route.setStatus(RouteStatus.PLANNING.getCode());
        routeMapper.insert(route);

        List<RoutePoint> points = new ArrayList<>();
        int order = 1;
        for (RoutePointRequest pointReq : request.getPoints()) {
            RoutePoint point = new RoutePoint();
            point.setRouteId(route.getId());
            point.setPointOrder(order++);
            point.setOrderId(pointReq.getOrderId());
            point.setOrderNo(pointReq.getOrderNo());
            point.setCustomerName(pointReq.getCustomerName());
            point.setCustomerPhone(pointReq.getCustomerPhone());
            point.setAddress(pointReq.getAddress());
            point.setLatitude(pointReq.getLatitude());
            point.setLongitude(pointReq.getLongitude());
            point.setStatus(PointStatus.PENDING.getCode());
            pointMapper.insert(point);
            points.add(point);
        }

        String origin = request.getStartLongitude() + "," + request.getStartLatitude();
        List<String> waypoints = points.stream()
                .map(p -> p.getLongitude() + "," + p.getLatitude())
                .collect(Collectors.toList());
        String destination = waypoints.get(waypoints.size() - 1);

        List<String> optimizedOrder = amapService.optimizeRouteOrder(origin, waypoints, destination);

        AmapRouteResult amapResult = amapService.getMultiPointRoute(
                origin, destination, waypoints.subList(0, waypoints.size() - 1), request.getStrategy());

        if (amapResult.getRoute() != null && !amapResult.getRoute().isEmpty()) {
            AmapRoutePath path = amapResult.getRoute().get(0);
            route.setTotalDistance(path.getDistance());
            route.setTotalDuration(path.getDuration());
            route.setRouteData(JSONUtil.toJsonStr(amapResult));
            route.setOptimizedRouteData(JSONUtil.toJsonStr(optimizedOrder));
            route.setStatus(RouteStatus.READY.getCode());
            routeMapper.updateById(route);
        }

        return buildRoutePlanResult(route, points);
    }

    @Override
    @Transactional
    public RoutePlanResult optimizeRoute(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在: " + routeId);
        }

        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        List<RoutePoint> pendingPoints = points.stream()
                .filter(p -> PointStatus.PENDING.getCode().equals(p.getStatus()) || 
                             PointStatus.IN_ROUTE.getCode().equals(p.getStatus()))
                .collect(Collectors.toList());

        if (pendingPoints.isEmpty()) {
            throw new RuntimeException("没有待配送的点");
        }

        String origin = route.getStartLongitude() + "," + route.getStartLatitude();
        List<String> waypoints = pendingPoints.stream()
                .map(p -> p.getLongitude() + "," + p.getLatitude())
                .collect(Collectors.toList());

        List<String> optimizedOrder = amapService.optimizeRouteOrder(origin, waypoints, waypoints.get(waypoints.size() - 1));

        int newOrder = 1;
        for (String location : optimizedOrder) {
            if (location.equals(origin)) continue;
            for (RoutePoint point : pendingPoints) {
                if ((point.getLongitude() + "," + point.getLatitude()).equals(location)) {
                    point.setPointOrder(newOrder++);
                    pointMapper.updateById(point);
                    break;
                }
            }
        }

        route.setOptimizedRouteData(JSONUtil.toJsonStr(optimizedOrder));
        routeMapper.updateById(route);

        return buildRoutePlanResult(route, pointMapper.selectByRouteId(routeId));
    }

    @Override
    public DeliveryRoute getRouteById(Long id) {
        return routeMapper.selectById(id);
    }

    @Override
    public DeliveryRoute getActiveRouteByPerson(String deliveryPersonId) {
        return routeMapper.selectActiveRouteByPerson(deliveryPersonId);
    }

    @Override
    public Page<DeliveryRoute> listRoutes(Integer page, Integer size, String deliveryPersonId, String status) {
        Page<DeliveryRoute> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<DeliveryRoute> wrapper = new LambdaQueryWrapper<>();
        if (deliveryPersonId != null && !deliveryPersonId.isEmpty()) {
            wrapper.eq(DeliveryRoute::getDeliveryPersonId, deliveryPersonId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DeliveryRoute::getStatus, status);
        }
        wrapper.orderByDesc(DeliveryRoute::getCreateTime);
        return routeMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void startRoute(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在: " + routeId);
        }
        route.setStatus(RouteStatus.IN_PROGRESS.getCode());
        route.setStartTime(LocalDateTime.now());
        routeMapper.updateById(route);

        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        if (!points.isEmpty()) {
            RoutePoint firstPoint = points.get(0);
            firstPoint.setStatus(PointStatus.IN_ROUTE.getCode());
            pointMapper.updateById(firstPoint);
        }
    }

    @Override
    @Transactional
    public void completeRoute(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在: " + routeId);
        }
        route.setStatus(RouteStatus.COMPLETED.getCode());
        route.setCompleteTime(LocalDateTime.now());
        int deliveredCount = pointMapper.countDeliveredByRoute(routeId);
        route.setCompletedPoints(deliveredCount);
        routeMapper.updateById(route);
    }

    @Override
    @Transactional
    public void cancelRoute(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在: " + routeId);
        }
        route.setStatus(RouteStatus.CANCELLED.getCode());
        routeMapper.updateById(route);
    }

    @Override
    @Transactional
    public void updatePointStatus(Long routeId, Integer pointOrder, String status) {
        RoutePoint point = pointMapper.selectByRouteIdAndOrder(routeId, pointOrder);
        if (point == null) {
            throw new RuntimeException("配送点不存在");
        }
        point.setStatus(status);
        if (PointStatus.ARRIVED.getCode().equals(status)) {
            point.setArriveTime(LocalDateTime.now());
        } else if (PointStatus.DELIVERED.getCode().equals(status)) {
            point.setLeaveTime(LocalDateTime.now());
        }
        pointMapper.updateById(point);

        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route != null) {
            int deliveredCount = pointMapper.countDeliveredByRoute(routeId);
            route.setCompletedPoints(deliveredCount);
            routeMapper.updateById(route);
        }
    }

    @Override
    public RoutePlanResult getRouteDetail(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在: " + routeId);
        }
        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        return buildRoutePlanResult(route, points);
    }

    @Override
    public NavigationResult getNavigation(NavigationRequest request) {
        String origin = request.getCurrentLongitude() + "," + request.getCurrentLatitude();
        String destination = request.getTargetLongitude() + "," + request.getTargetLatitude();

        AmapRouteResult amapResult = amapService.getDrivingRoute(origin, destination, "1");

        NavigationResult result = new NavigationResult();
        result.setNavigationUrl(amapService.getNavigationUrl(origin, destination));

        if (amapResult.getRoute() != null && !amapResult.getRoute().isEmpty()) {
            AmapRoutePath path = amapResult.getRoute().get(0);
            result.setDistance(path.getDistance());
            result.setDuration(path.getDuration());

            if (path.getSteps() != null) {
                List<NavigationStep> steps = path.getSteps().stream()
                        .map(s -> {
                            NavigationStep step = new NavigationStep();
                            step.setInstruction(s.getInstruction());
                            step.setDistance(s.getDistance());
                            step.setDuration(s.getDuration());
                            step.setRoadName(s.getRoad());
                            return step;
                        })
                        .collect(Collectors.toList());
                result.setSteps(steps);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public RoutePlanResult reoptimizeByTraffic(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在: " + routeId);
        }

        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        List<RoutePoint> pendingPoints = points.stream()
                .filter(p -> PointStatus.PENDING.getCode().equals(p.getStatus()))
                .collect(Collectors.toList());

        if (pendingPoints.size() < 2) {
            return buildRoutePlanResult(route, points);
        }

        RoutePoint currentPoint = points.stream()
                .filter(p -> PointStatus.IN_ROUTE.getCode().equals(p.getStatus()))
                .findFirst()
                .orElse(points.get(0));

        String origin = currentPoint.getLongitude() + "," + currentPoint.getLatitude();
        List<String> waypoints = pendingPoints.stream()
                .map(p -> p.getLongitude() + "," + p.getLatitude())
                .collect(Collectors.toList());

        List<String> optimizedOrder = amapService.optimizeRouteOrder(origin, waypoints, waypoints.get(waypoints.size() - 1));

        int newOrder = currentPoint.getPointOrder() + 1;
        for (String location : optimizedOrder) {
            if (location.equals(origin)) continue;
            for (RoutePoint point : pendingPoints) {
                if ((point.getLongitude() + "," + point.getLatitude()).equals(location)) {
                    point.setPointOrder(newOrder++);
                    pointMapper.updateById(point);
                    break;
                }
            }
        }

        route.setOptimizedRouteData(JSONUtil.toJsonStr(optimizedOrder));
        routeMapper.updateById(route);

        return buildRoutePlanResult(route, pointMapper.selectByRouteId(routeId));
    }

    private RoutePlanResult buildRoutePlanResult(DeliveryRoute route, List<RoutePoint> points) {
        RoutePlanResult result = new RoutePlanResult();
        result.setRouteId(route.getId());
        result.setRouteCode(route.getRouteCode());
        result.setTotalPoints(route.getTotalPoints());
        result.setTotalDistance(route.getTotalDistance());
        result.setTotalDuration(route.getTotalDuration());
        result.setOptimizedOrder(route.getOptimizedRouteData());

        List<RoutePointResult> pointResults = points.stream()
                .sorted((a, b) -> a.getPointOrder() - b.getPointOrder())
                .map(p -> {
                    RoutePointResult pr = new RoutePointResult();
                    pr.setPointId(p.getId());
                    pr.setPointOrder(p.getPointOrder());
                    pr.setOrderId(p.getOrderId());
                    pr.setOrderNo(p.getOrderNo());
                    pr.setCustomerName(p.getCustomerName());
                    pr.setAddress(p.getAddress());
                    pr.setLatitude(p.getLatitude());
                    pr.setLongitude(p.getLongitude());
                    pr.setDistanceFromPrev(p.getDistanceFromPrev());
                    pr.setDurationFromPrev(p.getDurationFromPrev());
                    pr.setStatus(p.getStatus());
                    return pr;
                })
                .collect(Collectors.toList());
        result.setPoints(pointResults);

        return result;
    }
}