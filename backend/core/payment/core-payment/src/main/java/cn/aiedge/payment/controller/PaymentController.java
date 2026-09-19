package cn.aiedge.payment.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.service.PaymentService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 支付管理控制器（支付请求 / 支付记录 / 支付回调）。
 *
 * <p><b>2026-09-18 补权限注解</b>：本控制器的端点原先**全部无权限注解** ——
 * 任何登录用户都能创建 / 取消 / 确认他人的支付请求（开发文档 §12-③ 登记的越权面 P0）。
 * 现按读写语义补 {@code @SaCheckPermission}，权限码见 {@code V11.395.0} 迁移的种子。</p>
 *
 * <p><b>例外（有意不加）</b>：{@code POST /callback/{channel}} 是**渠道侧服务器**调用的回调入口，
 * 调用方没有 Sa-Token 会话，加权限注解没有意义（它会先被登录拦截器挡下）。
 * 该端点的验签与防重放属另一议题，开发文档 §12-⑰ 已如实登记为未核对项，不在本次改动范围。</p>
 */
@Tag(name = "支付管理", description = "支付请求、支付记录、支付回调")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付请求")
    @PostMapping("/request")
    @SaCheckPermission("payment:request:create")
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
    @SaCheckPermission("payment:request:list")
    public Result<PageResult<PaymentRequest>> pagePaymentRequest(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "业务单号（模糊）") @RequestParam(required = false) String bizNo,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "创建时间起") @RequestParam(required = false) String startTime,
            @Parameter(description = "创建时间止") @RequestParam(required = false) String endTime,
            @Parameter(description = "付款人（模糊）") @RequestParam(required = false) String payerName) {
        return Result.success(paymentService.pagePaymentRequest(
                pageNum, pageSize, bizType, bizNo, channel, status, startTime, endTime, payerName));
    }

    @Operation(summary = "支付请求统计（后端聚合）")
    @GetMapping("/request/stat")
    @SaCheckPermission("payment:request:list")
    public Result<Map<String, Object>> statPaymentRequest(
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel) {
        return Result.success(paymentService.statPaymentRequest(channel));
    }

    @Operation(summary = "查询支付请求详情")
    @GetMapping("/request/{id}")
    @SaCheckPermission("payment:request:list")
    public Result<PaymentRequest> getPaymentRequest(@PathVariable Long id) {
        return Result.success(paymentService.getPaymentRequest(id));
    }

    @Operation(summary = "取消支付请求")
    @PostMapping("/request/{id}/cancel")
    @SaCheckPermission("payment:request:cancel")
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
    @SaCheckPermission("payment:request:confirm")
    public Result<Void> confirmOfflinePayment(
            @PathVariable Long id,
            @RequestParam String channelOrderNo) {
        paymentService.confirmOfflinePayment(id, channelOrderNo);
        return Result.success();
    }

    @Operation(summary = "分页查询支付记录")
    @GetMapping("/record/page")
    @SaCheckPermission("payment:record:list")
    public Result<PageResult<PaymentRecord>> pagePaymentRecord(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "渠道订单号（模糊）") @RequestParam(required = false) String channelOrderNo,
            @Parameter(description = "支付（回调）时间起") @RequestParam(required = false) String startTime,
            @Parameter(description = "支付（回调）时间止") @RequestParam(required = false) String endTime) {
        return Result.success(paymentService.pagePaymentRecord(
                pageNum, pageSize, channel, status, channelOrderNo, startTime, endTime));
    }

    @Operation(summary = "支付记录统计（后端聚合）")
    @GetMapping("/record/stat")
    @SaCheckPermission("payment:record:list")
    public Result<Map<String, Object>> statPaymentRecord(
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel) {
        return Result.success(paymentService.statPaymentRecord(channel));
    }

    @Operation(summary = "获取可用支付渠道")
    @GetMapping("/channels")
    @SaCheckPermission("payment:channel:list")
    public Result<List<PaymentService.ChannelInfo>> getAvailableChannels(
            @Parameter(description = "支付金额") @RequestParam BigDecimal amount) {
        return Result.success(paymentService.getAvailableChannels(amount));
    }
}