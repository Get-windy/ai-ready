package cn.aiedge.erp.expense.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeePaymentCreateRequest;
import cn.aiedge.erp.expense.dto.FeePaymentQueryRequest;
import cn.aiedge.erp.expense.dto.FeePaymentRecordVO;
import cn.aiedge.erp.expense.service.FeePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 费用付款控制器
 */
@Tag(name = "费用付款管理", description = "费用付款确认接口")
@RestController
@RequestMapping("/api/erp/expense/payment")
@RequiredArgsConstructor
public class FeePaymentController {

    private final FeePaymentService feePaymentService;

    @Operation(summary = "创建付款记录")
    @PostMapping
    @RequiresPermission("erp:expense:payment:create")
    public ApiResponse<Long> createPayment(@Valid @RequestBody FeePaymentCreateRequest request) {
        Long id = feePaymentService.createPayment(request);
        return ApiResponse.success(id);
    }

    @Operation(summary = "确认付款")
    @PostMapping("/{id}/confirm")
    @RequiresPermission("erp:expense:payment:confirm")
    public ApiResponse<Void> confirmPayment(@PathVariable Long id) {
        Long confirmUserId = SecurityUtils.getCurrentUserId();
        String confirmUserName = SecurityUtils.getCurrentUsername();
        feePaymentService.confirmPayment(id, confirmUserId, confirmUserName);
        return ApiResponse.success();
    }

    @Operation(summary = "取消付款")
    @PostMapping("/{id}/cancel")
    @RequiresPermission("erp:expense:payment:cancel")
    public ApiResponse<Void> cancelPayment(@PathVariable Long id, @RequestParam String reason) {
        feePaymentService.cancelPayment(id, reason);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询付款记录")
    @GetMapping("/page")
    @RequiresPermission("erp:expense:payment:list")
    public ApiResponse<PageResult<FeePaymentRecordVO>> pageList(FeePaymentQueryRequest request) {
        return ApiResponse.ok(feePaymentService.pageList(request));
    }

    @Operation(summary = "获取付款记录详情")
    @GetMapping("/{id}")
    @RequiresPermission("erp:expense:payment:query")
    public ApiResponse<FeePaymentRecordVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(feePaymentService.getDetail(id));
    }

    @Operation(summary = "获取业务单据的付款记录")
    @GetMapping("/business")
    @RequiresPermission("erp:expense:payment:list")
    public ApiResponse<PageResult<FeePaymentRecordVO>> getByBusiness(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(feePaymentService.getByBusiness(businessType, businessId, pageNum, pageSize));
    }
}
