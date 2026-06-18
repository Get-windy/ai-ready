package cn.aiedge.dms.route.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.route.dto.*;
import cn.aiedge.dms.route.service.RouteService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 路线规划与地图服务控制器
 *
 * @author AI-Ready Team
 */
@Tag(name = "路线规划")
@RestController
@RequestMapping("/api/dms/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @Operation(summary = "配送路线规划（多点最优）")
    @PostMapping("/plan")
    @SaCheckLogin
    public ApiResponse<RoutePlanResponse> planDeliveryRoute(
            @Parameter(description = "路线规划请求") @RequestBody RoutePlanRequest request) {
        RoutePlanResponse response = routeService.planDeliveryRoute(
                request.getOrigin(),
                request.getWaypoints() != null ? request.getWaypoints() : List.of(),
                request.getStrategy());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "地理编码：地址转坐标")
    @GetMapping("/geocode")
    @SaCheckLogin
    public ApiResponse<GeocodeResponse> geocode(@Parameter(description = "地址") @RequestParam String address) {
        return ApiResponse.ok(routeService.geocode(address));
    }

    @Operation(summary = "逆地理编码：坐标转地址")
    @GetMapping("/reverse-geocode")
    @SaCheckLogin
    public ApiResponse<ReverseGeocodeResponse> reverseGeocode(
            @Parameter(description = "纬度") @RequestParam double lat,
            @Parameter(description = "经度") @RequestParam double lng) {
        return ApiResponse.ok(routeService.reverseGeocode(lat, lng));
    }

    @Operation(summary = "批量距离计算")
    @PostMapping("/distance")
    @SaCheckLogin
    public ApiResponse<DistanceResponse> calculateDistances(
            @Parameter(description = "距离计算请求") @RequestBody DistanceRequest request) {
        return ApiResponse.ok(routeService.calculateDistances(
                request.getOrigin(), request.getDestinations()));
    }

    @Operation(summary = "电子围栏校验")
    @GetMapping("/fence-check")
    @SaCheckLogin
    public ApiResponse<Boolean> fenceCheck(
            @Parameter(description = "纬度") @RequestParam double lat,
            @Parameter(description = "经度") @RequestParam double lng,
            @Parameter(description = "中心点纬度") @RequestParam double centerLat,
            @Parameter(description = "中心点经度") @RequestParam double centerLng,
            @Parameter(description = "半径(米)") @RequestParam double radiusMeters) {
        return ApiResponse.ok(routeService.checkFence(lat, lng, centerLat, centerLng, radiusMeters));
    }
}
