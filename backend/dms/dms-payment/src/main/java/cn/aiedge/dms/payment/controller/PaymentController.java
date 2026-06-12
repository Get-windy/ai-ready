package cn.aiedge.dms.payment.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.payment.entity.DmsPayment;
import cn.aiedge.dms.payment.service.PaymentService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 现场收款控制器
 *
 * 提供二维码生成、收款确认、未付标记和收款记录查询等 REST 接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "现场收款")
@RestController
@RequestMapping("/api/dms/payment")
@RequiredArgsConstructor
@SaCheckLogin
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "生成收款二维码")
    @PostMapping("/qrcode")
    @SaCheckPermission("dms:payment:operate")
    public ApiResponse<DmsPayment> generateQrcode(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "收款金额") @RequestParam BigDecimal amount) {
        DmsPayment payment = paymentService.generateQrcode(taskId, amount);
        return ApiResponse.success(payment);
    }

    @Operation(summary = "确认收款")
    @PostMapping("/confirm")
    @SaCheckPermission("dms:payment:operate")
    public ApiResponse<Void> confirmPayment(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "支付方式：1-微信 2-支付宝 3-现金 4-其他") @RequestParam Integer paymentType,
            @Parameter(description = "收款金额") @RequestParam BigDecimal amount,
            @Parameter(description = "外部订单号") @RequestParam(required = false) String externalOrderNo) {
        paymentService.confirmPayment(taskId, paymentType, amount, externalOrderNo);
        return ApiResponse.success();
    }

    @Operation(summary = "标记未付")
    @PostMapping("/mark-unpaid")
    @SaCheckPermission("dms:payment:operate")
    public ApiResponse<Void> markUnpaid(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "未付备注") @RequestParam String remark) {
        paymentService.markUnpaid(taskId, remark);
        return ApiResponse.success();
    }

    @Operation(summary = "获取收款记录")
    @GetMapping("/{taskId}")
    public ApiResponse<DmsPayment> getByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        DmsPayment payment = paymentService.getByTaskId(taskId);
        return ApiResponse.success(payment);
    }
}
