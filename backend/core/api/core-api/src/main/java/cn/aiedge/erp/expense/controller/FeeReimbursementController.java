package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeReimbursementCreateRequest;
import cn.aiedge.erp.expense.dto.FeeReimbursementQueryRequest;
import cn.aiedge.erp.expense.dto.FeeReimbursementVO;
import cn.aiedge.erp.expense.service.FeeReimbursementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 费用报销控制器
 */
@Tag(name = "费用报销管理", description = "费用报销增删改查、提交审批接口")
@RestController
@RequestMapping("/api/erp/expense/reimbursement")
@RequiredArgsConstructor
public class FeeReimbursementController {

    private final FeeReimbursementService feeReimbursementService;

    @Operation(summary = "分页查询费用报销")
    @GetMapping("/page")
    @RequiresPermission("erp:expense:reimbursement:list")
    public ApiResponse<PageResult<FeeReimbursementVO>> pageList(FeeReimbursementQueryRequest request) {
        return ApiResponse.ok(feeReimbursementService.pageList(request));
    }

    @Operation(summary = "获取费用报销详情")
    @GetMapping("/{id}")
    @RequiresPermission("erp:expense:reimbursement:query")
    public ApiResponse<FeeReimbursementVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(feeReimbursementService.getDetail(id));
    }

    @Operation(summary = "创建费用报销")
    @PostMapping
    @RequiresPermission("erp:expense:reimbursement:create")
    public ApiResponse<Long> create(@Valid @RequestBody FeeReimbursementCreateRequest request) {
        Long id = feeReimbursementService.create(request);
        return ApiResponse.success(id);
    }

    @Operation(summary = "更新费用报销")
    @PutMapping("/{id}")
    @RequiresPermission("erp:expense:reimbursement:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FeeReimbursementCreateRequest request) {
        feeReimbursementService.update(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除费用报销")
    @DeleteMapping("/{id}")
    @RequiresPermission("erp:expense:reimbursement:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        feeReimbursementService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @RequiresPermission("erp:expense:reimbursement:submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        feeReimbursementService.submit(id);
        return ApiResponse.success();
    }

    @Operation(summary = "撤回申请")
    @PostMapping("/{id}/withdraw")
    @RequiresPermission("erp:expense:reimbursement:submit")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        feeReimbursementService.withdraw(id);
        return ApiResponse.success();
    }
}
