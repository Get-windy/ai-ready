package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.dto.BudgetTemplateDTO;
import cn.aiedge.erp.budget.service.BudgetTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/budget/template")
@Tag(name = "预算模板", description = "预算模板管理")
@RequiredArgsConstructor
public class BudgetTemplateController {

    private final BudgetTemplateService budgetTemplateService;

    @Operation(summary = "创建预算模板")
    @PostMapping
    public ApiResponse<BudgetTemplateDTO> create(@Valid @RequestBody BudgetTemplateDTO dto) {
        BudgetTemplateDTO result = budgetTemplateService.create(dto);
        return ApiResponse.success("创建成功", result);
    }

    @Operation(summary = "更新预算模板")
    @PutMapping("/{id}")
    public ApiResponse<BudgetTemplateDTO> update(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Valid @RequestBody BudgetTemplateDTO dto) {
        BudgetTemplateDTO result = budgetTemplateService.update(id, dto);
        return ApiResponse.success("更新成功", result);
    }

    @Operation(summary = "删除预算模板")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "模板ID") @PathVariable Long id) {
        budgetTemplateService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "获取预算模板详情")
    @GetMapping("/{id}")
    public ApiResponse<BudgetTemplateDTO> getDetail(@Parameter(description = "模板ID") @PathVariable Long id) {
        BudgetTemplateDTO result = budgetTemplateService.getById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "分页查询预算模板列表")
    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = budgetTemplateService.page(keyword, fiscalYear, status, page, size);
        return ApiResponse.success(result);
    }

    @Operation(summary = "发布模板")
    @PostMapping("/{id}/publish")
    public ApiResponse<BudgetTemplateDTO> publish(@Parameter(description = "模板ID") @PathVariable Long id) {
        BudgetTemplateDTO result = budgetTemplateService.publish(id);
        return ApiResponse.success("发布成功", result);
    }

    @Operation(summary = "按年度查询模板列表")
    @GetMapping("/list-by-year")
    public ApiResponse<List<BudgetTemplateDTO>> listByYear(
            @Parameter(description = "财政年度") @RequestParam Integer fiscalYear) {
        List<BudgetTemplateDTO> list = budgetTemplateService.listByFiscalYear(fiscalYear);
        return ApiResponse.success(list);
    }

    @Operation(summary = "批量删除预算模板")
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        budgetTemplateService.batchDelete(ids);
        return ApiResponse.success("批量删除成功", null);
    }

    @Operation(summary = "导出预算模板列表")
    @GetMapping("/export")
    public ApiResponse<List<BudgetTemplateDTO>> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        List<BudgetTemplateDTO> list = budgetTemplateService.exportList(keyword, fiscalYear, status);
        return ApiResponse.success(list);
    }
}
