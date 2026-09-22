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
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 路线规划与地理能力控制器
 *
 * 统一前缀 `/api/dms/route`（此前前端漏写 `/route` 前缀导致地理编码 404，现已收敛）：
 * plan / reoptimize / geocode / reverse-geocode / distance / fence-check / convert / config
 *
 * @author AI-Ready Team
 */
@Tag(name = "路线规划")
@RestController
@RequestMapping("/api/dms/route")
@RequiredArgsConstructor
@SaCheckLogin
public class RouteController {

    private final RouteService routeService;

    @Operation(summary = "配送路线规划（多点最优，兼容 waypoints / destinations）")
    @SaCheckPermission("dms:route:update")
    @PostMapping("/plan")
    public ApiResponse<RoutePlanResponse> planDeliveryRoute(
            @Parameter(description = "路线规划请求") @RequestBody RoutePlanRequest request) {
        return ApiResponse.success(routeService.planDeliveryRoute(request));
    }

    @Operation(summary = "重新规划（当前位置 → 剩余点位重排序）")
    @SaCheckPermission("dms:route:update")
    @PostMapping("/reoptimize")
    public ApiResponse<RoutePlanResponse> reoptimize(
            @Parameter(description = "重新规划请求，visitedCount 为已访问点数") @RequestBody RoutePlanRequest request) {
        return ApiResponse.success(routeService.reoptimize(request));
    }

    @Operation(summary = "地理编码：地址转坐标（返回候选列表，带缓存）")
    @SaCheckPermission("dms:route:view")
    @GetMapping("/geocode")
    public ApiResponse<GeocodeResponse> geocode(
            @Parameter(description = "地址") @RequestParam String address,
            @Parameter(description = "城市（可选，辅助解析）") @RequestParam(required = false) String city) {
        return ApiResponse.success(routeService.geocode(address, city));
    }

    @Operation(summary = "逆地理编码：坐标转地址")
    @SaCheckPermission("dms:route:view")
    @GetMapping("/reverse-geocode")
    public ApiResponse<ReverseGeocodeResponse> reverseGeocode(
            @Parameter(description = "纬度") @RequestParam double lat,
            @Parameter(description = "经度") @RequestParam double lng,
            @Parameter(description = "入参坐标体系 WGS84/GCJ02/BD09，默认 GCJ02") @RequestParam(required = false) String from) {
        return ApiResponse.success(routeService.reverseGeocode(lat, lng, from));
    }

    @Operation(summary = "批量距离计算")
    @SaCheckPermission("dms:route:update")
    @PostMapping("/distance")
    public ApiResponse<DistanceResponse> calculateDistances(
            @Parameter(description = "距离计算请求") @RequestBody DistanceRequest request) {
        return ApiResponse.success(routeService.calculateDistances(request));
    }

    @Operation(summary = "电子围栏校验（围栏档案 / 内联圆形 / 内联多边形，支持批量点位）")
    @SaCheckPermission("dms:route:update")
    @PostMapping("/fence-check")
    public ApiResponse<FenceCheckResponse> fenceCheck(
            @Parameter(description = "围栏校验请求") @RequestBody FenceCheckRequest request) {
        return ApiResponse.success(routeService.fenceCheck(request));
    }

    @Operation(summary = "坐标体系转换（WGS84 / GCJ02 / BD09 互转）")
    @SaCheckPermission("dms:route:update")
    @PostMapping("/convert")
    public ApiResponse<CoordConvertResponse> convert(
            @Parameter(description = "坐标转换请求") @RequestBody CoordConvertRequest request) {
        return ApiResponse.success(routeService.convert(request));
    }

    @Operation(summary = "地理能力配置状态（服务商 / 是否降级 / 坐标体系）")
    @SaCheckPermission("dms:route:view")
    @GetMapping("/config")
    public ApiResponse<RouteConfigResponse> config() {
        return ApiResponse.success(routeService.config());
    }

    @Operation(summary = "地图服务连通性自检（用当前 Key 真实调用一次地理编码）")
    @SaCheckPermission("dms:route:update")
    @PostMapping("/verify")
    public ApiResponse<RouteVerifyResponse> verify() {
        return ApiResponse.success(routeService.verify());
    }
}
