package cn.aiedge.erp.finance.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.aiedge.erp.finance.analytics.service.CollectionStatsAnalyticsService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 回款统计（分析 → 提成分析 → 回款统计，菜单 80442）。
 *
 * <p>与既有 {@code /api/erp/finance/collection-stats}（旧口径：erp_receipt 单表 + 支付方式分桶）<b>互不影响</b>：
 * 本控制器按对标两视图（按职员 / 按部门）做三口径（收款 / 预收款 / 预订货收款）归集。</p>
 *
 * <p>响应体统一 {@code {records,total,page,size,pages,summary}}，由 {@link ApiResponse} 包一层。</p>
 */
@Tag(name = "回款统计（分析模块）")
@RestController
@RequestMapping("/api/erp/finance/analytics/collection-stats")
@RequiredArgsConstructor
public class CollectionStatsAnalyticsController {

    private final CollectionStatsAnalyticsService collectionStatsAnalyticsService;

    @Operation(summary = "回款统计分页（tab=staff 按职员 / dept 按部门）",
            description = "收款金额 + 预收款金额 + 预订货收款金额 = 回款总金额；含合计行 summary")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(AnalyticsQuery query) {
        return ApiResponse.ok(collectionStatsAnalyticsService.page(query));
    }

    @Operation(summary = "回款统计行级明细钻取", description = "该职员/部门下的收款单 / 预收款单 / 预订货单流水")
    @GetMapping("/detail")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> detail(AnalyticsQuery query) {
        return ApiResponse.ok(collectionStatsAnalyticsService.detail(query));
    }
}
