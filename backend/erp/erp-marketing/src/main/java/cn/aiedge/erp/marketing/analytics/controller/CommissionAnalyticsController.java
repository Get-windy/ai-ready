package cn.aiedge.erp.marketing.analytics.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.marketing.analytics.dto.CommissionAnalyticsQuery;
import cn.aiedge.erp.marketing.analytics.service.CommissionAnalyticsService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 业绩提成中心（分析 → 提成分析 → 业绩提成中心，菜单 80443）。
 *
 * <p>六视图：配送员 / 每月提成（年度矩阵）、提成构成、方案汇总提成、业绩概览、业绩明细，外加批量结算。
 * 与既有 {@code /api/erp/marketing/commission/record|rule|staff-summary} <b>互不影响</b>，仅新增路径。</p>
 */
@Tag(name = "业绩提成中心（分析模块）")
@RestController
@RequestMapping("/api/erp/marketing/commission/analytics")
@RequiredArgsConstructor
public class CommissionAnalyticsController {

    private final CommissionAnalyticsService commissionAnalyticsService;

    @Operation(summary = "配送员 / 每月提成（年度矩阵）",
            description = "人员 × 12 月提成矩阵；遵守结算顺序约束：某月未结算则其后月份不显示")
    @SaCheckPermission("marketing:commission-analytics:list")
    @GetMapping("/rider-matrix/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> riderMatrix(CommissionAnalyticsQuery query) {
        return ApiResponse.ok(commissionAnalyticsService.riderMatrix(query));
    }

    @Operation(summary = "提成构成（人员 × 方案，7 列）")
    @SaCheckPermission("marketing:commission-analytics:list")
    @GetMapping("/composition/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> composition(CommissionAnalyticsQuery query) {
        return ApiResponse.ok(commissionAnalyticsService.composition(query));
    }

    @Operation(summary = "方案汇总提成（按方案汇总，5 列）")
    @SaCheckPermission("marketing:commission-analytics:list")
    @GetMapping("/plan-summary/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> planSummary(CommissionAnalyticsQuery query) {
        return ApiResponse.ok(commissionAnalyticsService.planSummary(query));
    }

    @Operation(summary = "业绩概览（配送员维度配送指标，21 列）")
    @SaCheckPermission("marketing:commission-analytics:list")
    @GetMapping("/performance-overview/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> performanceOverview(CommissionAnalyticsQuery query) {
        return ApiResponse.ok(commissionAnalyticsService.performanceOverview(query));
    }

    @Operation(summary = "业绩明细（配送员 × 商品，21 列）")
    @SaCheckPermission("marketing:commission-analytics:list")
    @GetMapping("/performance-detail/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> performanceDetail(CommissionAnalyticsQuery query) {
        return ApiResponse.ok(commissionAnalyticsService.performanceDetail(query));
    }

    @Operation(summary = "批量结算", description = "把指定年月的未结提成置为已结算（PAID）；跨过未结月份会被拒绝")
    @SaCheckPermission("marketing:commission-analytics:settle")
    @PostMapping("/settle")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> settle(@RequestBody Map<String, Object> body) {
        if (body == null) {
            throw BusinessException.badRequest("结算参数不能为空");
        }
        Integer year = body.get("year") instanceof Number n ? n.intValue() : null;
        Integer month = body.get("month") instanceof Number n ? n.intValue() : null;
        @SuppressWarnings("unchecked")
        List<String> riderNames = body.get("riderNames") instanceof List<?> list
            ? list.stream().map(String::valueOf).toList() : null;
        String operator = StpUtil.isLogin() ? String.valueOf(StpUtil.getLoginId()) : null;
        return ApiResponse.ok(commissionAnalyticsService.settle(year, month, riderNames, operator));
    }

    @Operation(summary = "提成方案列表（工具栏「提成方案」只读入口）")
    @SaCheckPermission("marketing:commission-rule:list")
    @GetMapping("/plans")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> plans() {
        return ApiResponse.ok(commissionAnalyticsService.planList());
    }
}
