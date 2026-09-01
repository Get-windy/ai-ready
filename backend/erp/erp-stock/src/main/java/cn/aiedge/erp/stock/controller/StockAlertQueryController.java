package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import cn.aiedge.erp.stock.service.StockAlertQueryService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警查询Controller —— 触发上下限预警商品清单查询（只读）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "预警查询", description = "触发库存上下限预警的商品清单查询")
@RestController
@RequestMapping("/api/erp/stock-alert")
@RequiredArgsConstructor
public class StockAlertQueryController {

    private final StockAlertQueryService stockAlertQueryService;

    @Operation(summary = "分页查询预警清单")
    @GetMapping("/page")
    public Result<IPage<StockAlertQueryVO>> page(
            @Parameter(description = "仓库ID（空=全部仓库）") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "商品名称/货号/条码") @RequestParam(required = false) String productKeyword,
            @Parameter(description = "品牌") @RequestParam(required = false) String brand,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "预警类型：LOW_STOCK=下限预警，OVER_STOCK=上限预警，空/ALL=全部") @RequestParam(required = false) String alertType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(stockAlertQueryService.page(warehouseId, productKeyword, brand, categoryId, alertType, pageNum, pageSize));
    }
}
