package cn.aiedge.payment.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import cn.aiedge.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Tag(name = "退款管理", description = "退款请求、退款审批、退款记录")
@Slf4j
@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
@Validated
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "创建退款请求")
    @SaCheckPermission("payment:refund:create")
    @PostMapping("/request")
    public Result<RefundRequest> createRefund(@RequestBody Map<String, Object> params) {
        Long paymentId = Long.valueOf(params.get("paymentId").toString());
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String reason = (String) params.get("reason");

        RefundRequest request = refundService.createRefund(paymentId, amount, reason);
        return Result.success(request);
    }

    @Operation(summary = "分页查询退款请求")
    @SaCheckPermission("payment:refund:list")
    @GetMapping("/request/page")
    public Result<PageResult<RefundRequest>> pageRefundRequest(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "单号（渠道退款号 / 支付请求ID）") @RequestParam(required = false) String refundNo,
            @Parameter(description = "退款（申请）日期起") @RequestParam(required = false) String startTime,
            @Parameter(description = "退款（申请）日期止") @RequestParam(required = false) String endTime,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel) {
        return Result.success(refundService.pageRefundRequest(
                pageNum, pageSize, status, refundNo, startTime, endTime, channel));
    }

    @Operation(summary = "退款请求统计（后端聚合）")
    @SaCheckPermission("payment:refund:view")
    @GetMapping("/request/stat")
    public Result<Map<String, Object>> statRefundRequest(
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel) {
        return Result.success(refundService.statRefundRequest(channel));
    }

    @Operation(summary = "查询退款请求详情")
    @SaCheckPermission("payment:refund:detail")
    @GetMapping("/request/{id}")
    public Result<RefundRequest> getRefundRequest(@PathVariable Long id) {
        return Result.success(refundService.getRefundRequest(id));
    }

    @Operation(summary = "审批退款")
    @SaCheckPermission("payment:refund:approve")
    @PostMapping("/request/{id}/approve")
    public Result<Void> approveRefund(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String remark) {
        refundService.approveRefund(id, approved, remark);
        return Result.success();
    }

    /**
     * 退款回调 —— **已停用**（2026-09-26，F-07 同一批处置）。
     *
     * <p>原实现把原始报文交给 `RefundServiceImpl.handleCallback` → `PaymentChannel.queryRefund`，
     * 而各渠道的 queryRefund 都是硬编码返回（`CashChannel` 甚至直接 `setStatus(1)` 注释写着
     * "线下退款默认成功"）⇒ 任何登录用户 POST 一下就能把退款单置为"已退款"。
     * 与支付回调是同一个洞。</p>
     *
     * <p>支付侧已有 `PaymentCallbackVerifier` SPI（含支付宝/微信/银联三家实现），
     * 但**退款验签没有对应的 SPI**，本轮不臆造。因此这里直接 fail-closed：
     * 退款一律走人工核销（`approveRefund`），回调入口拒绝。</p>
     */
    @Operation(summary = "退款回调（已停用）",
            description = "退款验签 SPI 未建设，回调一律拒绝；退款请走人工核销")
    @PostMapping("/callback/{channel}")
    public ResponseEntity<String> callback(@PathVariable String channel) {
        log.warn("收到退款回调但退款验签未实现，已拒绝：channel={}", channel);
        return ResponseEntity.status(501)
                .body("REFUND_CALLBACK_NOT_IMPLEMENTED: 退款回调验签未建设，请走人工核销");
    }

    @Operation(summary = "分页查询退款记录")
    @SaCheckPermission("payment:refund:list")
    @GetMapping("/record/page")
    public Result<PageResult<RefundRecord>> pageRefundRecord(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String channel) {
        return Result.success(refundService.pageRefundRecord(pageNum, pageSize, channel));
    }
}