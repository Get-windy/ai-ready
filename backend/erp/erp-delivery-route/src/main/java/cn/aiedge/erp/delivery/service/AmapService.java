package cn.aiedge.erp.delivery.service;

import cn.aiedge.erp.delivery.dto.*;
import java.util.List;

public interface AmapService {

    AmapRouteResult getDrivingRoute(String origin, String destination, String strategy);

    AmapRouteResult getMultiPointRoute(String origin, String destination, List<String> waypoints, String strategy);

    String getNavigationUrl(String origin, String destination);

    String geocodeAddress(String address);

    String regeocodeLocation(String location);

    Double calculateDistance(String origin, String destination);

    List<String> optimizeRouteOrder(String origin, List<String> waypoints, String destination);
}