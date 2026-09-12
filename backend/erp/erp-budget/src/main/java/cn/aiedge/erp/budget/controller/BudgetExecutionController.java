package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.dto.BudgetExecutionLogVO;
import cn.aiedge.erp.budget.dto.BudgetExecutionQuery;
import cn.aiedge.erp.budget.service.BudgetExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 预算执行跟踪（只读）
 *
 * <p>P0 红线：只提供查询能力，不提供任何修改已执行 / 冻结 / 剩余额的接口。</p>
 */
@RestController
@RequestMapping("/api/erp/budget/execution")
@Tag(name = "预算执行", description = "预算执行跟踪与超支预警（只读）")
@RequiredArgsConstructor
public class BudgetExecutionController {

    private final BudgetExecutionService budgetExecutionService;

    @Operation(summary = "按预算单维度分页")
    @GetMapping("/rows")
    public ApiResponse<Map<String, Object>> rows(BudgetExecutionQuery query) {
        return ApiResponse.success(budgetExecutionService.pageRows(query));
    }

    @Operation(summary = "按预算科目明细维度分页")
    @GetMapping("/items")
    public ApiResponse<Map<String, Object>> items(BudgetExecutionQuery query) {
        return ApiResponse.success(budgetExecutionService.pageItems(query));
    }

    @Operation(summary = "预算执行流水")
    @GetMapping("/logs")
    public ApiResponse<List<BudgetExecutionLogVO>> logs(
            @Parameter(description = "预算ID") @RequestParam(required = false) Long budgetId,
            @Parameter(description = "预算科目明细ID") @RequestParam(required = false) Long budgetItemId) {
        return ApiResponse.success(budgetExecutionService.logs(budgetId, budgetItemId));
    }

    @Operation(summary = "超支预警清单")
    @GetMapping("/warnings")
    public ApiResponse<Map<String, Object>> warnings(
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear) {
        if (fiscalYear == null) {
            fiscalYear = java.time.Year.now().getValue();
        }
        return ApiResponse.success(budgetExecutionService.warnings(fiscalYear));
    }
}
