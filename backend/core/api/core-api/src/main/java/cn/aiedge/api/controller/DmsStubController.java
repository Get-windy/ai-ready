package cn.aiedge.api.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * DMS（配送管理系统）占位 Controller
 * DMS 为独立微服务，在主应用中返回空数据避免前端 404
 */
@Tag(name = "配送管理（占位）")
@RestController
@RequestMapping("/api/dms")
public class DmsStubController {

    @Operation(summary = "仪表盘待处理告警（占位）")
    @GetMapping("/dashboard/pending-alerts")
    public Result<List<?>> pendingAlerts() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "仪表盘任务汇总（占位）")
    @GetMapping("/dashboard/task-summary")
    public Result<Map<String, Object>> taskSummary() {
        return Result.ok(Map.of("total", 0, "pending", 0, "inProgress", 0, "completed", 0));
    }

    @Operation(summary = "仪表盘活跃绑定（占位）")
    @GetMapping("/dashboard/active-bindings")
    public Result<List<?>> activeBindings() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "仪表盘统计（占位）")
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(Map.of(
            "channels", 0, "riders", 0, "vehicles", 0,
            "todayOrders", 0, "completedOrders", 0
        ));
    }

    @Operation(summary = "渠道分页（占位）")
    @GetMapping("/channel/page")
    public Result<Map<String, Object>> channelPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "骑手分页（占位）")
    @GetMapping("/rider/page")
    public Result<Map<String, Object>> riderPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "车辆分页（占位）")
    @GetMapping("/vehicle/page")
    public Result<Map<String, Object>> vehiclePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "配送路线分页（占位）")
    @GetMapping("/route/page")
    public Result<Map<String, Object>> routePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "智能调度分页（占位）")
    @GetMapping("/dispatch/page")
    public Result<Map<String, Object>> dispatchPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "订单池分页（占位）")
    @GetMapping("/order-pool/page")
    public Result<Map<String, Object>> orderPoolPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "配送跟踪分页（占位）")
    @GetMapping("/tracking/page")
    public Result<Map<String, Object>> trackingPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "实名认证绑定分页（占位）")
    @GetMapping("/verification/binding/page")
    public Result<Map<String, Object>> verificationBindingPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "配送配置（占位）")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.ok(Map.of());
    }
}
