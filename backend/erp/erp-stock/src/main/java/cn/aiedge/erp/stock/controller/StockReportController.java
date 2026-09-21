package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.InvSummaryVO;
import cn.aiedge.erp.stock.dto.PurchasePrepAnalysisVO;
import cn.aiedge.erp.stock.dto.ShortageReplenishVO;
import cn.aiedge.erp.stock.dto.SmartReplenishVO;
import cn.aiedge.erp.stock.dto.StockAlertReplenishVO;
import cn.aiedge.erp.stock.dto.StockFlowVO;
import cn.aiedge.erp.stock.service.ShortageReplenishService;
import cn.aiedge.erp.stock.service.SmartReplenishService;
import cn.aiedge.erp.stock.service.StockAlertReplenishService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import cn.dev33.satoken.annotation.SaCheckPermission;

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
    private final StockAlertReplenishService stockAlertReplenishService;
    private final ShortageReplenishService shortageReplenishService;
    private final SmartReplenishService smartReplenishService;

    @Operation(summary = "进销存汇总分页", description = "每商品+仓库一行：期初数量/金额、期间入库、期间出库、结存（金额按成本价）")
    @SaCheckPermission("stock:list")
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
    @SaCheckPermission("stock:list")
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
    @SaCheckPermission("stock:view")
    @GetMapping("/prep-analysis")
    public Result<PurchasePrepAnalysisVO> purchasePrepAnalysis(
            @Parameter(description = "仓库ID（可空，为空统计全部仓库）") @RequestParam(required = false) Long warehouseId) {
        return Result.ok(stockReportService.getPurchasePrepAnalysis(warehouseId));
    }

    @Operation(summary = "库存预警补货分页", description = "按商品×仓库一行：仓库/商品档案/预警类型/缺货数量/待发货/账面库存/待收货/最近采购")
    @SaCheckPermission("stock:list")
    @GetMapping("/alert-replenish/page")
    public Result<IPage<StockAlertReplenishVO>> alertReplenishPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "商品名称/编码/货号") @RequestParam(required = false) String keyword,
            @Parameter(description = "品牌") @RequestParam(required = false) String brand,
            @Parameter(description = "所属供应商(模糊)") @RequestParam(required = false) String supplierName,
            @Parameter(description = "备注") @RequestParam(required = false) String remark,
            @Parameter(description = "只显示下限预警商品") @RequestParam(required = false) Boolean onlyLowStock,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) Long categoryId) {
        return Result.ok(stockAlertReplenishService.page(warehouseId, keyword, brand, supplierName,
                remark, onlyLowStock, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "缺货补货分页", description = "按商品×仓库一行：订单数量/价税合计/已发货/待发货/待收货/账面库存/缺货数量")
    @SaCheckPermission("stock:list")
    @GetMapping("/shortage-replenish/page")
    public Result<IPage<ShortageReplenishVO>> shortageReplenishPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "单据状态（空=有效状态 2,3,4,5）") @RequestParam(required = false) Integer orderStatus,
            @Parameter(description = "开始日期(yyyy-MM-dd，含)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期(yyyy-MM-dd，含)") @RequestParam(required = false) String endDate,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "经手人ID") @RequestParam(required = false) Long salesmanId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "订单来源") @RequestParam(required = false) Integer orderSource,
            @Parameter(description = "商品名称/编码/货号") @RequestParam(required = false) String productKeyword,
            @Parameter(description = "供应商(模糊)") @RequestParam(required = false) String supplierName,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "缺货数量口径(1=待发货-账面库存 2=待发货-待收货-账面库存)") @RequestParam(required = false) Integer shortageMode,
            @Parameter(description = "仅显示缺货商品") @RequestParam(required = false) Boolean onlyShortage) {
        return Result.ok(shortageReplenishService.page(orderStatus, startDate, endDate, customerId,
                salesmanId, warehouseId, orderSource, productKeyword, supplierName, categoryId,
                shortageMode, onlyShortage, pageNum, pageSize));
    }

    @Operation(summary = "智能补货分页", description = "每商品一行：商品档案/销售数量/销售金额/采购金额/日均销量/待收货/待发货/采购数量/账面库存/换算结果/计划采购数量/可用库存/最近销售/最近进货")
    @SaCheckPermission("stock:list")
    @GetMapping("/smart-replenish/page")
    public Result<IPage<SmartReplenishVO>> smartReplenishPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "销售日期开始(yyyy-MM-dd，含)") @RequestParam(required = false) String startDate,
            @Parameter(description = "销售日期结束(yyyy-MM-dd，含)") @RequestParam(required = false) String endDate,
            @Parameter(description = "备货天数") @RequestParam(required = false) Integer stockDays,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "商品名称/编码/货号/条码") @RequestParam(required = false) String productKeyword,
            @Parameter(description = "供货商(模糊)") @RequestParam(required = false) String supplierName,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "计划采购数量下限") @RequestParam(required = false) BigDecimal minPlanQty) {
        return Result.ok(smartReplenishService.page(startDate, endDate, stockDays, warehouseId,
                productKeyword, supplierName, categoryId, minPlanQty, pageNum, pageSize));
    }
}
