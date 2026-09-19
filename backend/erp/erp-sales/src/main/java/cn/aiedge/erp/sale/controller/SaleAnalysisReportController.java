package cn.aiedge.erp.sale.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.dto.SaleAnalysisReportQueryDTO;
import cn.aiedge.erp.sale.service.SaleAnalysisReportService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 销售分析组报表控制器（分析 → 采销分析）
 *
 * <p>覆盖四个页面的维度聚合取数，统一挂在 {@code /api/erp/sale/analysis/**} 前缀下：</p>
 * <ul>
 *   <li>销售业绩（80411）：/sales-performance/page —— 按时间 / 按职员</li>
 *   <li>销售分析（80413）：/sales-analysis/page —— 按时间/商品/品牌/客户/区域/仓库/职员/来源</li>
 *   <li>销售履约分析（80414）：/sales-fulfillment/page —— 按单据 / 按客户</li>
 *   <li>销售欠款分析（80415）：/sales-debt/page —— 按职员 / 按客户 / 按区域</li>
 * </ul>
 *
 * <p>响应体统一为 {@code {records,total,page,size,pages,summary}}，由 {@link ApiResponse} 包一层
 * （响应拦截器只认数字 code===200）。租户取当前登录会话，不信任前端传参。</p>
 */
@Tag(name = "销售分析报表")
@RestController
@RequestMapping("/api/erp/sale/analysis")
@RequiredArgsConstructor
public class SaleAnalysisReportController {

    private final SaleAnalysisReportService saleAnalysisReportService;

    @Operation(summary = "销售业绩（按时间/按职员）",
            description = "漏斗口径：拓客/拜访/订货/销售/退货/回款/利润，含合计行 summary")
    @GetMapping("/sales-performance/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> salesPerformance(SaleAnalysisReportQueryDTO query) {
        return ApiResponse.ok(saleAnalysisReportService.salesPerformance(query));
    }

    @Operation(summary = "销售分析（8 维度量本利）",
            description = "按时间/商品/品牌/客户/区域/仓库/职员/来源聚合：数量/金额/退货/收入/成本/毛利/费用/利润，含合计行 summary")
    @GetMapping("/sales-analysis/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> salesAnalysis(SaleAnalysisReportQueryDTO query) {
        return ApiResponse.ok(saleAnalysisReportService.salesAnalysis(query));
    }

    @Operation(summary = "销售履约分析（按单据/按客户）",
            description = "发货履约率 + 订单价税合计/发货/未发/已结/未结五金额口径，含合计行 summary")
    @GetMapping("/sales-fulfillment/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> salesFulfillment(SaleAnalysisReportQueryDTO query) {
        return ApiResponse.ok(saleAnalysisReportService.salesFulfillment(query));
    }

    @Operation(summary = "销售欠款分析（按职员/按客户/按区域）",
            description = "欠款滚动（此前欠款/本期新增/本期收款/结算优惠/欠款余额）+ 超期账龄五档 + 信用额度，含合计行 summary")
    @GetMapping("/sales-debt/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> salesDebt(SaleAnalysisReportQueryDTO query) {
        return ApiResponse.ok(saleAnalysisReportService.salesDebt(query));
    }
}
