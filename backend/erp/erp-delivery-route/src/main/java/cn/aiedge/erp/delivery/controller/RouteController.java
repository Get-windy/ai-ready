package cn.aiedge.erp.delivery.controller;

import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.service.RouteService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "配送路线管理", description = "路线规划、优化、导航、实时路况调整")
@RestController
@RequestMapping("/api/delivery/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @Operation(summary = "规划配送路线")
    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> planRoute(@RequestBody RoutePlanRequest request) {
        RoutePlanResult result = routeService.planRoute(request);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "优化配送路线")
    @PostMapping("/{routeId}/optimize")
    public ResponseEntity<Map<String, Object>> optimizeRoute(@PathVariable Long routeId) {
        RoutePlanResult result = routeService.optimizeRoute(routeId);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取路线详情")
    @GetMapping("/{routeId}")
    public ResponseEntity<Map<String, Object>> getRouteDetail(@PathVariable Long routeId) {
        RoutePlanResult result = routeService.getRouteDetail(routeId);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取配送员当前路线")
    @GetMapping("/active/{deliveryPersonId}")
    public ResponseEntity<Map<String, Object>> getActiveRoute(@PathVariable String deliveryPersonId) {
        DeliveryRoute route = routeService.getActiveRouteByPerson(deliveryPersonId);
        return ResponseEntity.ok(success(route));
    }

    @Operation(summary = "路线列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listRoutes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String deliveryPersonId,
            @RequestParam(required = false) String status) {
        Page<DeliveryRoute> pageResult = routeService.listRoutes(page, size, deliveryPersonId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "开始配送")
    @PostMapping("/{routeId}/start")
    public ResponseEntity<Map<String, Object>> startRoute(@PathVariable Long routeId) {
        routeService.startRoute(routeId);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "完成配送")
    @PostMapping("/{routeId}/complete")
    public ResponseEntity<Map<String, Object>> completeRoute(@PathVariable Long routeId) {
        routeService.completeRoute(routeId);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "取消路线")
    @PostMapping("/{routeId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelRoute(@PathVariable Long routeId) {
        routeService.cancelRoute(routeId);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "更新配送点状态")
    @PostMapping("/{routeId}/point/{pointOrder}/status")
    public ResponseEntity<Map<String, Object>> updatePointStatus(
            @PathVariable Long routeId,
            @PathVariable Integer pointOrder,
            @RequestParam String status) {
        routeService.updatePointStatus(routeId, pointOrder, status);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取导航信息")
    @PostMapping("/navigation")
    public ResponseEntity<Map<String, Object>> getNavigation(@RequestBody NavigationRequest request) {
        NavigationResult result = routeService.getNavigation(request);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "根据实时路况重新优化")
    @PostMapping("/{routeId}/reoptimize")
    public ResponseEntity<Map<String, Object>> reoptimizeByTraffic(@PathVariable Long routeId) {
        RoutePlanResult result = routeService.reoptimizeByTraffic(routeId);
        return ResponseEntity.ok(success(result));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}