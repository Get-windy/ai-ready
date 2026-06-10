package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockBom;
import cn.aiedge.erp.stock.entity.StockBomItem;
import cn.aiedge.erp.stock.service.StockBomService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/bom")
@RequiredArgsConstructor
@Tag(name = "BOM物料清单管理", description = "物料清单创建、维护、启用/停用等操作")
public class StockBomController {

    private final StockBomService bomService;

    @lombok.Data
    public static class CreateBomRequest {
        private String bomName;
        private Long productId;
        private Integer bomType;
        private BigDecimal outputQuantity;
        private String remark;
        private List<StockBomItem> items;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询BOM清单")
    public Result<Page<StockBom>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(bomService.pageList(keyword, productId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取BOM详情")
    public Result<StockBom> getById(@PathVariable Long id) {
        StockBom bom = bomService.getById(id);
        if (bom == null) {
            throw BusinessException.notFound("BOM不存在");
        }
        return Result.ok(bom);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取BOM明细")
    public Result<List<StockBomItem>> getItems(@PathVariable Long id) {
        return Result.ok(bomService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建BOM")
    public Result<StockBom> create(@RequestBody CreateBomRequest request) {
        StockBom bom = new StockBom();
        bom.setBomName(request.getBomName());
        bom.setProductId(request.getProductId());
        bom.setBomType(request.getBomType());
        bom.setOutputQuantity(request.getOutputQuantity());
        bom.setRemark(request.getRemark());
        return Result.ok(bomService.createBom(bom, request.getItems()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新BOM")
    public Result<StockBom> update(@PathVariable Long id, @RequestBody CreateBomRequest request) {
        StockBom bom = new StockBom();
        bom.setBomName(request.getBomName());
        bom.setProductId(request.getProductId());
        bom.setBomType(request.getBomType());
        bom.setOutputQuantity(request.getOutputQuantity());
        bom.setRemark(request.getRemark());
        return Result.ok(bomService.updateBom(id, bom, request.getItems()));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用BOM")
    public Result<StockBom> enableBom(@PathVariable Long id) {
        return Result.ok(bomService.enableBom(id));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用BOM")
    public Result<StockBom> disableBom(@PathVariable Long id) {
        return Result.ok(bomService.disableBom(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除BOM")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(bomService.removeById(id));
    }
}
