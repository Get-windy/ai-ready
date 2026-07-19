package cn.aiedge.finance.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.finance.entity.OtherIncomeDoc;
import cn.aiedge.finance.service.OtherIncomeDocService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 其他收入单控制器
 * Ref: Odoo 18.0 account.move - misc income
 */
@RestController
@RequestMapping("/api/finance/other-income-doc")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "其他收入单管理", description = "其他收入单CRUD、审批、作废等操作")
public class OtherIncomeDocController {

    private final OtherIncomeDocService otherIncomeDocService;

    @PostMapping("/page")
    @Operation(summary = "分页查询其他收入单")
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

    @GetMapping("/{id}")
    @Operation(summary = "获取其他收入单详情")
    public ApiResponse<OtherIncomeDoc> getById(@PathVariable Long id) {
        OtherIncomeDoc doc = otherIncomeDocService.getById(id);
        return ApiResponse.success(doc);
    }

    @PostMapping
    @Operation(summary = "创建其他收入单")
    public ApiResponse<OtherIncomeDoc> create(@RequestBody OtherIncomeDoc doc) {
        OtherIncomeDoc created = otherIncomeDocService.createDoc(doc);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新其他收入单")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody OtherIncomeDoc doc) {
        doc.setId(id);
        return ApiResponse.success(otherIncomeDocService.updateById(doc));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交其他收入单")
    public ApiResponse<Boolean> submit(@PathVariable Long id) {
        otherIncomeDocService.submitDoc(id);
        return ApiResponse.success(true);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批其他收入单")
    public ApiResponse<Boolean> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        otherIncomeDocService.approveDoc(id, StpUtil.getLoginIdAsLong(), note);
        return ApiResponse.success(true);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "作废其他收入单")
    public ApiResponse<Boolean> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        otherIncomeDocService.cancelDoc(id, reason);
        return ApiResponse.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除其他收入单")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(otherIncomeDocService.removeById(id));
    }
}
