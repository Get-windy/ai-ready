package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductRelated;
import cn.aiedge.erp.stock.service.ProductRelatedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "产品关联管理")
@RestController
@RequestMapping("/api/erp/product/related")
@RequiredArgsConstructor
public class ProductRelatedController {

    private final ProductRelatedService productRelatedService;

    @Operation(summary = "查询关联产品列表")
    @GetMapping("/{productId}")
    public Result<List<ProductRelated>> getByProduct(@PathVariable Long productId) {
        return Result.ok(productRelatedService.getByProductId(productId));
    }

    @Operation(summary = "新增关联")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductRelated related) {
        return Result.ok(productRelatedService.save(related));
    }

    @Operation(summary = "删除关联")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productRelatedService.removeById(id));
    }
}
