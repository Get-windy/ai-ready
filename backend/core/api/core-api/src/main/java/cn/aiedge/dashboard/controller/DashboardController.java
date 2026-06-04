package cn.aiedge.dashboard.controller;

import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 仪表盘控制器
 * 提供工作台首页的 KPI 统计、趋势图、待办事项和库存预警数据
 */
@RestController
@RequestMapping("/api/dashboard")
@SaCheckLogin
@Tag(name = "仪表盘", description = "工作台首页数据接口")
public class DashboardController {

    /**
     * 获取仪表盘 KPI 统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取KPI统计数据")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("todaySales", Map.of(
            "value", 128650.00,
            "trend", 12.5,
            "trendType", "up"
        ));
        stats.put("todayPurchase", Map.of(
            "value", 85600.00,
            "trend", -3.2,
            "trendType", "down"
        ));
        stats.put("pendingApprovals", Map.of(
            "value", 7,
            "trendType", "warn"
        ));
        stats.put("stockAlerts", Map.of(
            "value", 4,
            "trend", -1,
            "trendType", "down"
        ));

        return Result.ok(stats);
    }

    /**
     * 获取销售趋势图数据
     */
    @GetMapping("/trend")
    @Operation(summary = "获取销售趋势图数据")
    public Result<Map<String, Object>> getTrend() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> categories = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            categories.add(today.minusDays(i).format(fmt));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("series", List.of(
            Map.of("name", "销售额", "data", List.of(45200, 53800, 61200, 58700, 72300, 68900, 82500)),
            Map.of("name", "采购额", "data", List.of(32100, 29800, 35600, 41200, 38500, 42900, 39800)),
            Map.of("name", "利润", "data", List.of(13100, 16500, 18300, 17200, 21800, 19500, 24700))
        ));

        return Result.ok(result);
    }

    /**
     * 获取待办事项列表
     */
    @GetMapping("/todos")
    @Operation(summary = "获取待办事项列表")
    public Result<List<Map<String, Object>>> getTodos() {
        List<Map<String, Object>> todos = new ArrayList<>();

        Map<String, Object> todo1 = new LinkedHashMap<>();
        todo1.put("id", 1);
        todo1.put("title", "采购订单 PO-2026-0056 待审批");
        todo1.put("time", "10 分钟前");
        todo1.put("type", "approval");
        todos.add(todo1);

        Map<String, Object> todo2 = new LinkedHashMap<>();
        todo2.put("id", 2);
        todo2.put("title", "销售合同 CT-2026-0032 待审核");
        todo2.put("time", "30 分钟前");
        todo2.put("type", "approval");
        todos.add(todo2);

        Map<String, Object> todo3 = new LinkedHashMap<>();
        todo3.put("id", 3);
        todo3.put("title", "库存预警：A类物料螺丝(MC-001)低于安全库存");
        todo3.put("time", "1 小时前");
        todo3.put("type", "alert");
        todos.add(todo3);

        Map<String, Object> todo4 = new LinkedHashMap<>();
        todo4.put("id", 4);
        todo4.put("title", "库存预警：B类物料轴承(MC-008)低于安全库存");
        todo4.put("time", "2 小时前");
        todo4.put("type", "alert");
        todos.add(todo4);

        Map<String, Object> todo5 = new LinkedHashMap<>();
        todo5.put("id", 5);
        todo5.put("title", "应收账款-XX公司 已逾期3天，请跟进");
        todo5.put("time", "昨天");
        todo5.put("type", "info");
        todos.add(todo5);

        Map<String, Object> todo6 = new LinkedHashMap<>();
        todo6.put("id", 6);
        todo6.put("title", "本月财务报表待生成");
        todo6.put("time", "昨天");
        todo6.put("type", "info");
        todos.add(todo6);

        return Result.ok(todos);
    }

    /**
     * 获取库存预警列表
     */
    @GetMapping("/alerts")
    @Operation(summary = "获取库存预警列表")
    public Result<List<Map<String, Object>>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        Map<String, Object> alert1 = new LinkedHashMap<>();
        alert1.put("id", 1);
        alert1.put("code", "MC-001");
        alert1.put("name", "不锈钢螺丝 M8×30");
        alert1.put("current", 15);
        alert1.put("safe", 100);
        alert1.put("level", "high");
        alerts.add(alert1);

        Map<String, Object> alert2 = new LinkedHashMap<>();
        alert2.put("id", 2);
        alert2.put("code", "MC-008");
        alert2.put("name", "深沟球轴承 6205");
        alert2.put("current", 8);
        alert2.put("safe", 50);
        alert2.put("level", "high");
        alerts.add(alert2);

        Map<String, Object> alert3 = new LinkedHashMap<>();
        alert3.put("id", 3);
        alert3.put("code", "MC-015");
        alert3.put("name", "铜管 Φ12×1.5");
        alert3.put("current", 42);
        alert3.put("safe", 80);
        alert3.put("level", "low");
        alerts.add(alert3);

        Map<String, Object> alert4 = new LinkedHashMap<>();
        alert4.put("id", 4);
        alert4.put("code", "MC-023");
        alert4.put("name", "密封圈 O型 Φ50");
        alert4.put("current", 35);
        alert4.put("safe", 60);
        alert4.put("level", "low");
        alerts.add(alert4);

        return Result.ok(alerts);
    }
}
