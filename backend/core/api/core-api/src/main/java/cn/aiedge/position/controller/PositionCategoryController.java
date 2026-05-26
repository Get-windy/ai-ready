package cn.aiedge.position.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.position.dto.CategoryVO;
import cn.aiedge.position.service.PositionCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位分类控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "岗位分类管理", description = "岗位分类增删改查接口")
@RestController
@RequestMapping("/api/v1/position-categories")
@RequiredArgsConstructor
public class PositionCategoryController {

    private final PositionCategoryService categoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public ApiResponse<List<CategoryVO>> getTree() {
        return ApiResponse.ok(categoryService.getTree());
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    @RequiresPermission("position:query")
    public ApiResponse<CategoryVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getDetail(id));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    @RequiresPermission("position:create")
    public ApiResponse<Long> create(@RequestBody CategoryVO request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @Operation(summary = "更新分类")
    @PutMapping
    @RequiresPermission("position:edit")
    public ApiResponse<Void> update(@RequestBody CategoryVO request) {
        categoryService.update(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @RequiresPermission("position:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取子分类")
    @GetMapping("/{parentId}/children")
    public ApiResponse<List<CategoryVO>> getChildren(@PathVariable Long parentId) {
        return ApiResponse.ok(categoryService.getChildren(parentId));
    }
}
