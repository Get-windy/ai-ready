package cn.aiedge.position.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.position.dto.CategoryQueryRequest;
import cn.aiedge.position.dto.CategoryVO;
import cn.aiedge.position.service.PositionCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 岗位分类控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "岗位分类管理", description = "岗位分类增删改查接口")
@RestController
@RequestMapping("/api/position/category")
@RequiredArgsConstructor
public class PositionCategoryController {

    private final PositionCategoryService categoryService;

    @Operation(summary = "分页查询分类")
    @SaCheckPermission("tenant-admin:position:list")
    @GetMapping("/page")
    @RequiresPermission("tenant-admin:position:list")
    public ApiResponse<PageResult<CategoryVO>> pageList(CategoryQueryRequest request) {
        return ApiResponse.ok(categoryService.pageList(request));
    }

    @Operation(summary = "获取所有分类列表")
    @SaCheckPermission("tenant-admin:position:list")
    @GetMapping("/list")
    public ApiResponse<List<CategoryVO>> listAll() {
        return ApiResponse.ok(categoryService.listAll());
    }

    @Operation(summary = "获取分类树")
    @SaCheckPermission("tenant-admin:position:list")
    @GetMapping("/tree")
    public ApiResponse<List<CategoryVO>> getTree() {
        return ApiResponse.ok(categoryService.getTree());
    }

    @Operation(summary = "获取分类详情")
    @SaCheckPermission("tenant-admin:position:query")
    @GetMapping("/{id}")
    @RequiresPermission("tenant-admin:position:query")
    public ApiResponse<CategoryVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getDetail(id));
    }

    @Operation(summary = "创建分类")
    @SaCheckPermission("tenant-admin:position:create")
    @PostMapping
    @RequiresPermission("tenant-admin:position:create")
    public ApiResponse<Long> create(@RequestBody CategoryVO request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @Operation(summary = "更新分类")
    @SaCheckPermission("tenant-admin:position:edit")
    @PutMapping("/{id}")
    @RequiresPermission("tenant-admin:position:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CategoryVO request) {
        categoryService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除分类")
    @SaCheckPermission("tenant-admin:position:delete")
    @DeleteMapping("/{id}")
    @RequiresPermission("tenant-admin:position:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "更新分类状态")
    @SaCheckPermission("tenant-admin:position:edit")
    @PutMapping("/{id}/status")
    @RequiresPermission("tenant-admin:position:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取子分类")
    @SaCheckPermission("tenant-admin:position:list")
    @GetMapping("/{parentId}/children")
    public ApiResponse<List<CategoryVO>> getChildren(@PathVariable Long parentId) {
        return ApiResponse.ok(categoryService.getChildren(parentId));
    }
}
