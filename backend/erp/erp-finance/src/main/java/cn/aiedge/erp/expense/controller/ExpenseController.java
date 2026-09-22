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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@RestController
@RequestMapping("/api/erp/expense")
@Tag(name = "费用单管理", description = "费用单申请、审批、报销等全流程管理")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // ========== /application/* 端点（前端标准路径） ==========

    @Operation(summary = "费用申请分页列表", description = "获取费用申请分页列表")
    @SaCheckPermission("erp:expense:application:list")
    @GetMapping("/application/page")
    public ApiResponse<List<ExpenseApplicationDTO>> getApplicationPage(
            @Parameter(description = "申请人ID") @RequestParam(required = false) String applicantId,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) ExpenseStatus status,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        List<ExpenseApplicationDTO> list = expenseService.getExpenseList(
                applicantId, departmentId, status, startDate, endDate, page - 1, size);
        return ApiResponse.success(list);
    }

    @Operation(summary = "获取费用申请详情", description = "根据ID获取费用申请详细信息")
    @SaCheckPermission("erp:expense:application:query")
    @GetMapping("/application/{id}")
    public ApiResponse<ExpenseApplicationDTO> getApplicationDetail(
            @Parameter(description = "费用申请ID") @PathVariable Long id) {
        ExpenseApplicationDTO dto = expenseService.getExpenseDetail(id);
        return ApiResponse.success(dto);
    }

    @Operation(summary = "创建费用申请", description = "创建新的费用申请")
    @SaCheckPermission("erp:expense:application:create")
    @PostMapping("/application")
    public ApiResponse<ExpenseApplicationDTO> createApplication(
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseApplicationDTO dto = expenseService.applyExpense(request);
        return ApiResponse.success("费用单申请成功", dto);
    }

    @Operation(summary = "更新费用申请", description = "更新费用申请信息")
    @SaCheckPermission("erp:expense:application:edit")
    @PutMapping("/application/{id}")
    public ApiResponse<ExpenseApplicationDTO> updateApplication(
            @Parameter(description = "费用申请ID") @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseApplicationDTO dto = expenseService.updateExpense(id, request);
        return ApiResponse.success("费用单更新成功", dto);
    }

    @Operation(summary = "删除费用申请", description = "删除指定的费用申请")
    @SaCheckPermission("erp:expense:application:delete")
    @DeleteMapping("/application/{id}")
    public ApiResponse<Void> deleteApplication(
            @Parameter(description = "费用申请ID") @PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ApiResponse.success("费用单删除成功", null);
    }

    @Operation(summary = "提交审批", description = "提交费用单进入审批流程")
    @SaCheckPermission("erp:expense:application:submit")
    @PostMapping("/application/{id}/submit")
    public ApiResponse<ExpenseApplicationDTO> submitApplication(
            @Parameter(description = "费用申请ID") @PathVariable Long id) {
        ExpenseApplicationDTO dto = expenseService.submitForApproval(id);
        return ApiResponse.success("费用单已提交审批", dto);
    }

    @Operation(summary = "撤回申请", description = "撤回已提交的费用申请")
    @SaCheckPermission("erp:expense:application:edit")
    @PostMapping("/application/{id}/withdraw")
    public ApiResponse<ExpenseApplicationDTO> withdrawApplication(
            @Parameter(description = "费用申请ID") @PathVariable Long id) {
        ExpenseApplicationDTO dto = expenseService.withdrawApplication(id);
        return ApiResponse.success("费用单已撤回", dto);
    }

    // ========== /statistics/* 端点 ==========

    @Operation(summary = "统计分页列表", description = "获取费用统计数据分页列表")
    @SaCheckPermission("erp:expense:statistics:list")
    @GetMapping("/statistics/page")
    public ApiResponse<Map<String, Object>> getStatisticsPage(
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "费用类型") @RequestParam(required = false) ExpenseType expenseType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> statistics = expenseService.getExpenseStatistics(
                startDate, endDate, departmentId, expenseType);
        statistics.put("page", page);
        statistics.put("size", size);
        return ApiResponse.success(statistics);
    }

    @Operation(summary = "统计汇总", description = "获取费用统计汇总数据")
    @SaCheckPermission("erp:expense:statistics:list")
    @GetMapping("/statistics/summary")
    public ApiResponse<Map<String, Object>> getStatisticsSummary(
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

    @Operation(summary = "按部门统计", description = "获取按部门维度的费用统计")
    @SaCheckPermission("erp:expense:statistics:list")
    @GetMapping("/statistics/by-department")
    public ApiResponse<Map<String, Object>> getStatisticsByDepartment(
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> statistics = expenseService.getExpenseStatistics(
                startDate, endDate, null, null);
        Map<String, Object> result = new HashMap<>();
        result.put("byDepartment", statistics.get("byDepartment"));
        result.put("totalAmount", statistics.get("totalAmount"));
        result.put("expenseCount", statistics.get("expenseCount"));
        return ApiResponse.success(result);
    }

    @Operation(summary = "按类型统计", description = "获取按费用类型维度的统计")
    @SaCheckPermission("erp:expense:statistics:list")
    @GetMapping("/statistics/by-type")
    public ApiResponse<Map<String, Object>> getStatisticsByType(
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> statistics = expenseService.getExpenseStatistics(
                startDate, endDate, null, null);
        Map<String, Object> result = new HashMap<>();
        result.put("byType", statistics.get("byType"));
        result.put("totalAmount", statistics.get("totalAmount"));
        result.put("expenseCount", statistics.get("expenseCount"));
        return ApiResponse.success(result);
    }

    // ========== 兼容旧路径端点 ==========

    @Operation(summary = "申请费用单", description = "创建新的费用申请（兼容旧路径）")
    @SaCheckPermission("erp:expense:application:create")
    @PostMapping("/apply")
    public ApiResponse<ExpenseApplicationDTO> applyExpense(
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseApplicationDTO dto = expenseService.applyExpense(request);
        return ApiResponse.success("费用单申请成功", dto);
    }

    @Operation(summary = "获取费用单详情", description = "根据ID获取费用单详细信息（兼容旧路径）")
    @SaCheckPermission("erp:expense:application:query")
    @GetMapping("/{id}")
    public ApiResponse<ExpenseApplicationDTO> getExpenseDetail(
            @Parameter(description = "费用单ID") @PathVariable Long id) {
        ExpenseApplicationDTO dto = expenseService.getExpenseDetail(id);
        return ApiResponse.success(dto);
    }

    @Operation(summary = "更新费用单", description = "更新费用单信息（兼容旧路径）")
    @SaCheckPermission("erp:expense:application:edit")
    @PutMapping("/{id}")
    public ApiResponse<ExpenseApplicationDTO> updateExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseApplicationDTO dto = expenseService.updateExpense(id, request);
        return ApiResponse.success("费用单更新成功", dto);
    }

    @Operation(summary = "删除费用单", description = "删除指定的费用单（兼容旧路径）")
    @SaCheckPermission("erp:expense:application:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ApiResponse.success("费用单删除成功", null);
    }

    @Operation(summary = "审批通过", description = "审批人批准费用单")
    @SaCheckPermission("erp:expense:approval:process")
    @PostMapping("/{id}/approve")
    public ApiResponse<ExpenseApplicationDTO> approveExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        ExpenseApplicationDTO dto = expenseService.approveExpense(id, comment);
        return ApiResponse.success("费用单审批通过", dto);
    }

    @Operation(summary = "审批拒绝", description = "审批人拒绝费用单")
    @SaCheckPermission("erp:expense:approval:process")
    @PostMapping("/{id}/reject")
    public ApiResponse<ExpenseApplicationDTO> rejectExpense(
            @Parameter(description = "费用单ID") @PathVariable Long id,
            @RequestParam String reason) {
        ExpenseApplicationDTO dto = expenseService.rejectExpense(id, reason);
        return ApiResponse.success("费用单审批拒绝", dto);
    }

    @Operation(summary = "费用单列表", description = "获取费用单列表，支持分页和过滤（兼容旧路径）")
    @SaCheckPermission("erp:expense:application:list")
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

    @Operation(summary = "费用统计", description = "获取费用统计数据（兼容旧路径）")
    @SaCheckPermission("erp:expense:statistics:list")
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