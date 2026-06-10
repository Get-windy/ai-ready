package cn.aiedge.gateway.controller;

import cn.aiedge.gateway.service.RouteManagementService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网关管理控制器
 * 提供路由配置、监控统计等管理功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/gateway")
@SaCheckLogin
public class GatewayManagementController {

    @Autowired
    private RouteManagementService routeManagementService;

    /**
     * 获取所有路由定义
     */
    @GetMapping("/routes")
    public Result<List<RouteDefinition>> getAllRoutes() {
        return Result.success(routeManagementService.getAllRoutes());
    }

    /**
     * 添加路由
     */
    @PostMapping("/route")
    public Result<Boolean> addRoute(@RequestBody RouteDefinition routeDefinition) {
        return Result.success(routeManagementService.addRoute(routeDefinition));
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/route/{id}")
    public Result<Boolean> deleteRoute(@PathVariable String id) {
        return Result.success(routeManagementService.deleteRoute(id));
    }

    /**
     * 更新路由
     */
    @PutMapping("/route")
    public Result<Boolean> updateRoute(@RequestBody RouteDefinition routeDefinition) {
        return Result.success(routeManagementService.updateRoute(routeDefinition));
    }

    /**
     * 刷新路由
     */
    @PostMapping("/routes/refresh")
    public Result<Boolean> refreshRoutes() {
        routeManagementService.refreshRoutes();
        return Result.success(true);
    }

    /**
     * 获取网关统计信息
     */
    @GetMapping("/stats")
    public Result<Object> getGatewayStats() {
        return Result.success(routeManagementService.getGatewayStats());
    }
}
