package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetCategoryDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 固定资产分类控制器
 */
@RestController
@RequestMapping("/api/erp/fixed-asset/category")
@Tag(name = "固定资产分类管理", description = "固定资产分类的增删改查及树形结构")
@RequiredArgsConstructor
public class FixedAssetCategoryController {

    private final FixedAssetCategoryService categoryService;

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<FixedAssetCategoryDTO> create(@Valid @RequestBody FixedAssetCategoryDTO dto) {
        FixedAssetCategoryDTO result = categoryService.create(dto);
        return ApiResponse.success("分类创建成功", result);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ApiResponse<FixedAssetCategoryDTO> update(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetCategoryDTO dto) {
        FixedAssetCategoryDTO result = categoryService.update(id, dto);
        return ApiResponse.success("分类更新成功", result);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("分类删除成功", null);
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public ApiResponse<FixedAssetCategoryDTO> getById(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        FixedAssetCategoryDTO result = categoryService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取所有分类")
    @GetMapping("/list")
    public ApiResponse<List<FixedAssetCategoryDTO>> getAll() {
        List<FixedAssetCategoryDTO> result = categoryService.getAll();
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public ApiResponse<List<Map<String, Object>>> getTree() {
        List<Map<String, Object>> tree = categoryService.getTree();
        return ApiResponse.success(tree);
    }
}
