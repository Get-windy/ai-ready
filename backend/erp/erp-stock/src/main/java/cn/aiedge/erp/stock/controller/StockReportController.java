package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.InvSummaryVO;
import cn.aiedge.erp.stock.dto.PurchasePrepAnalysisVO;
import cn.aiedge.erp.stock.dto.StockFlowVO;
import cn.aiedge.erp.stock.service.StockReportService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 库存分析报表Controller（进销存汇总 / 库存变动流水 / 采购准备分析）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "库存分析报表", description = "进销存汇总、库存变动流水、采购准备分析接口")
@RestController
@RequestMapping("/api/erp/stock")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StockReportController {

    private final StockReportService stockReportService;

    @Operation(summary = "进销存汇总分页", description = "每商品+仓库一行：期初数量/金额、期间入库、期间出库、结存（金额按成本价）")
    @GetMapping("/inv-summary/page")
    public Result<IPage<InvSummaryVO>> invSummaryPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "期间开始日期(yyyy-MM-dd，含)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "期间结束日期(yyyy-MM-dd，含)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "商品编码/名称关键字") @RequestParam(required = false) String keyword) {
        return Result.ok(stockReportService.pageInvSummary(startDate, endDate, warehouseId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "库存变动流水分页", description = "每单据明细行一条变动：时间/单据/商品/仓库/变动数量(正负)/变动后结存/操作人")
    @GetMapping("/flow/page")
    public Result<IPage<StockFlowVO>> stockFlowPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "商品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "单据类型(PURCHASE_IN/SALE_OUT/TRANSFER_IN/TRANSFER_OUT/OVERFLOW_IN/DAMAGE_OUT/CHECK_ADJUST)") @RequestParam(required = false) String docType,
            @Parameter(description = "变动开始日期(yyyy-MM-dd，含)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "变动结束日期(yyyy-MM-dd，含)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(stockReportService.pageStockFlow(productId, warehouseId, docType, startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "采购准备分析", description = "预警商品数/缺货SKU数/在途采购/建议补货金额汇总")
    @GetMapping("/prep-analysis")
    public Result<PurchasePrepAnalysisVO> purchasePrepAnalysis(
            @Parameter(description = "仓库ID（可空，为空统计全部仓库）") @RequestParam(required = false) Long warehouseId) {
        return Result.ok(stockReportService.getPurchasePrepAnalysis(warehouseId));
    }
}
