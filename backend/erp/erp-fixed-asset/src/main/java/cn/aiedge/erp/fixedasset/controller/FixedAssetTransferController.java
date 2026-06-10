package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetTransferDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetTransferService;
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
 * 固定资产转移控制器
 */
@RestController
@RequestMapping("/api/erp/fixed-asset/transfer")
@Tag(name = "固定资产转移管理", description = "资产转移申请、审批等管理功能")
@RequiredArgsConstructor
public class FixedAssetTransferController {

    private final FixedAssetTransferService transferService;

    @Operation(summary = "创建转移申请")
    @PostMapping
    @RequiresPermission("erp:fixed-asset:transfer:create")
    public ApiResponse<FixedAssetTransferDTO> create(@Valid @RequestBody FixedAssetTransferDTO dto) {
        FixedAssetTransferDTO result = transferService.create(dto);
        return ApiResponse.success("转移申请创建成功", result);
    }

    @Operation(summary = "更新转移申请")
    @PutMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:transfer:update")
    public ApiResponse<FixedAssetTransferDTO> update(
            @Parameter(description = "转移ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetTransferDTO dto) {
        FixedAssetTransferDTO result = transferService.update(id, dto);
        return ApiResponse.success("转移申请更新成功", result);
    }

    @Operation(summary = "删除转移申请")
    @DeleteMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:transfer:delete")
    public ApiResponse<Void> delete(
            @Parameter(description = "转移ID") @PathVariable Long id) {
        transferService.delete(id);
        return ApiResponse.success("转移申请删除成功", null);
    }

    @Operation(summary = "分页查询转移记录")
    @GetMapping("/page")
    @RequiresPermission("erp:fixed-asset:transfer:list")
    public ApiResponse<Page<FixedAssetTransferDTO>> getPage(
            @Parameter(description = "转移单号") @RequestParam(required = false) String transferNo,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Page<FixedAssetTransferDTO> result = transferService.getPage(transferNo, status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取转移记录详情")
    @GetMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:transfer:query")
    public ApiResponse<FixedAssetTransferDTO> getById(
            @Parameter(description = "转移ID") @PathVariable Long id) {
        FixedAssetTransferDTO result = transferService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @RequiresPermission("erp:fixed-asset:transfer:approve")
    public ApiResponse<FixedAssetTransferDTO> approve(
            @Parameter(description = "转移ID") @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        FixedAssetTransferDTO result = transferService.approve(id, comment);
        return ApiResponse.success("转移申请已审批通过", result);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @RequiresPermission("erp:fixed-asset:transfer:approve")
    public ApiResponse<FixedAssetTransferDTO> reject(
            @Parameter(description = "转移ID") @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam(required = false) String comment) {
        FixedAssetTransferDTO result = transferService.reject(id, comment);
        return ApiResponse.success("转移申请已拒绝", result);
    }
}
