package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.PaymentCreateRequest;
import cn.aiedge.finance.dto.PaymentQueryRequest;
import cn.aiedge.finance.dto.PaymentVO;
import cn.aiedge.finance.service.IPaymentService;
import cn.aiedge.common.result.ApiResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 付款记录控制器
 */
@RestController
@RequestMapping("/api/finance/payment")
@Tag(name = "付款记录管理", description = "付款记录相关操作接口")
@SaCheckLogin
public class PaymentController {

    private final IPaymentService paymentService;

    public PaymentController(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建付款记录")
    public ApiResponse<Long> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        Long id = paymentService.createPayment(request);
        return ApiResponse.success(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取付款记录详情")
    public ApiResponse<PaymentVO> getPaymentById(@PathVariable Long id) {
        PaymentVO paymentVO = paymentService.getPaymentById(id);
        return ApiResponse.success(paymentVO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除付款记录")
    public ApiResponse<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询付款记录")
    public ApiResponse<PageResult<PaymentVO>> pagePayments(@RequestBody PaymentQueryRequest request) {
        Page<PaymentVO> pageResult = paymentService.pagePayments(request);
        PageResult<PaymentVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除付款记录")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Long> ids) {
        paymentService.removeBatchByIds(ids);
        return ApiResponse.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出付款记录列表")
    public ApiResponse<List<PaymentVO>> export(
            @Parameter(description = "应付账款ID") @RequestParam(required = false) Long payableId,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "付款日期起") @RequestParam(required = false) LocalDate paymentDateStart,
            @Parameter(description = "付款日期止") @RequestParam(required = false) LocalDate paymentDateEnd) {
        PaymentQueryRequest request = new PaymentQueryRequest();
        request.setPayableId(payableId);
        request.setSupplierId(supplierId);
        request.setStatus(status);
        request.setPaymentDateStart(paymentDateStart);
        request.setPaymentDateEnd(paymentDateEnd);
        request.setPageNum(1);
        request.setPageSize(Integer.MAX_VALUE);
        Page<PaymentVO> pageResult = paymentService.pagePayments(request);
        return ApiResponse.success(pageResult.getRecords());
    }
}
