package cn.aiedge.finance.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.ReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应收账款控制器
 */
@Tag(name = "应收账款管理", description = "应收账款增删改查接口")
@RestController
@RequestMapping("/api/v1/finance/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    @Operation(summary = "分页查询应收账款")
    @GetMapping("/page")
    @RequiresPermission("finance:receivable:list")
    public ApiResponse<PageResult<ReceivableVO>> pageList(ReceivableQueryRequest request) {
        return ApiResponse.ok(receivableService.pageList(request));
    }

    @Operation(summary = "获取应收账款详情")
    @GetMapping("/{id}")
    @RequiresPermission("finance:receivable:query")
    public ApiResponse<ReceivableVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(receivableService.getDetail(id));
    }

    @Operation(summary = "创建应收账款")
    @PostMapping
    @RequiresPermission("finance:receivable:create")
    public ApiResponse<Long> create(@RequestBody ReceivableCreateRequest request) {
        Long receivableId = receivableService.create(request);
        return ApiResponse.success(receivableId);
    }

    @Operation(summary = "更新应收账款")
    @PutMapping
    @RequiresPermission("finance:receivable:edit")
    public ApiResponse<Void> update(@RequestBody ReceivableUpdateRequest request) {
        receivableService.update(request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除应收账款")
    @DeleteMapping("/{id}")
    @RequiresPermission("finance:receivable:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        receivableService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "收款操作")
    @PostMapping("/receive-payment")
    @RequiresPermission("finance:receivable:payment")
    public ApiResponse<Void> receivePayment(@RequestBody ReceiptCreateRequest request) {
        receivableService.receivePayment(request);
        return ApiResponse.success();
    }

    @Operation(summary = "账龄分析")
    @GetMapping("/aging-analysis")
    @RequiresPermission("finance:receivable:analysis")
    public ApiResponse<List<AgingAnalysisVO>> agingAnalysis() {
        return ApiResponse.ok(receivableService.analyzeAging());
    }

    @Operation(summary = "根据客户查询应收账款")
    @GetMapping("/customer/{customerId}")
    @RequiresPermission("finance:receivable:list")
    public ApiResponse<List<ReceivableVO>> getByCustomer(@PathVariable Long customerId) {
        return ApiResponse.ok(receivableService.getByCustomer(customerId));
    }
}
