package cn.aiedge.payment.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "支付管理", description = "支付请求、支付记录、支付回调")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付请求")
    @PostMapping("/request")
    public Result<PaymentRequest> createPayment(@RequestBody Map<String, Object> params) {
        String bizType = (String) params.get("bizType");
        Long bizId = Long.valueOf(params.get("bizId").toString());
        String bizNo = (String) params.get("bizNo");
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String channel = (String) params.get("channel");

        PaymentRequest request = paymentService.createPayment(bizType, bizId, bizNo, amount, channel);
        return Result.success(request);
    }

    @Operation(summary = "分页查询支付请求")
    @GetMapping("/request/page")
    public Result<PageResult<PaymentRequest>> pagePaymentRequest(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(paymentService.pagePaymentRequest(pageNum, pageSize, bizType, channel, status));
    }

    @Operation(summary = "查询支付请求详情")
    @GetMapping("/request/{id}")
    public Result<PaymentRequest> getPaymentRequest(@PathVariable Long id) {
        return Result.success(paymentService.getPaymentRequest(id));
    }

    @Operation(summary = "取消支付请求")
    @PostMapping("/request/{id}/cancel")
    public Result<Void> cancelPayment(@PathVariable Long id) {
        paymentService.cancelPayment(id);
        return Result.success();
    }

    @Operation(summary = "支付回调")
    @PostMapping("/callback/{channel}")
    public Result<PaymentRecord> handleCallback(
            @PathVariable String channel,
            @RequestBody String callbackData) {
        PaymentRecord record = paymentService.handleCallback(channel, callbackData);
        return Result.success(record);
    }

    @Operation(summary = "确认线下支付")
    @PostMapping("/request/{id}/confirm")
    public Result<Void> confirmOfflinePayment(
            @PathVariable Long id,
            @RequestParam String channelOrderNo) {
        paymentService.confirmOfflinePayment(id, channelOrderNo);
        return Result.success();
    }

    @Operation(summary = "分页查询支付记录")
    @GetMapping("/record/page")
    public Result<PageResult<PaymentRecord>> pagePaymentRecord(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel) {
        return Result.success(paymentService.pagePaymentRecord(pageNum, pageSize, channel));
    }

    @Operation(summary = "获取可用支付渠道")
    @GetMapping("/channels")
    public Result<List<PaymentService.ChannelInfo>> getAvailableChannels(
            @Parameter(description = "支付金额") @RequestParam BigDecimal amount) {
        return Result.success(paymentService.getAvailableChannels(amount));
    }
}