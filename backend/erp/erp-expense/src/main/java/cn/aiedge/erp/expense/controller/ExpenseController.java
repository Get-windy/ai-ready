package cn.aiedge.erp.expense.controller;

import cn.aiedge.erp.expense.dto.ApiResponse;
import cn.aiedge.erp.expense.dto.ExpenseApplicationDTO;
import cn.aiedge.erp.expense.dto.ExpenseRequest;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import cn.aiedge.erp.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expense")
@Tag(name = "费用单管理", description = "费用单申请、审批、报销等全流程管理")
@RequiredArgsConstructor
public class ExpenseController {
    
    private final ExpenseService expenseService;
    
    @Operation(summary = "申请费用单", description = "创建新的费用申请")
    @PostMapping("/apply")
    public ApiResponse<ExpenseApplicationDTO> applyExpense(
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseApplicationDTO dto = expenseService.applyExpense(request);
        return ApiResponse.success("费用单申请成功", dto);
    }
    
    @Operation(summary = "获取费用单详情", description = "根据ID获取费用单详细信息")
    @GetMapping("/{id}")
    public ApiResponse<ExpenseApplicationDTO> getExpenseDetail(
            @Parameter(description = "费用单ID") @PathVariable Long id) {
        ExpenseApplicationDTO dto = expenseService.getExpenseDetail(id);
        return ApiResponse.success(dto);
    }
    
    @Operation(summary = "更新费用单", description = "更新费用单信息")
    @PutMapping("/{id}")
    public ApiResponse<ExpenseApplicationDTO> updateExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseApplicationDTO dto = expenseService.updateExpense(id, request);
        return ApiResponse.success("费用单更新成功", dto);
    }
    
    @Operation(summary = "删除费用单", description = "删除指定的费用单")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ApiResponse.success("费用单删除成功", null);
    }
    
    @Operation(summary = "提交审批", description = "提交费用单进入审批流程")
    @PostMapping("/{id}/submit")
    public ApiResponse<ExpenseApplicationDTO> submitForApproval(
            @Parameter(description = "费用单ID") @PathVariable Long id) {
        ExpenseApplicationDTO dto = expenseService.submitForApproval(id);
        return ApiResponse.success("费用单已提交审批", dto);
    }
    
    @Operation(summary = "审批通过", description = "审批人批准费用单")
    @PostMapping("/{id}/approve")
    public ApiResponse<ExpenseApplicationDTO> approveExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        ExpenseApplicationDTO dto = expenseService.approveExpense(id, comment);
        return ApiResponse.success("费用单审批通过", dto);
    }
    
    @Operation(summary = "审批拒绝", description = "审批人拒绝费用单")
    @PostMapping("/{id}/reject")
    public ApiResponse<ExpenseApplicationDTO> rejectExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id,
            @RequestParam String reason) {
        ExpenseApplicationDTO dto = expenseService.rejectExpense(id, reason);
        return ApiResponse.success("费用单审批拒绝", dto);
    }
    
    @Operation(summary = "费用单列表", description = "获取费用单列表，支持分页和过滤")
    @GetMapping("/list")
    public ApiResponse<List<ExpenseApplicationDTO>> getExpenseList(
            @Parameter(description = "申请人ID") @RequestParam(required = false) String applicantId,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) ExpenseStatus status,
            @Parameter(description = "开始日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        List<ExpenseApplicationDTO> list = expenseService.getExpenseList(
                applicantId, departmentId, status, startDate, endDate, page, size);
        return ApiResponse.success(list);
    }
    
    @Operation(summary = "费用统计", description = "获取费用统计数据")
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getExpenseStatistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "费用类型") @RequestParam(required = false) ExpenseType expenseType) {
        Map<String, Object> statistics = expenseService.getExpenseStatistics(
                startDate, endDate, departmentId, expenseType);
        return ApiResponse.success(statistics);
    }
}