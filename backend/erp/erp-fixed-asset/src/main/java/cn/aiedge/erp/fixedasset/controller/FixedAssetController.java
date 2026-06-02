package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 固定资产控制器
 */
@RestController
@RequestMapping("/erp/fixed-asset/asset")
@Tag(name = "固定资产管理", description = "固定资产登记、折旧、查询等管理功能")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    @Operation(summary = "创建固定资产")
    @PostMapping
    public ApiResponse<FixedAssetDTO> create(@Valid @RequestBody FixedAssetDTO dto) {
        FixedAssetDTO result = fixedAssetService.create(dto);
        return ApiResponse.success("固定资产创建成功", result);
    }

    @Operation(summary = "更新固定资产")
    @PutMapping("/{id}")
    public ApiResponse<FixedAssetDTO> update(
            @Parameter(description = "资产ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetDTO dto) {
        FixedAssetDTO result = fixedAssetService.update(id, dto);
        return ApiResponse.success("固定资产更新成功", result);
    }

    @Operation(summary = "删除固定资产")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "资产ID") @PathVariable Long id) {
        fixedAssetService.delete(id);
        return ApiResponse.success("固定资产删除成功", null);
    }

    @Operation(summary = "获取固定资产详情")
    @GetMapping("/{id}")
    public ApiResponse<FixedAssetDTO> getById(
            @Parameter(description = "资产ID") @PathVariable Long id) {
        FixedAssetDTO result = fixedAssetService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "分页查询固定资产")
    @GetMapping("/page")
    public ApiResponse<Page<FixedAssetDTO>> getPage(
            @Parameter(description = "资产编码") @RequestParam(required = false) String assetCode,
            @Parameter(description = "资产名称") @RequestParam(required = false) String assetName,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Page<FixedAssetDTO> result = fixedAssetService.getPage(assetCode, assetName, categoryId,
            status, departmentId, keyword, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(result);
    }

    @Operation(summary = "计提折旧")
    @PostMapping("/{id}/depreciate")
    public ApiResponse<FixedAssetDTO> depreciate(
            @Parameter(description = "资产ID") @PathVariable Long id) {
        FixedAssetDTO result = fixedAssetService.depreciate(id);
        return ApiResponse.success("折旧计提成功", result);
    }

    @Operation(summary = "获取资产统计")
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = fixedAssetService.getStatistics();
        return ApiResponse.success(statistics);
    }

    @Operation(summary = "导出资产数据")
    @GetMapping("/export")
    public ApiResponse<Void> export() {
        // In production, implement file export logic
        return ApiResponse.success("导出功能待实现", null);
    }
}
