package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductRecommend;
import cn.aiedge.erp.stock.service.ProductRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐商品Controller
 */
@Slf4j
@Tag(name = "推荐商品管理")
@RestController
@RequestMapping("/api/erp/product-recommend")
@RequiredArgsConstructor
public class ProductRecommendController {

    private final ProductRecommendService productRecommendService;

    @Operation(summary = "获取商品的推荐列表")
    @GetMapping("/by-product/{productId}")
    public Result<List<ProductRecommend>> getByProductId(@PathVariable Long productId) {
        return Result.ok(productRecommendService.getByProductId(productId));
    }

    @Operation(summary = "批量保存推荐商品")
    @PostMapping("/batch-save")
    public Result<Boolean> batchSave(@RequestParam Long productId, @RequestBody List<ProductRecommend> recommends) {
        return Result.ok(productRecommendService.batchSave(productId, recommends));
    }

    @Operation(summary = "删除推荐")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productRecommendService.removeById(id));
    }
}
