package cn.aiedge.dashboard.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import cn.aiedge.erp.stock.service.StockService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘控制器
 * 提供工作台首页的 KPI 统计、趋势图、待办事项和库存预警数据
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "工作台首页数据接口")
public class DashboardController {

    private final SaleOrderMapper saleOrderMapper;
    private final PurchaseOrderService purchaseOrderService;
    private final StockService stockService;

    /**
     * 获取仪表盘 KPI 统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取KPI统计数据")
    public Result<Map<String, Object>> getStats() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            // 今日销售总额（从明细汇总）
            BigDecimal todaySales = saleOrderMapper.sumTodaySalesAmount(tenantId);
            BigDecimal yesterdaySales = saleOrderMapper.sumMonthSalesAmount(tenantId)
                .subtract(todaySales); // 近似：用本月减去今日作为对比基准
            double salesTrend = yesterdaySales.compareTo(BigDecimal.ZERO) > 0
                ? todaySales.subtract(yesterdaySales).divide(yesterdaySales, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

            stats.put("todaySales", Map.of(
                "value", todaySales.doubleValue(),
                "trend", Math.round(salesTrend * 10.0) / 10.0,
                "trendType", salesTrend >= 0 ? "up" : "down"
            ));
        } catch (Exception e) {
            log.warn("查询销售数据失败", e);
            stats.put("todaySales", Map.of("value", 0, "trend", 0, "trendType", "up"));
        }

        try {
            // 采购统计（使用订单中心模块）
            LocalDate now = LocalDate.now();
            var monthStart = now.withDayOfMonth(1).atStartOfDay();
            var monthEnd = now.plusDays(1).atStartOfDay();
            var purchaseStats = purchaseOrderService.getPurchaseStatistics(tenantId, monthStart, monthEnd);
            Object poAmount = purchaseStats.getTotalAmount();
            double poValue = poAmount instanceof BigDecimal ? ((BigDecimal) poAmount).doubleValue() : 0.0;

            stats.put("todayPurchase", Map.of(
                "value", poValue,
                "trend", 0.0,
                "trendType", "up"
            ));
        } catch (Exception e) {
            log.warn("查询采购数据失败", e);
            stats.put("todayPurchase", Map.of("value", 0, "trend", 0, "trendType", "up"));
        }

        try {
            // 待审批数量（销售订单 + 采购订单）
            int salePending = saleOrderMapper.countPendingApproval(tenantId);
            stats.put("pendingApprovals", Map.of(
                "value", salePending,
                "trendType", salePending > 0 ? "warn" : "safe"
            ));
        } catch (Exception e) {
            log.warn("查询待审批数据失败", e);
            stats.put("pendingApprovals", Map.of("value", 0, "trendType", "safe"));
        }

        try {
            // 库存预警数量
            int alertCount = stockService.checkStockAlert().size();
            stats.put("stockAlerts", Map.of(
                "value", alertCount,
                "trend", 0,
                "trendType", alertCount > 0 ? "warn" : "safe"
            ));
        } catch (Exception e) {
            log.warn("查询库存预警失败", e);
            stats.put("stockAlerts", Map.of("value", 0, "trend", 0, "trendType", "safe"));
        }

        return Result.ok(stats);
    }

    /**
     * 获取销售趋势图数据
     */
    @GetMapping("/trend")
    @Operation(summary = "获取销售趋势图数据")
    public Result<Map<String, Object>> getTrend() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 过去7天日期列表
        List<String> categories = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            categories.add(today.minusDays(i).format(fmt));
        }

        // 销售额和采购额趋势
        List<BigDecimal> salesData = new ArrayList<>(Collections.nCopies(7, BigDecimal.ZERO));
        List<BigDecimal> purchaseData = new ArrayList<>(Collections.nCopies(7, BigDecimal.ZERO));
        LocalDate start = today.minusDays(6);

        try {
            String startDate = today.minusDays(6).toString();
            String endDate = today.plusDays(1).toString(); // 排除当天之后
            var dailySales = saleOrderMapper.selectDailySalesTrend(tenantId, startDate, endDate);
            for (var row : dailySales) {
                String day = row.get("day").toString();
                LocalDate d = LocalDate.parse(day);
                int idx = 6 - (int) today.datesUntil(d.plusDays(1)).count() + 1;
                // 简化：从日期差值计算索引
                long diff = d.toEpochDay() - today.minusDays(6).toEpochDay();
                if (diff >= 0 && diff < 7) {
                    salesData.set((int) diff, (BigDecimal) row.get("amount"));
                }
            }
        } catch (Exception e) {
            log.warn("查询销售趋势失败", e);
        }

        try {
            // 采购趋势（简化：从采购统计获取）
            var purchaseStats = purchaseOrderService.getPurchaseStatistics(tenantId, start.atStartOfDay(), today.plusDays(1).atStartOfDay());
            Object poAmount = purchaseStats.getTotalAmount();
            double totalAmt = poAmount instanceof BigDecimal ? ((BigDecimal) poAmount).doubleValue() : 0.0;
            // 每日平均，作为近似趋势
            double dailyAvg = totalAmt / 7.0;
            for (int i = 0; i < 7; i++) {
                purchaseData.set(i, BigDecimal.valueOf(dailyAvg));
            }
        } catch (Exception e) {
            log.warn("查询采购趋势失败", e);
        }

        // 利润 ≈ 销售额 - 采购额
        List<BigDecimal> profitData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            profitData.add(salesData.get(i).subtract(purchaseData.get(i)));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("series", List.of(
            Map.of("name", "销售额", "data", salesData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList())),
            Map.of("name", "采购额", "data", purchaseData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList())),
            Map.of("name", "利润", "data", profitData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()))
        ));

        return Result.ok(result);
    }

    /**
     * 获取待办事项列表
     */
    @GetMapping("/todos")
    @Operation(summary = "获取待办事项列表")
    public Result<List<Map<String, Object>>> getTodos() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        List<Map<String, Object>> todos = new ArrayList<>();

        try {
            // 待审批销售订单
            var pendingOrders = saleOrderMapper.selectPendingOrders(tenantId);
            for (var order : pendingOrders) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", order.getId());
                item.put("title", "销售订单 " + order.getOrderNo() + " 待审批");
                item.put("time", "待审批");
                item.put("type", "approval");
                todos.add(item);
            }
        } catch (Exception e) {
            log.warn("查询待审批销售订单失败", e);
        }

        try {
            // 库存预警
            var alerts = stockService.checkStockAlert();
            for (var alert : alerts) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", alert.getProductId());
                item.put("title", "库存预警：" + alert.getProductName() + " 库存不足（当前:" + alert.getAvailableQuantity() + "）");
                item.put("time", "实时");
                item.put("type", "alert");
                todos.add(item);
            }
        } catch (Exception e) {
            log.warn("查询库存预警失败", e);
        }

        return Result.ok(todos);
    }

    /**
     * 获取库存预警列表
     */
    @GetMapping("/alerts")
    @Operation(summary = "获取库存预警列表")
    public Result<List<Map<String, Object>>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        try {
            var stockAlerts = stockService.checkStockAlert();
            for (var alert : stockAlerts) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", alert.getId());
                item.put("code", alert.getProductCode());
                item.put("name", alert.getProductName());
                item.put("current", alert.getAvailableQuantity());
                item.put("safe", alert.getSafetyStock() != null ? alert.getSafetyStock() : 0);
                item.put("level", alert.getAvailableQuantity() != null && alert.getAvailableQuantity().compareTo(
    alert.getSafetyStock() != null ? alert.getSafetyStock().multiply(new java.math.BigDecimal("0.5")) : new java.math.BigDecimal("10")) <= 0 ? "high" : "low");
                alerts.add(item);
            }
        } catch (Exception e) {
            log.warn("查询库存预警列表失败", e);
        }

        return Result.ok(alerts);
    }
}
