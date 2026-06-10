package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.dto.FixedAssetPurchaseDTO;
import cn.aiedge.erp.fixedasset.service.FixedAssetPurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 固定资产购置申请控制器
 */
@RestController
@RequestMapping("/api/erp/fixed-asset/purchase")
@Tag(name = "固定资产购置管理", description = "资产购置申请、审批、入库验收等管理功能")
@RequiredArgsConstructor
public class FixedAssetPurchaseController {

    private final FixedAssetPurchaseService purchaseService;

    @Operation(summary = "创建购置申请")
    @PostMapping
    @RequiresPermission("erp:fixed-asset:purchase:create")
    public ApiResponse<FixedAssetPurchaseDTO> create(@Valid @RequestBody FixedAssetPurchaseDTO dto) {
        FixedAssetPurchaseDTO result = purchaseService.create(dto);
        return ApiResponse.success("购置申请创建成功", result);
    }

    @Operation(summary = "更新购置申请")
    @PutMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:purchase:update")
    public ApiResponse<FixedAssetPurchaseDTO> update(
            @Parameter(description = "购置ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetPurchaseDTO dto) {
        FixedAssetPurchaseDTO result = purchaseService.update(id, dto);
        return ApiResponse.success("购置申请更新成功", result);
    }

    @Operation(summary = "删除购置申请")
    @DeleteMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:purchase:delete")
    public ApiResponse<Void> delete(
            @Parameter(description = "购置ID") @PathVariable Long id) {
        purchaseService.delete(id);
        return ApiResponse.success("购置申请删除成功", null);
    }

    @Operation(summary = "分页查询购置申请")
    @GetMapping("/page")
    @RequiresPermission("erp:fixed-asset:purchase:list")
    public ApiResponse<Page<FixedAssetPurchaseDTO>> getPage(
            @Parameter(description = "申请单号") @RequestParam(required = false) String purchaseNo,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "申请标题") @RequestParam(required = false) String title,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Page<FixedAssetPurchaseDTO> result = purchaseService.getPage(purchaseNo, status, title,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取购置申请详情")
    @GetMapping("/{id}")
    @RequiresPermission("erp:fixed-asset:purchase:query")
    public ApiResponse<FixedAssetPurchaseDTO> getById(
            @Parameter(description = "购置ID") @PathVariable Long id) {
        FixedAssetPurchaseDTO result = purchaseService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @RequiresPermission("erp:fixed-asset:purchase:submit")
    public ApiResponse<FixedAssetPurchaseDTO> submit(
            @Parameter(description = "购置ID") @PathVariable Long id) {
        FixedAssetPurchaseDTO result = purchaseService.submit(id);
        return ApiResponse.success("购置申请已提交审批", result);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @RequiresPermission("erp:fixed-asset:purchase:approve")
    public ApiResponse<FixedAssetPurchaseDTO> approve(
            @Parameter(description = "购置ID") @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        FixedAssetPurchaseDTO result = purchaseService.approve(id, comment);
        return ApiResponse.success("购置申请已审批通过", result);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @RequiresPermission("erp:fixed-asset:purchase:approve")
    public ApiResponse<FixedAssetPurchaseDTO> reject(
            @Parameter(description = "购置ID") @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam(required = false) String comment) {
        FixedAssetPurchaseDTO result = purchaseService.reject(id, comment);
        return ApiResponse.success("购置申请已拒绝", result);
    }

    @Operation(summary = "入库验收")
    @PostMapping("/{id}/accept")
    @RequiresPermission("erp:fixed-asset:purchase:accept")
    public ApiResponse<FixedAssetPurchaseDTO> accept(
            @Parameter(description = "购置ID") @PathVariable Long id,
            @Valid @RequestBody FixedAssetPurchaseDTO dto) {
        FixedAssetPurchaseDTO result = purchaseService.accept(id, dto);
        return ApiResponse.success("入库验收完成，已生成资产卡片", result);
    }

    @Operation(summary = "获取购置申请统计")
    @GetMapping("/statistics")
    @RequiresPermission("erp:fixed-asset:purchase:list")
    public ApiResponse<Map<String, Object>> getStatistics() {
        return ApiResponse.success(purchaseService.getStatistics());
    }
}
