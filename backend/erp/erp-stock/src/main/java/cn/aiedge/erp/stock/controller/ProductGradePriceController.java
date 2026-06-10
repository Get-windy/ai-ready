package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductGradePrice;
import cn.aiedge.erp.stock.service.ProductGradePriceService;
import cn.aiedge.erp.stock.service.ProductService;
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

    private final ProductGradePriceService gradePriceService;
    private final ProductService productService;

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
        // 同步更新产品表标记
        Product product = new Product();
        product.setId(productId);
        product.setHasGradePrice(priceList != null && !priceList.isEmpty() ? 1 : 0);
        productService.updateById(product);
        return Result.ok(true);
    }

    @Operation(summary = "新增单条等级价格")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductGradePrice price) {
        if (price.getIsActive() == null) price.setIsActive(1);
        boolean saved = gradePriceService.save(price);
        if (saved) {
            // 更新产品标记
            Product product = new Product();
            product.setId(price.getProductId());
            product.setHasGradePrice(1);
            productService.updateById(product);
        }
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
            // 检查是否还有剩余等级价格
            long count = gradePriceService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductGradePrice>()
                            .eq(ProductGradePrice::getProductId, price.getProductId())
                            .eq(ProductGradePrice::getDeleted, 0));
            if (count == 0) {
                Product product = new Product();
                product.setId(price.getProductId());
                product.setHasGradePrice(0);
                productService.updateById(product);
            }
        }
        return Result.ok(true);
    }
}
