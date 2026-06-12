package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeApplicationCreateRequest;
import cn.aiedge.erp.expense.dto.FeeApplicationQueryRequest;
import cn.aiedge.erp.expense.dto.FeeApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 费用申请占位 Controller
 * 数据库表尚未创建，返回空数据避免前端报错
 */
@Tag(name = "费用申请管理（占位）")
@RestController
@RequestMapping("/api/erp/expense/application")
public class FeeApplicationController {

    @Operation(summary = "分页查询费用申请（占位）")
    @GetMapping("/page")
    public ApiResponse<PageResult<FeeApplicationVO>> pageList(FeeApplicationQueryRequest request) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize()));
    }

    @Operation(summary = "获取费用申请详情（占位）")
    @GetMapping("/{id}")
    public ApiResponse<FeeApplicationVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok((FeeApplicationVO) null);
    }

    @Operation(summary = "创建费用申请（占位）")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody FeeApplicationCreateRequest request) {
        return ApiResponse.success(0L);
    }

    @Operation(summary = "更新费用申请（占位）")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FeeApplicationCreateRequest request) {
        return ApiResponse.success();
    }

    @Operation(summary = "删除费用申请（占位）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return ApiResponse.success();
    }

    @Operation(summary = "提交审批（占位）")
    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        return ApiResponse.success();
    }

    @Operation(summary = "撤回申请（占位）")
    @PostMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        return ApiResponse.success();
    }

    @Operation(summary = "获取我的申请（占位）")
    @GetMapping("/my")
    public ApiResponse<PageResult<FeeApplicationVO>> myApplications(FeeApplicationQueryRequest request) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize()));
    }

    @Operation(summary = "获取待我审批（占位）")
    @GetMapping("/my-pending")
    public ApiResponse<PageResult<FeeApplicationVO>> myPendingApprovals(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, pageNum, pageSize));
    }
}
