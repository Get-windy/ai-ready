package cn.aiedge.dms.dashboard.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.dashboard.dto.DashboardStatsVO;
import cn.aiedge.dms.dashboard.dto.DistributionItemVO;
import cn.aiedge.dms.dashboard.dto.TaskSummaryItemVO;
import cn.aiedge.dms.dashboard.dto.TopRiderVO;
import cn.aiedge.dms.dashboard.dto.TrendPointVO;
import cn.aiedge.dms.dashboard.service.DashboardService;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 配送仪表盘控制器
 *
 * 运营看板数据源：KPI 聚合 / 单量时效趋势 / 任务分布 / 配送员绩效 / 人车绑定与核验预警待办。
 * 全部指标由数据库聚合产出（{@link DashboardService}），页面侧不做二次汇总。
 * 时间范围口径：今日 / 昨日 / 近 7 日 / 近 30 日 / 本月 / 自定义（左闭右开）。
 *
 * @author AI-Ready Team
 */
@Tag(name = "配送仪表盘")
@RestController
@RequestMapping("/api/dms/dashboard")
@RequiredArgsConstructor
@SaCheckLogin
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "KPI 聚合（运力规模 / 单量结构 / 时效 / 金额 / 待办计数）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsVO> stats(
            @Parameter(description = "时间范围：today/yesterday/last7/last30/month/custom")
            @RequestParam(defaultValue = "today") String range,
            @Parameter(description = "自定义开始日期（range=custom 时必填）")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "自定义结束日期")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "运力渠道ID") @RequestParam(required = false) Long channelId,
            @Parameter(description = "订单类型：1-销售配送 2-调拨 3-退货")
            @RequestParam(required = false) Integer orderType) {
        return ApiResponse.success(dashboardService.getStats(range, startDate, endDate, channelId, orderType));
    }

    @Operation(summary = "活跃人车绑定（绑定中，分页）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/active-bindings")
    public ApiResponse<IPage<DmsRiderVehicleBinding>> activeBindings(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.success(dashboardService.pageActiveBindings(page, size));
    }

    @Operation(summary = "人车核验预警（默认未处理，分页）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/pending-alerts")
    public ApiResponse<IPage<DmsVerificationAlert>> pendingAlerts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "处理状态：0-未处理 1-已确认 2-已忽略 3-已处理，缺省=未处理")
            @RequestParam(required = false) Integer handleStatus) {
        return ApiResponse.success(dashboardService.pagePendingAlerts(page, size, handleStatus));
    }

    @Operation(summary = "任务状态分布（9 态全量，区间无数据补 0）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/task-summary")
    public ApiResponse<List<TaskSummaryItemVO>> taskSummary(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Integer orderType) {
        return ApiResponse.success(dashboardService.getTaskSummary(range, startDate, endDate, channelId, orderType));
    }

    @Operation(summary = "单量时效趋势（按日，缺失日期补 0）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVO>> trend(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Integer orderType,
            @Parameter(description = "趋势天数（缺省按时间范围推导：今日/昨日=7，近30日/本月=30）")
            @RequestParam(required = false) Integer days) {
        return ApiResponse.success(dashboardService.getTrend(range, startDate, endDate, channelId, orderType, days));
    }

    @Operation(summary = "配送员绩效 Top（单量/完成量/准时率/时长/评分）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/top-riders")
    public ApiResponse<List<TopRiderVO>> topRiders(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Integer orderType,
            @Parameter(description = "返回条数，默认 10，上限 50")
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(dashboardService.getTopRiders(range, startDate, endDate, channelId, orderType, limit));
    }

    @Operation(summary = "任务分布（by=channel 按渠道 / by=orderType 按订单类型）")
    @SaCheckPermission("dms:dashboard:view")
    @GetMapping("/distribution")
    public ApiResponse<List<DistributionItemVO>> distribution(
            @Parameter(description = "分布维度：channel / orderType")
            @RequestParam(defaultValue = "orderType") String by,
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Integer orderType) {
        return ApiResponse.success(
                dashboardService.getDistribution(by, range, startDate, endDate, channelId, orderType));
    }
}
