package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品分类Controller - 左侧分类树管理
 */
@Slf4j
@Tag(name = "产品分类管理")
@RestController
@RequestMapping("/api/erp/product-category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<ProductCategory>> getTree() {
        return Result.ok(productCategoryService.getCategoryTree());
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable Long id) {
        return Result.ok(productCategoryService.getById(id));
    }

    @Operation(summary = "获取子分类列表")
    @GetMapping("/children/{parentId}")
    public Result<List<ProductCategory>> getChildren(@PathVariable Long parentId) {
        return Result.ok(productCategoryService.getChildren(parentId));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductCategory category) {
        return Result.ok(productCategoryService.createCategory(category));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        return Result.ok(productCategoryService.updateCategory(category));
    }

    @Operation(summary = "更新排序(拖拽)")
    @PutMapping("/{id}/sort")
    public Result<Boolean> updateSort(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        return Result.ok(productCategoryService.updateCategory(category));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        try {
            return Result.ok(productCategoryService.deleteCategory(id));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
