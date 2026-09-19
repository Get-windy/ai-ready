package cn.aiedge.erp.sale.preorder.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.preorder.analytics.dto.PreOrderAnalysisQueryDTO;
import cn.aiedge.erp.sale.preorder.analytics.service.PreOrderAnalysisReportService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 预订货查询汇总控制器（分析 → 采销分析 → 销售分析 → 预订货查询，菜单 80419）
 *
 * <p>与既有单据 CRUD/明细分页端点（{@code /api/erp/sale/pre-order/**}）互补：
 * 本控制器只做<b>汇总分析</b>（按商品 / 按客户两个维度），供分析模块页面取数。</p>
 *
 * <p>取数红线：已取消、已驳回预订货单不参与统计（status &gt;= 0）。</p>
 */
@Tag(name = "预订货查询汇总分析")
@RestController
@RequestMapping("/api/erp/sale/pre-order/analysis")
@RequiredArgsConstructor
public class PreOrderAnalysisController {

    private final PreOrderAnalysisReportService preOrderAnalysisReportService;

    @Operation(summary = "预订货汇总（按商品/按客户）",
            description = "三量递进（预订/已订/已发）+ 赠品三口径 + 价格税口径 + 预订金三口径，含合计行 summary")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(PreOrderAnalysisQueryDTO query) {
        return ApiResponse.ok(preOrderAnalysisReportService.analysis(query));
    }
}
