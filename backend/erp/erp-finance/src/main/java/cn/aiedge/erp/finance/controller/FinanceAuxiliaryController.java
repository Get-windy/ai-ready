package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryBalanceDTO;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryItemDTO;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryBalanceService;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryItemService;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryTypeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 辅助核算Controller
 */
@Tag(name = "辅助核算管理", description = "辅助核算类型、项目、余额查询接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/auxiliary")
@RequiredArgsConstructor
public class FinanceAuxiliaryController {

    private final FinanceAuxiliaryTypeService financeAuxiliaryTypeService;
    private final FinanceAuxiliaryItemService financeAuxiliaryItemService;
    private final FinanceAuxiliaryBalanceService financeAuxiliaryBalanceService;

    // ==================== 辅助核算类型 ====================

    @Operation(summary = "辅助核算类型分页查询")
    @GetMapping("/type/page")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/type/list', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "辅助核算类型分页查询")
    public Result<IPage<FinanceAuxiliaryTypeDTO>> typePage(
            @Parameter(description = "类型编码") @RequestParam(required = false) String typeCode,
            @Parameter(description = "类型名称") @RequestParam(required = false) String typeName,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        return Result.success(financeAuxiliaryTypeService.page(typeCode, typeName, enabled, new Page<>(page, size)));
    }

    @Operation(summary = "查询辅助核算类型列表")
    @GetMapping("/type/list")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/type/list', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "查询辅助核算类型列表")
    public Result<java.util.List<FinanceAuxiliaryTypeDTO>> typeList(
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        return Result.success(financeAuxiliaryTypeService.list(enabled));
    }

    @Operation(summary = "根据ID查询辅助核算类型")
    @GetMapping("/type/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/type/view', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "根据ID查询辅助核算类型")
    public Result<FinanceAuxiliaryTypeDTO> getTypeById(
            @Parameter(description = "类型ID") @PathVariable Long id) {
        return Result.success(financeAuxiliaryTypeService.getById(id));
    }

    @Operation(summary = "创建辅助核算类型")
    @PostMapping("/type")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/type/create', 'finance:auxiliary:create')")
    @OperationLog(module = "辅助核算管理", type = "CREATE", desc = "创建辅助核算类型")
    public Result<FinanceAuxiliaryTypeDTO> createType(@Valid @RequestBody FinanceAuxiliaryTypeDTO dto) {
        return Result.success("创建成功", financeAuxiliaryTypeService.create(dto));
    }

    @Operation(summary = "更新辅助核算类型")
    @PutMapping("/type/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/type/update', 'finance:auxiliary:update')")
    @OperationLog(module = "辅助核算管理", type = "UPDATE", desc = "更新辅助核算类型")
    public Result<FinanceAuxiliaryTypeDTO> updateType(
            @Parameter(description = "类型ID") @PathVariable Long id,
            @Valid @RequestBody FinanceAuxiliaryTypeDTO dto) {
        return Result.success("更新成功", financeAuxiliaryTypeService.update(id, dto));
    }

    @Operation(summary = "删除辅助核算类型")
    @DeleteMapping("/type/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/type/delete', 'finance:auxiliary:delete')")
    @OperationLog(module = "辅助核算管理", type = "DELETE", desc = "删除辅助核算类型")
    public Result<Void> deleteType(
            @Parameter(description = "类型ID") @PathVariable Long id) {
        financeAuxiliaryTypeService.delete(id);
        return Result.success("删除成功", null);
    }

    // ==================== 辅助核算项目 ====================

    @Operation(summary = "辅助核算项目分页查询")
    @GetMapping("/item/page")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/item/list', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "辅助核算项目分页查询")
    public Result<IPage<FinanceAuxiliaryItemDTO>> itemPage(
            @Parameter(description = "辅助核算类型ID") @RequestParam(required = false) Long auxiliaryTypeId,
            @Parameter(description = "项目编码") @RequestParam(required = false) String itemCode,
            @Parameter(description = "项目名称") @RequestParam(required = false) String itemName,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        return Result.success(financeAuxiliaryItemService.page(auxiliaryTypeId, itemCode, itemName, enabled, new Page<>(page, size)));
    }

    @Operation(summary = "查询指定类型下的辅助核算项目列表")
    @GetMapping("/item/list")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/item/list', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "查询指定类型下的辅助核算项目列表")
    public Result<java.util.List<FinanceAuxiliaryItemDTO>> itemList(
            @Parameter(description = "辅助核算类型ID") @RequestParam(required = false) Long auxiliaryTypeId,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        return Result.success(financeAuxiliaryItemService.listByTypeId(auxiliaryTypeId, enabled));
    }

    @Operation(summary = "根据ID查询辅助核算项目")
    @GetMapping("/item/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/item/view', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "根据ID查询辅助核算项目")
    public Result<FinanceAuxiliaryItemDTO> getItemById(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        return Result.success(financeAuxiliaryItemService.getById(id));
    }

    @Operation(summary = "创建辅助核算项目")
    @PostMapping("/item")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/item/create', 'finance:auxiliary:create')")
    @OperationLog(module = "辅助核算管理", type = "CREATE", desc = "创建辅助核算项目")
    public Result<FinanceAuxiliaryItemDTO> createItem(@Valid @RequestBody FinanceAuxiliaryItemDTO dto) {
        return Result.success("创建成功", financeAuxiliaryItemService.create(dto));
    }

    @Operation(summary = "更新辅助核算项目")
    @PutMapping("/item/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/item/update', 'finance:auxiliary:update')")
    @OperationLog(module = "辅助核算管理", type = "UPDATE", desc = "更新辅助核算项目")
    public Result<FinanceAuxiliaryItemDTO> updateItem(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Valid @RequestBody FinanceAuxiliaryItemDTO dto) {
        return Result.success("更新成功", financeAuxiliaryItemService.update(id, dto));
    }

    @Operation(summary = "删除辅助核算项目")
    @DeleteMapping("/item/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/item/delete', 'finance:auxiliary:delete')")
    @OperationLog(module = "辅助核算管理", type = "DELETE", desc = "删除辅助核算项目")
    public Result<Void> deleteItem(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        financeAuxiliaryItemService.delete(id);
        return Result.success("删除成功", null);
    }

    // ==================== 辅助核算余额 ====================

    @Operation(summary = "辅助核算余额分页查询")
    @GetMapping("/balance/page")
    @PreAuthorize("hasPermission('/api/erp/finance/auxiliary/balance/list', 'finance:auxiliary:view')")
    @OperationLog(module = "辅助核算管理", type = "QUERY", desc = "辅助核算余额分页查询")
    public Result<IPage<FinanceAuxiliaryBalanceDTO>> balancePage(
            @Parameter(description = "会计期间ID") @RequestParam(required = false) Long accountingPeriodId,
            @Parameter(description = "科目ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "辅助核算类型ID") @RequestParam(required = false) Long auxiliaryTypeId,
            @Parameter(description = "辅助核算项目ID") @RequestParam(required = false) Long auxiliaryItemId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        return Result.success(financeAuxiliaryBalanceService.page(accountingPeriodId, subjectId, auxiliaryTypeId, auxiliaryItemId, new Page<>(page, size)));
    }
}
