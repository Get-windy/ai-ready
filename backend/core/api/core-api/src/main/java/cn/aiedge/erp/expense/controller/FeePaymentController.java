package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeePaymentCreateRequest;
import cn.aiedge.erp.expense.dto.FeePaymentQueryRequest;
import cn.aiedge.erp.expense.dto.FeePaymentRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 费用付款占位 Controller
 * 数据库表尚未创建，返回空数据避免前端报错
 */
@Tag(name = "费用付款管理（占位）")
@RestController
@RequestMapping("/api/erp/expense/payment")
public class FeePaymentController {

    @Operation(summary = "创建付款记录（占位）")
    @PostMapping
    public ApiResponse<Long> createPayment(@Valid @RequestBody FeePaymentCreateRequest request) {
        return ApiResponse.success(0L);
    }

    @Operation(summary = "确认付款（占位）")
    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirmPayment(@PathVariable Long id) {
        return ApiResponse.success();
    }

    @Operation(summary = "取消付款（占位）")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelPayment(@PathVariable Long id, @RequestParam String reason) {
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询付款记录（占位）")
    @GetMapping("/page")
    public ApiResponse<PageResult<FeePaymentRecordVO>> pageList(FeePaymentQueryRequest request) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize()));
    }

    @Operation(summary = "获取付款记录详情（占位）")
    @GetMapping("/{id}")
    public ApiResponse<FeePaymentRecordVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok((FeePaymentRecordVO) null);
    }

    @Operation(summary = "获取业务单据的付款记录（占位）")
    @GetMapping("/business")
    public ApiResponse<PageResult<FeePaymentRecordVO>> getByBusiness(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, pageNum, pageSize));
    }
}
