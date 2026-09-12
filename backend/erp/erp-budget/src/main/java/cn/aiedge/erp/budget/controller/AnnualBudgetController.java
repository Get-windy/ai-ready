package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.AnnualBudgetDTO;
import cn.aiedge.erp.budget.dto.AnnualBudgetQuery;
import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.dto.BudgetBatchOpRequest;
import cn.aiedge.erp.budget.service.AnnualBudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/budget/annual")
@Tag(name = "年度预算", description = "年度预算管理")
@RequiredArgsConstructor
public class AnnualBudgetController {

    private final AnnualBudgetService annualBudgetService;

    @Operation(summary = "创建年度预算")
    @PostMapping
    public ApiResponse<AnnualBudgetDTO> create(@Valid @RequestBody AnnualBudgetDTO dto) {
        AnnualBudgetDTO result = annualBudgetService.create(dto);
        return ApiResponse.success("创建成功", result);
    }

    @Operation(summary = "更新年度预算")
    @PutMapping("/{id}")
    public ApiResponse<AnnualBudgetDTO> update(
            @Parameter(description = "预算ID") @PathVariable Long id,
            @Valid @RequestBody AnnualBudgetDTO dto) {
        AnnualBudgetDTO result = annualBudgetService.update(id, dto);
        return ApiResponse.success("更新成功", result);
    }

    @Operation(summary = "删除年度预算")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "预算ID") @PathVariable Long id) {
        annualBudgetService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "获取年度预算详情")
    @GetMapping("/{id}")
    public ApiResponse<AnnualBudgetDTO> getDetail(@Parameter(description = "预算ID") @PathVariable Long id) {
        AnnualBudgetDTO result = annualBudgetService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "多条件分页查询预算编制单")
    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(AnnualBudgetQuery query) {
        return ApiResponse.success(annualBudgetService.pageQuery(query));
    }

    @Operation(summary = "多条件分页查询预算编制单（别名）")
    @GetMapping("/doc-query")
    public ApiResponse<Map<String, Object>> docQuery(AnnualBudgetQuery query) {
        return ApiResponse.success(annualBudgetService.pageQuery(query));
    }

    @Operation(summary = "生成下一预算编制单号")
    @GetMapping("/next-no")
    public ApiResponse<String> nextNo() {
        return ApiResponse.success(annualBudgetService.generateDocNo());
    }

    @Operation(summary = "保存预算编制单（含预算科目明细）")
    @PostMapping("/save")
    public ApiResponse<AnnualBudgetDTO> save(@RequestBody AnnualBudgetDTO dto) {
        return ApiResponse.success("保存成功", annualBudgetService.save(dto));
    }

    @Operation(summary = "批量删除预算编制单（仅草稿/已驳回）")
    @PostMapping("/batch-delete")
    public ApiResponse<Integer> batchDelete(@RequestBody BudgetBatchOpRequest request) {
        return ApiResponse.success("批量删除完成", annualBudgetService.batchDelete(request.getIds()));
    }

    @Operation(summary = "批量提交审批")
    @PostMapping("/batch-submit")
    public ApiResponse<Integer> batchSubmit(@RequestBody BudgetBatchOpRequest request) {
        return ApiResponse.success("批量提交完成", annualBudgetService.batchSubmit(request.getIds()));
    }

    @Operation(summary = "批量审批通过")
    @PostMapping("/batch-approve")
    public ApiResponse<Integer> batchApprove(@RequestBody BudgetBatchOpRequest request) {
        return ApiResponse.success("批量审批完成", annualBudgetService.batchApprove(
                request.getIds(), request.getAuditorId(), request.getAuditorName(), request.getAuditRemark()));
    }

    @Operation(summary = "批量驳回")
    @PostMapping("/batch-reject")
    public ApiResponse<Integer> batchReject(@RequestBody BudgetBatchOpRequest request) {
        return ApiResponse.success("批量驳回完成", annualBudgetService.batchReject(
                request.getIds(), request.getAuditorId(), request.getAuditorName(), request.getAuditRemark()));
    }

    @Operation(summary = "打印次数 +1")
    @PostMapping("/{id}/print")
    public ApiResponse<Void> print(@PathVariable Long id) {
        annualBudgetService.increasePrintCount(id);
        return ApiResponse.success("已记录打印", null);
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    public ApiResponse<AnnualBudgetDTO> submit(@Parameter(description = "预算ID") @PathVariable Long id) {
        AnnualBudgetDTO result = annualBudgetService.submit(id);
        return ApiResponse.success("已提交审批", result);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public ApiResponse<AnnualBudgetDTO> approve(@Parameter(description = "预算ID") @PathVariable Long id) {
        AnnualBudgetDTO result = annualBudgetService.approve(id);
        return ApiResponse.success("审批通过", result);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    public ApiResponse<AnnualBudgetDTO> reject(@Parameter(description = "预算ID") @PathVariable Long id) {
        AnnualBudgetDTO result = annualBudgetService.reject(id);
        return ApiResponse.success("已拒绝", result);
    }

    @Operation(summary = "开始执行")
    @PostMapping("/{id}/start-exec")
    public ApiResponse<AnnualBudgetDTO> startExec(@Parameter(description = "预算ID") @PathVariable Long id) {
        AnnualBudgetDTO result = annualBudgetService.startExec(id);
        return ApiResponse.success("已开始执行", result);
    }

    @Operation(summary = "关闭预算")
    @PostMapping("/{id}/close")
    public ApiResponse<AnnualBudgetDTO> close(@Parameter(description = "预算ID") @PathVariable Long id) {
        AnnualBudgetDTO result = annualBudgetService.close(id);
        return ApiResponse.success("已关闭", result);
    }

    @Operation(summary = "导出年度预算列表")
    @GetMapping("/export")
    public ApiResponse<List<AnnualBudgetDTO>> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        List<AnnualBudgetDTO> list = annualBudgetService.exportList(keyword, fiscalYear, departmentId, status);
        return ApiResponse.success(list);
    }
}
