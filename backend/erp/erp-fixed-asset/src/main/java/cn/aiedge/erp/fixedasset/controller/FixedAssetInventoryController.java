package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetInventoryDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 固定资产盘点控制器
 */
@RestController
@RequestMapping("/erp/fixed-asset/inventory")
@Tag(name = "固定资产盘点管理", description = "资产盘点、结果录入等管理功能")
@RequiredArgsConstructor
public class FixedAssetInventoryController {

    private final FixedAssetInventoryService inventoryService;

    @Operation(summary = "创建盘点记录")
    @PostMapping
    public ApiResponse<FixedAssetInventoryDTO> create(@Valid @RequestBody FixedAssetInventoryDTO dto) {
        FixedAssetInventoryDTO result = inventoryService.create(dto);
        return ApiResponse.success("盘点记录创建成功", result);
    }

    @Operation(summary = "更新盘点记录")
    @PutMapping("/{id}")
    public ApiResponse<FixedAssetInventoryDTO> update(
            @Parameter(description = "盘点ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetInventoryDTO dto) {
        FixedAssetInventoryDTO result = inventoryService.update(id, dto);
        return ApiResponse.success("盘点记录更新成功", result);
    }

    @Operation(summary = "分页查询盘点记录")
    @GetMapping("/page")
    public ApiResponse<Page<FixedAssetInventoryDTO>> getPage(
            @Parameter(description = "盘点单号") @RequestParam(required = false) String inventoryNo,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "盘点结果") @RequestParam(required = false) String checkResult,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Page<FixedAssetInventoryDTO> result = inventoryService.getPage(inventoryNo, status, checkResult,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取盘点记录详情")
    @GetMapping("/{id}")
    public ApiResponse<FixedAssetInventoryDTO> getById(
            @Parameter(description = "盘点ID") @PathVariable Long id) {
        FixedAssetInventoryDTO result = inventoryService.getById(id);
        return ApiResponse.success(result);
    }
}
