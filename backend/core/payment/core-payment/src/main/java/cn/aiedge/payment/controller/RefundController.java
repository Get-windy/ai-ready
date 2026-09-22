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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Tag(name = "退款管理", description = "退款请求、退款审批、退款记录")
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

    @Operation(summary = "退款回调")
    @PostMapping("/callback/{channel}")
    public Result<RefundRecord> handleCallback(
            @PathVariable String channel,
            @RequestBody String callbackData) {
        RefundRecord record = refundService.handleCallback(channel, callbackData);
        return Result.success(record);
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