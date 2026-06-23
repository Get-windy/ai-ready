package cn.aiedge.erp.pricing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.pricing.entity.ProductGradePrice;
import cn.aiedge.erp.pricing.service.IProductGradePriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品等级价格Controller - 产品详情页内的等级价格管理
 */
@Slf4j
@Tag(name = "产品等级价格管理")
@RestController
@RequestMapping("/api/erp/product-grade-price")
@RequiredArgsConstructor
public class ProductGradePriceController {

    private final IProductGradePriceService gradePriceService;

    @Operation(summary = "获取某产品的等级价格列表")
    @GetMapping("/by-product/{productId}")
    public Result<List<ProductGradePrice>> getByProduct(@PathVariable Long productId) {
        return Result.ok(gradePriceService.getByProductId(productId));
    }

    @Operation(summary = "批量保存产品等级价格(全量覆盖)")
    @PostMapping("/batch-save")
    public Result<Boolean> batchSave(@RequestParam Long productId,
                                      @RequestBody List<ProductGradePrice> priceList) {
        gradePriceService.batchSave(productId, priceList);
        return Result.ok(true);
    }

    @Operation(summary = "新增单条等级价格")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductGradePrice price) {
        if (price.getIsActive() == null) price.setIsActive(1);
        boolean saved = gradePriceService.save(price);
        return Result.ok(saved);
    }

    @Operation(summary = "编辑等级价格")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductGradePrice price) {
        price.setId(id);
        return Result.ok(gradePriceService.updateById(price));
    }

    @Operation(summary = "删除等级价格")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        ProductGradePrice price = gradePriceService.getById(id);
        if (price != null) {
            gradePriceService.removeById(id);
        }
        return Result.ok(true);
    }
}