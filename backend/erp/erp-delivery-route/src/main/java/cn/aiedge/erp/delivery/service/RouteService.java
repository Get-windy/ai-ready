package cn.aiedge.erp.delivery.service;

import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface RouteService {

    RoutePlanResult planRoute(RoutePlanRequest request);

    RoutePlanResult optimizeRoute(Long routeId);

    DeliveryRoute getRouteById(Long id);

    DeliveryRoute getActiveRouteByPerson(String deliveryPersonId);

    Page<DeliveryRoute> listRoutes(Integer page, Integer size, String deliveryPersonId, String status);

    void startRoute(Long routeId);

    void completeRoute(Long routeId);

    void cancelRoute(Long routeId);

    void updatePointStatus(Long routeId, Integer pointOrder, String status);

    RoutePlanResult getRouteDetail(Long routeId);

    NavigationResult getNavigation(NavigationRequest request);

    RoutePlanResult reoptimizeByTraffic(Long routeId);
}