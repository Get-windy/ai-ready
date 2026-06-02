package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetDepreciationDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetDepreciationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 固定资产折旧控制器
 */
@RestController
@RequestMapping("/erp/fixed-asset/depreciation")
@Tag(name = "固定资产折旧管理", description = "折旧计提、查询等管理功能")
@RequiredArgsConstructor
public class FixedAssetDepreciationController {

    private final FixedAssetDepreciationService depreciationService;

    @Operation(summary = "批量计提折旧")
    @PostMapping("/batch-calculate")
    public ApiResponse<List<FixedAssetDepreciationDTO>> batchCalculate() {
        List<FixedAssetDepreciationDTO> result = depreciationService.batchCalculate();
        return ApiResponse.success("批量折旧计提完成", result);
    }

    @Operation(summary = "分页查询折旧记录")
    @GetMapping("/page")
    public ApiResponse<Page<FixedAssetDepreciationDTO>> getPage(
            @Parameter(description = "资产ID") @RequestParam(required = false) Long assetId,
            @Parameter(description = "期间") @RequestParam(required = false) String period,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Page<FixedAssetDepreciationDTO> result = depreciationService.getPage(assetId, period,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取折旧记录详情")
    @GetMapping("/{id}")
    public ApiResponse<FixedAssetDepreciationDTO> getById(
            @Parameter(description = "折旧记录ID") @PathVariable Long id) {
        FixedAssetDepreciationDTO result = depreciationService.getById(id);
        return ApiResponse.success(result);
    }
}
