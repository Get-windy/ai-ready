package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.ReconciliationCreateRequest;
import cn.aiedge.finance.dto.ReconciliationUpdateRequest;
import cn.aiedge.finance.dto.ReconciliationQueryRequest;
import cn.aiedge.finance.dto.ReconciliationVO;
import cn.aiedge.finance.service.IReconciliationService;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账记录控制器
 */
@RestController
@RequestMapping("/api/finance/reconciliation")
@Tag(name = "对账记录管理", description = "对账记录相关操作接口")
@SaCheckLogin
public class ReconciliationController {

    private final IReconciliationService reconciliationService;

    public ReconciliationController(IReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建对账记录")
    public ApiResponse<Long> createReconciliation(@Valid @RequestBody ReconciliationCreateRequest request) {
        Long id = reconciliationService.createReconciliation(request);
        return ApiResponse.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新对账记录")
    public ApiResponse<Void> updateReconciliation(@Valid @RequestBody ReconciliationUpdateRequest request) {
        reconciliationService.updateReconciliation(request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取对账记录详情")
    public ApiResponse<ReconciliationVO> getReconciliationById(@PathVariable Long id) {
        ReconciliationVO reconciliationVO = reconciliationService.getReconciliationById(id);
        return ApiResponse.success(reconciliationVO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除对账记录")
    public ApiResponse<Void> deleteReconciliation(@PathVariable Long id) {
        reconciliationService.deleteReconciliation(id);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询对账记录")
    public ApiResponse<PageResult<ReconciliationVO>> pageReconciliations(@RequestBody ReconciliationQueryRequest request) {
        Page<ReconciliationVO> pageResult = reconciliationService.pageReconciliations(request);
        PageResult<ReconciliationVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @PostMapping("/reconcile/{id}")
    @Operation(summary = "执行对账")
    public ApiResponse<Void> reconcile(@PathVariable Long id) {
        reconciliationService.reconcile(id);
        return ApiResponse.success();
    }

    @PostMapping("/handle-difference/{id}")
    @Operation(summary = "处理差异")
    public ApiResponse<Void> handleDifference(@PathVariable Long id, @RequestParam String differenceReason) {
        reconciliationService.handleDifference(id, differenceReason);
        return ApiResponse.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除对账记录")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Long> ids) {
        reconciliationService.removeBatchByIds(ids);
        return ApiResponse.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出对账记录列表")
    public ApiResponse<List<ReconciliationVO>> export(
            @Parameter(description = "对账类型") @RequestParam(required = false) String reconciliationType,
            @Parameter(description = "目标ID") @RequestParam(required = false) Long targetId,
            @Parameter(description = "目标名称") @RequestParam(required = false) String targetName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期起") @RequestParam(required = false) LocalDate startDateStart,
            @Parameter(description = "开始日期止") @RequestParam(required = false) LocalDate startDateEnd,
            @Parameter(description = "结束日期起") @RequestParam(required = false) LocalDate endDateStart,
            @Parameter(description = "结束日期止") @RequestParam(required = false) LocalDate endDateEnd) {
        ReconciliationQueryRequest request = new ReconciliationQueryRequest();
        request.setReconciliationType(reconciliationType);
        request.setTargetId(targetId);
        request.setTargetName(targetName);
        request.setStatus(status);
        request.setStartDateStart(startDateStart);
        request.setStartDateEnd(startDateEnd);
        request.setEndDateStart(endDateStart);
        request.setEndDateEnd(endDateEnd);
        request.setPageNum(1);
        request.setPageSize(Integer.MAX_VALUE);
        Page<ReconciliationVO> pageResult = reconciliationService.pageReconciliations(request);
        return ApiResponse.success(pageResult.getRecords());
    }
}
