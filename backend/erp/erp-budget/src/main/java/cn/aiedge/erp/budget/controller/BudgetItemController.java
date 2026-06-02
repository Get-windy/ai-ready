package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.dto.BudgetItemDTO;
import cn.aiedge.erp.budget.service.BudgetItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/erp/budget/item")
@Tag(name = "预算科目", description = "预算科目明细管理")
@RequiredArgsConstructor
public class BudgetItemController {

    private final BudgetItemService budgetItemService;

    @Operation(summary = "获取预算科目列表")
    @GetMapping("/list-by-budget/{budgetId}")
    public ApiResponse<List<BudgetItemDTO>> listByBudget(
            @Parameter(description = "预算ID") @PathVariable Long budgetId) {
        List<BudgetItemDTO> list = budgetItemService.listByBudgetId(budgetId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "更新预算科目")
    @PutMapping("/{id}")
    public ApiResponse<BudgetItemDTO> update(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @Valid @RequestBody BudgetItemDTO dto) {
        BudgetItemDTO result = budgetItemService.update(id, dto);
        return ApiResponse.success("更新成功", result);
    }

    @Operation(summary = "获取预算科目详情")
    @GetMapping("/{id}")
    public ApiResponse<BudgetItemDTO> getDetail(@Parameter(description = "科目ID") @PathVariable Long id) {
        BudgetItemDTO result = budgetItemService.getById(id);
        return ApiResponse.success(result);
    }
}
