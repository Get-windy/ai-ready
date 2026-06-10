package cn.aiedge.erp.expense.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeApplicationCreateRequest;
import cn.aiedge.erp.expense.dto.FeeApplicationQueryRequest;
import cn.aiedge.erp.expense.dto.FeeApplicationVO;
import cn.aiedge.erp.expense.service.FeeApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 费用申请控制器
 */
@Tag(name = "费用申请管理", description = "费用申请增删改查、提交审批接口")
@RestController
@RequestMapping("/api/erp/expense/application")
@RequiredArgsConstructor
public class FeeApplicationController {

    private final FeeApplicationService feeApplicationService;

    @Operation(summary = "分页查询费用申请")
    @GetMapping("/page")
    @RequiresPermission("erp:expense:application:list")
    public ApiResponse<PageResult<FeeApplicationVO>> pageList(FeeApplicationQueryRequest request) {
        return ApiResponse.ok(feeApplicationService.pageList(request));
    }

    @Operation(summary = "获取费用申请详情")
    @GetMapping("/{id}")
    @RequiresPermission("erp:expense:application:query")
    public ApiResponse<FeeApplicationVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(feeApplicationService.getDetail(id));
    }

    @Operation(summary = "创建费用申请")
    @PostMapping
    @RequiresPermission("erp:expense:application:create")
    public ApiResponse<Long> create(@Valid @RequestBody FeeApplicationCreateRequest request) {
        Long id = feeApplicationService.create(request);
        return ApiResponse.success(id);
    }

    @Operation(summary = "更新费用申请")
    @PutMapping("/{id}")
    @RequiresPermission("erp:expense:application:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FeeApplicationCreateRequest request) {
        feeApplicationService.update(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除费用申请")
    @DeleteMapping("/{id}")
    @RequiresPermission("erp:expense:application:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        feeApplicationService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @RequiresPermission("erp:expense:application:submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        feeApplicationService.submit(id);
        return ApiResponse.success();
    }

    @Operation(summary = "撤回申请")
    @PostMapping("/{id}/withdraw")
    @RequiresPermission("erp:expense:application:submit")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        feeApplicationService.withdraw(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取我的申请")
    @GetMapping("/my")
    @RequiresPermission("erp:expense:application:list")
    public ApiResponse<PageResult<FeeApplicationVO>> myApplications(FeeApplicationQueryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(feeApplicationService.getMyApplications(request, currentUserId));
    }

    @Operation(summary = "获取待我审批")
    @GetMapping("/my-pending")
    @RequiresPermission("erp:expense:application:approve")
    public ApiResponse<PageResult<FeeApplicationVO>> myPendingApprovals(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(feeApplicationService.getMyPendingApprovals(currentUserId, pageNum, pageSize));
    }
}
