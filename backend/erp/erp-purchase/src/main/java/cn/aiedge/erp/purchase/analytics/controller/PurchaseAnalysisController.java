package cn.aiedge.erp.purchase.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.analytics.dto.PurchaseAnalysisQueryDTO;
import cn.aiedge.erp.purchase.analytics.service.PurchaseAnalysisReportService;
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
 * 采购分析控制器（分析 → 采销分析 → 采购分析，菜单 80421）
 *
 * <p>单入口三视图 Tab，统一挂 {@code /api/erp/purchase/analytics}：</p>
 * <ul>
 *   <li>按时间（tab=time，granularity=day|week|month）—— 日期 + 采订/采购/退货三组量额</li>
 *   <li>按商品（tab=product）—— 每商品一行，28 列口径</li>
 *   <li>按供应商（tab=supplier）—— 每供应商一行，17 列口径</li>
 * </ul>
 *
 * <p>响应体统一为 {@code {records,total,page,size,pages,summary}}，由 {@link ApiResponse} 包一层
 * （响应拦截器只认数字 code===200）。租户取当前登录会话，不信任前端传参。</p>
 */
@Tag(name = "采购分析报表")
@RestController
@RequestMapping("/api/erp/purchase/analytics")
@RequiredArgsConstructor
public class PurchaseAnalysisController {

    private final PurchaseAnalysisReportService purchaseAnalysisReportService;

    @Operation(summary = "采购分析（按时间/按商品/按供应商）",
            description = "采订/采购入库/采购退货三段量额 + 实采金额、退货率派生列，含合计行 summary")
    @SaCheckPermission("purchase:analytics:list")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(PurchaseAnalysisQueryDTO query) {
        return ApiResponse.ok(purchaseAnalysisReportService.analysis(query));
    }
}
