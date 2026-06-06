package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.service.BudgetControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/budget/control")
@Tag(name = "预算控制", description = "预算控制与执行接口")
@RequiredArgsConstructor
public class BudgetControlController {

    private final BudgetControlService budgetControlService;

    @Operation(summary = "检查预算可用性")
    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> check(
            @Parameter(description = "预算ID") @RequestParam Long budgetId,
            @Parameter(description = "预算科目ID") @RequestParam Long budgetItemId,
            @Parameter(description = "金额") @RequestParam BigDecimal amount) {
        Map<String, Object> result = budgetControlService.checkAvailability(budgetId, budgetItemId, amount);
        return ApiResponse.success(result);
    }

    @Operation(summary = "冻结预算金额")
    @PostMapping("/freeze")
    public ApiResponse<Map<String, Object>> freeze(
            @Parameter(description = "预算ID") @RequestParam Long budgetId,
            @Parameter(description = "预算科目ID") @RequestParam Long budgetItemId,
            @Parameter(description = "金额") @RequestParam BigDecimal amount,
            @Parameter(description = "来源类型") @RequestParam String sourceType,
            @Parameter(description = "来源单号") @RequestParam(required = false) String sourceNo,
            @Parameter(description = "来源ID") @RequestParam(required = false) Long sourceId) {
        Map<String, Object> result = budgetControlService.freezeAmount(
                budgetId, budgetItemId, amount, sourceType, sourceNo, sourceId);
        return ApiResponse.success("冻结成功", result);
    }

    @Operation(summary = "释放冻结金额")
    @PostMapping("/release")
    public ApiResponse<Map<String, Object>> release(
            @Parameter(description = "预算ID") @RequestParam Long budgetId,
            @Parameter(description = "预算科目ID") @RequestParam Long budgetItemId,
            @Parameter(description = "金额") @RequestParam BigDecimal amount,
            @Parameter(description = "来源类型") @RequestParam String sourceType,
            @Parameter(description = "来源单号") @RequestParam(required = false) String sourceNo,
            @Parameter(description = "来源ID") @RequestParam(required = false) Long sourceId) {
        Map<String, Object> result = budgetControlService.releaseFrozenAmount(
                budgetId, budgetItemId, amount, sourceType, sourceNo, sourceId);
        return ApiResponse.success("释放成功", result);
    }

    @Operation(summary = "记录预算消耗")
    @PostMapping("/consume")
    public ApiResponse<Map<String, Object>> consume(
            @Parameter(description = "预算ID") @RequestParam Long budgetId,
            @Parameter(description = "预算科目ID") @RequestParam Long budgetItemId,
            @Parameter(description = "金额") @RequestParam BigDecimal amount,
            @Parameter(description = "来源类型") @RequestParam String sourceType,
            @Parameter(description = "来源单号") @RequestParam(required = false) String sourceNo,
            @Parameter(description = "来源ID") @RequestParam(required = false) Long sourceId,
            @Parameter(description = "描述") @RequestParam(required = false) String description) {
        Map<String, Object> result = budgetControlService.consumeBudget(
                budgetId, budgetItemId, amount, sourceType, sourceNo, sourceId, description);
        return ApiResponse.success("消耗成功", result);
    }
}
