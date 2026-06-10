package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetDisposalDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetDisposalService;
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
 * 固定资产处置控制器
 */
@RestController
@RequestMapping("/api/erp/fixed-asset/disposal")
@Tag(name = "固定资产处置管理", description = "资产处置申请、审批等管理功能")
@RequiredArgsConstructor
public class FixedAssetDisposalController {

    private final FixedAssetDisposalService disposalService;

    @Operation(summary = "创建处置申请")
    @PostMapping
    @RequiresPermission("erp:fixed-asset:disposal:create")
    public ApiResponse<FixedAssetDisposalDTO> create(@Valid @RequestBody FixedAssetDisposalDTO dto) {
        FixedAssetDisposalDTO result = disposalService.create(dto);
        return ApiResponse.success("处置申请创建成功", result);
    }

    @Operation(summary = "更新处置申请")
    @PutMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:disposal:update")
    public ApiResponse<FixedAssetDisposalDTO> update(
            @Parameter(description = "处置ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetDisposalDTO dto) {
        FixedAssetDisposalDTO result = disposalService.update(id, dto);
        return ApiResponse.success("处置申请更新成功", result);
    }

    @Operation(summary = "删除处置申请")
    @DeleteMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:disposal:delete")
    public ApiResponse<Void> delete(
            @Parameter(description = "处置ID") @PathVariable Long id) {
        disposalService.delete(id);
        return ApiResponse.success("处置申请删除成功", null);
    }

    @Operation(summary = "分页查询处置记录")
    @GetMapping("/page")
    @RequiresPermission("erp:fixed-asset:disposal:list")
    public ApiResponse<Page<FixedAssetDisposalDTO>> getPage(
            @Parameter(description = "处置单号") @RequestParam(required = false) String disposalNo,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Page<FixedAssetDisposalDTO> result = disposalService.getPage(disposalNo, status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取处置记录详情")
    @GetMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:disposal:query")
    public ApiResponse<FixedAssetDisposalDTO> getById(
            @Parameter(description = "处置ID") @PathVariable Long id) {
        FixedAssetDisposalDTO result = disposalService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @RequiresPermission("erp:fixed-asset:disposal:approve")
    public ApiResponse<FixedAssetDisposalDTO> approve(
            @Parameter(description = "处置ID") @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        FixedAssetDisposalDTO result = disposalService.approve(id, comment);
        return ApiResponse.success("处置申请已审批通过", result);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @RequiresPermission("erp:fixed-asset:disposal:approve")
    public ApiResponse<FixedAssetDisposalDTO> reject(
            @Parameter(description = "处置ID") @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam(required = false) String comment) {
        FixedAssetDisposalDTO result = disposalService.reject(id, comment);
        return ApiResponse.success("处置申请已拒绝", result);
    }
}
