package cn.aiedge.erp.stock.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.stock.analytics.dto.InventoryAnalysisQueryDTO;
import cn.aiedge.erp.stock.analytics.service.InventoryAnalysisReportService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 进销存分析控制器（分析 → 仓配分析 → 进销存分析，菜单 80433）
 *
 * <p>单入口三视图 Tab，统一挂 {@code /api/erp/stock/analytics}：</p>
 * <ul>
 *   <li>按商品（tab=product）—— 期初 + 五类入库 − 五类出库 = 期末（小单位数量 + 金额双口径，38 列）</li>
 *   <li>仓库调拨分析（tab=transferWarehouse）—— 出库仓 → 入库仓，调拨/成本/差异金额</li>
 *   <li>商品调拨分析（tab=transferProduct）—— 商品维度调拨汇总（含换算关系/换算结果）</li>
 * </ul>
 *
 * <p>与既有 {@code /erp/stock/inv-summary/page}（库存明细/进销存旧口径）互不影响：
 * 本控制器自带库存变动 UNION，不修改 {@code StockReportMapper}。</p>
 */
@Tag(name = "进销存分析报表")
@RestController
@RequestMapping("/api/erp/stock/analytics")
@RequiredArgsConstructor
public class InventoryAnalysisController {

    private final InventoryAnalysisReportService inventoryAnalysisReportService;

    @Operation(summary = "进销存分析（按商品/仓库调拨分析/商品调拨分析）",
            description = "按商品：期初+五类入库−五类出库=期末（小单位数量/金额）；调拨视图：调拨/成本/差异金额，含合计行 summary")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(InventoryAnalysisQueryDTO query) {
        return ApiResponse.ok(inventoryAnalysisReportService.analysis(query));
    }
}
