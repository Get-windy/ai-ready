package cn.aiedge.erp.finance.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.aiedge.erp.finance.analytics.service.PartnerLedgerService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 往来余额表（分析 → 财务分析 → 往来余额表，菜单 80459）。
 *
 * <p>与既有 {@code /api/erp/finance/partner-balance/page}（旧口径：仅四个期末余额 + 净额）<b>互不影响</b>：
 * 本控制器按对标 18 列（3 固定 + 5 组 × 3 叶子）给出四象限期初/本期/期末，并提供清账与清账历史。</p>
 */
@Tag(name = "往来余额表（分析模块）")
@RestController
@RequestMapping("/api/erp/finance/analytics/partner-balance")
@RequiredArgsConstructor
public class PartnerLedgerController {

    private final PartnerLedgerService partnerLedgerService;

    @Operation(summary = "往来余额表分页（四象限期初/本期/期末 + 往来净额）",
            description = "期末 = 期初 + 本期；往来合计 = 应收 + 预付 − 预收 − 应付；含合计行 summary")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(AnalyticsQuery query) {
        return ApiResponse.ok(partnerLedgerService.page(query));
    }

    @Operation(summary = "行级「对账」：该结算单位四象限来源流水")
    @GetMapping("/detail")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> detail(AnalyticsQuery query) {
        return ApiResponse.ok(partnerLedgerService.detail(query));
    }

    @Operation(summary = "行级「清账」：应收与应付对冲（经会计凭证，只支持应收/应付账款）")
    @PostMapping("/reconcile")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> reconcile(
            @Parameter(description = "结算单位名称") @RequestParam String partnerName,
            @Parameter(description = "清账金额") @RequestParam BigDecimal amount,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        return ApiResponse.ok(partnerLedgerService.reconcile(partnerName, amount, remark));
    }

    @Operation(summary = "工具栏「清账历史」")
    @GetMapping("/reconcile-history")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> reconcileHistory(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ApiResponse.ok(partnerLedgerService.reconcileHistory(page, size));
    }
}
