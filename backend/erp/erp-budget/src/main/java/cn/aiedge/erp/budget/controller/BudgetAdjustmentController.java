package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.dto.BudgetAdjustmentDTO;
import cn.aiedge.erp.budget.service.BudgetAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/budget/adjustment")
@Tag(name = "预算调整", description = "预算调整管理")
@RequiredArgsConstructor
public class BudgetAdjustmentController {

    private final BudgetAdjustmentService budgetAdjustmentService;

    @Operation(summary = "创建预算调整")
    @PostMapping
    public ApiResponse<BudgetAdjustmentDTO> create(@Valid @RequestBody BudgetAdjustmentDTO dto) {
        BudgetAdjustmentDTO result = budgetAdjustmentService.create(dto);
        return ApiResponse.success("创建成功", result);
    }

    @Operation(summary = "更新预算调整")
    @PutMapping("/{id}")
    public ApiResponse<BudgetAdjustmentDTO> update(
            @Parameter(description = "调整ID") @PathVariable Long id,
            @Valid @RequestBody BudgetAdjustmentDTO dto) {
        BudgetAdjustmentDTO result = budgetAdjustmentService.update(id, dto);
        return ApiResponse.success("更新成功", result);
    }

    @Operation(summary = "获取预算调整详情")
    @GetMapping("/{id}")
    public ApiResponse<BudgetAdjustmentDTO> getDetail(@Parameter(description = "调整ID") @PathVariable Long id) {
        BudgetAdjustmentDTO result = budgetAdjustmentService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "分页查询预算调整列表")
    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(
            @Parameter(description = "预算ID") @RequestParam(required = false) Long budgetId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "调整类型") @RequestParam(required = false) String adjustmentType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = budgetAdjustmentService.page(budgetId, status, adjustmentType, page, size);
        return ApiResponse.success(result);
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    public ApiResponse<BudgetAdjustmentDTO> submit(@Parameter(description = "调整ID") @PathVariable Long id) {
        BudgetAdjustmentDTO result = budgetAdjustmentService.submit(id);
        return ApiResponse.success("已提交审批", result);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public ApiResponse<BudgetAdjustmentDTO> approve(
            @Parameter(description = "调整ID") @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        BudgetAdjustmentDTO result = budgetAdjustmentService.approve(id, comment);
        return ApiResponse.success("审批通过", result);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    public ApiResponse<BudgetAdjustmentDTO> reject(
            @Parameter(description = "调整ID") @PathVariable Long id,
            @RequestParam String comment) {
        BudgetAdjustmentDTO result = budgetAdjustmentService.reject(id, comment);
        return ApiResponse.success("已拒绝", result);
    }

    @Operation(summary = "导出预算调整列表")
    @GetMapping("/export")
    public ApiResponse<List<BudgetAdjustmentDTO>> export(
            @Parameter(description = "预算ID") @RequestParam(required = false) Long budgetId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "调整类型") @RequestParam(required = false) String adjustmentType) {
        List<BudgetAdjustmentDTO> list = budgetAdjustmentService.exportList(budgetId, status, adjustmentType);
        return ApiResponse.success(list);
    }

    @Operation(summary = "删除预算调整")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "调整ID") @PathVariable Long id) {
        budgetAdjustmentService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
