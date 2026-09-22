package cn.aiedge.erp.finance.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.aiedge.erp.finance.analytics.service.InvoiceStatsService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 发票统计（分析 → 财务分析 → 发票统计，菜单 80455）。
 *
 * <p>增值税进销项月度台账，17 列（3 固定 + 销项开票 7 叶子 + 取得进项专票 7 叶子）。
 * 与既有 {@code /api/erp/invoice/date-range}（按日开票 + 收款进度）<b>互不影响</b>。</p>
 */
@Tag(name = "发票统计（分析模块）")
@RestController
@RequestMapping("/api/erp/finance/analytics/invoice-stats")
@RequiredArgsConstructor
public class InvoiceStatsController {

    private final InvoiceStatsService invoiceStatsService;

    @Operation(summary = "发票统计分页（按月进销项台账）",
            description = "销项/进项各 7 列（正负数张数与金额、净开票金额、价税合计、税额）+ 抵扣后应交税额；含合计行 summary")
    @SaCheckPermission("finance:analytics-invoice-stats:list")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(AnalyticsQuery query) {
        return ApiResponse.ok(invoiceStatsService.page(query));
    }
}
