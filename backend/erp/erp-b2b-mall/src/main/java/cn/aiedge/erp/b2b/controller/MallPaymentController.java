package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.service.MallPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mall/payments")
@Tag(name = "商城支付", description = "支付处理、回调等接口")
@RequiredArgsConstructor
public class MallPaymentController {

    private final MallPaymentService mallPaymentService;

    @Operation(summary = "创建支付", description = "创建支付请求")
    @PostMapping
    public ApiResponse<Void> createPayment(@RequestBody Map<String, Object> paymentRequest) {
        mallPaymentService.createPayment(paymentRequest);
        return ApiResponse.success("支付创建成功", null);
    }

    @Operation(summary = "查询支付状态", description = "查询指定支付的当前状态")
    @GetMapping("/{id}/status")
    public ApiResponse<Void> getPaymentStatus(
            @Parameter(description = "支付ID") @PathVariable String id) {
        mallPaymentService.getPaymentStatus(id);
        return ApiResponse.success("查询成功", null);
    }

    @Operation(summary = "支付回调", description = "支付系统回调处理")
    @PostMapping("/callback")
    public ApiResponse<Void> paymentCallback(@RequestBody Map<String, Object> callbackData) {
        mallPaymentService.handleCallback(callbackData);
        return ApiResponse.success("回调处理成功", null);
    }
}
