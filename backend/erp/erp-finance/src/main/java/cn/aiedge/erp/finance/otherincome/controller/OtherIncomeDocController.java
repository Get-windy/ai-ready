package cn.aiedge.erp.finance.otherincome.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.finance.otherincome.entity.OtherIncomeDoc;
import cn.aiedge.erp.finance.otherincome.service.OtherIncomeDocService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 其他收入单控制器
 * Ref: Odoo 18.0 account.move - misc income
 */
@Tag(name = "其他收入单管理", description = "其他收入单CRUD、审批、作废等操作")
@RestController
@RequestMapping("/api/erp/finance/other-income-doc")
@RequiredArgsConstructor
public class OtherIncomeDocController {

    private final OtherIncomeDocService otherIncomeDocService;

    @Operation(summary = "分页查询其他收入单")
    @PostMapping("/page")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/page', 'finance:other-income-doc:view')")
    @OperationLog(module = "其他收入单", type = "QUERY", desc = "分页查询其他收入单")
    public ApiResponse<PageResult<OtherIncomeDoc>> page(@RequestBody Map<String, Object> params) {
        String docNo = (String) params.get("docNo");
        String incomeType = (String) params.get("incomeType");
        Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
        LocalDate startDate = params.get("startDate") != null ? LocalDate.parse(params.get("startDate").toString()) : null;
        LocalDate endDate = params.get("endDate") != null ? LocalDate.parse(params.get("endDate").toString()) : null;
        int pageNum = params.get("pageNum") != null ? ((Number) params.get("pageNum")).intValue() : 1;
        int pageSize = params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 20;

        Page<OtherIncomeDoc> page = otherIncomeDocService.pageList(docNo, incomeType, status, startDate, endDate, pageNum, pageSize);
        PageResult<OtherIncomeDoc> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取其他收入单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/view', 'finance:other-income-doc:view')")
    @OperationLog(module = "其他收入单", type = "QUERY", desc = "获取其他收入单详情")
    public ApiResponse<OtherIncomeDoc> getById(@PathVariable Long id) {
        OtherIncomeDoc doc = otherIncomeDocService.getById(id);
        return ApiResponse.success(doc);
    }

    @Operation(summary = "创建其他收入单")
    @PostMapping
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/create', 'finance:other-income-doc:create')")
    @OperationLog(module = "其他收入单", type = "CREATE", desc = "创建其他收入单")
    public ApiResponse<OtherIncomeDoc> create(@RequestBody OtherIncomeDoc doc) {
        OtherIncomeDoc created = otherIncomeDocService.createDoc(doc);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新其他收入单")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/update', 'finance:other-income-doc:update')")
    @OperationLog(module = "其他收入单", type = "UPDATE", desc = "更新其他收入单")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody OtherIncomeDoc doc) {
        doc.setId(id);
        return ApiResponse.success(otherIncomeDocService.updateById(doc));
    }

    @Operation(summary = "提交其他收入单")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/submit', 'finance:other-income-doc:update')")
    @OperationLog(module = "其他收入单", type = "UPDATE", desc = "提交其他收入单")
    public ApiResponse<Boolean> submit(@PathVariable Long id) {
        otherIncomeDocService.submitDoc(id);
        return ApiResponse.success(true);
    }

    @Operation(summary = "审批其他收入单")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/approve', 'finance:other-income-doc:approve')")
    @OperationLog(module = "其他收入单", type = "UPDATE", desc = "审批其他收入单")
    public ApiResponse<Boolean> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        otherIncomeDocService.approveDoc(id, StpUtil.getLoginIdAsLong(), note);
        return ApiResponse.success(true);
    }

    @Operation(summary = "作废其他收入单")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/cancel', 'finance:other-income-doc:update')")
    @OperationLog(module = "其他收入单", type = "UPDATE", desc = "作废其他收入单")
    public ApiResponse<Boolean> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        otherIncomeDocService.cancelDoc(id, reason);
        return ApiResponse.success(true);
    }

    @Operation(summary = "删除其他收入单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/other-income-doc/delete', 'finance:other-income-doc:delete')")
    @OperationLog(module = "其他收入单", type = "DELETE", desc = "删除其他收入单")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(otherIncomeDocService.removeById(id));
    }
}
