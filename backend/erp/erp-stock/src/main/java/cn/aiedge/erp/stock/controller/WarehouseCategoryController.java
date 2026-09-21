package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.WarehouseCategory;
import cn.aiedge.erp.stock.service.WarehouseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 仓库分类Controller（资料 → 仓库管理 → 仓库规划 左侧分类树）
 *
 * @author AI-Ready Team
 * @since 11.156.0
 */
@Slf4j
@Tag(name = "仓库分类", description = "仓库规划分类树")
@RestController
@RequestMapping("/api/erp/warehouse-category")
@RequiredArgsConstructor
public class WarehouseCategoryController {

    private final WarehouseCategoryService warehouseCategoryService;

    @Operation(summary = "仓库分类树")
    @SaCheckPermission("wms:category:list")
    @GetMapping("/tree")
    public Result<List<WarehouseCategory>> tree() {
        return Result.ok(warehouseCategoryService.getCategoryTree());
    }

    @Operation(summary = "根据ID查询分类")
    @SaCheckPermission("wms:category:detail")
    @GetMapping("/{id}")
    public Result<WarehouseCategory> getById(@PathVariable Long id) {
        WarehouseCategory category = warehouseCategoryService.getById(id);
        return category == null ? Result.fail("分类不存在") : Result.ok(category);
    }

    @Operation(summary = "新增分类")
    @SaCheckPermission("wms:category:create")
    @PostMapping
    public Result<WarehouseCategory> create(@RequestBody WarehouseCategory category) {
        return Result.ok(warehouseCategoryService.createCategory(category));
    }

    @Operation(summary = "修改分类")
    @SaCheckPermission("wms:category:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody WarehouseCategory category) {
        category.setId(id);
        return Result.ok(warehouseCategoryService.updateCategory(category));
    }

    @Operation(summary = "删除分类")
    @SaCheckPermission("wms:category:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(warehouseCategoryService.removeCategory(id));
    }
}
