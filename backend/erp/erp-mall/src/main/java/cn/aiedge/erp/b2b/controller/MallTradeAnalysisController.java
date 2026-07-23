package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.b2b.dto.TradeAnalysisDTO;
import cn.aiedge.erp.b2b.service.MallTradeAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城交易分析控制器
 */
@RestController
@RequestMapping("/api/erp/mall/admin")
@Tag(name = "商城交易分析", description = "商城订单交易分析(按日GMV/客单价/退款率/支付状态分布)")
@RequiredArgsConstructor
public class MallTradeAnalysisController {

    private final MallTradeAnalysisService tradeAnalysisService;

    @GetMapping("/trade-analysis")
    @Operation(summary = "交易分析",
            description = "按日分组(订单数/GMV/客单价) + 汇总(总单数/GMV/客单价/退款单数/退款率) + 支付状态分布")
    public Result<TradeAnalysisDTO> tradeAnalysis(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.ok(tradeAnalysisService.tradeAnalysis(startDate, endDate));
    }
}
