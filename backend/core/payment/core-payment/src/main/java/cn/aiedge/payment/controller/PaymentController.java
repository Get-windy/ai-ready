package cn.aiedge.payment.controller;

import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.payment.PaymentCallbackVerifier;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.service.PaymentService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
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
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;
    /** 平台提供的验签实现集合；按渠道分发（**无实现 ⇒ 拒绝**，绝不默认放行） */
    private final List<PaymentCallbackVerifier> verifiers;

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

    // ══════════════════════════════════════════════════════════════════════
    // 支付回调（F-07，2026-09-26 改造为 fail-closed）
    //
    // 改造前：`POST /callback/{channel}` 把**原始报文**交给
    // `PaymentServiceImpl.handleCallback` → `PaymentChannel.handleCallback`，
    // 而渠道实现（支付宝/微信/银联/现金/银行）都是 `record.setStatus(2)` 的**无条件成功桩**
    // ⇒ 任何持有登录态的人 POST 一下就能把支付单置为"已支付"（审计 P0-2）。
    //
    // 改造后与 erp-mall 的 C 端回调（2026-09-21 已做）**同构**：
    //   无渠道实现认领 → 401 拒绝；未配置凭据 → 401 拒绝；验签失败 → 401 拒绝；
    //   业务处理失败 → `ackBody(false)` 让网关重推（**不能**回 SUCCESS，否则不再重试）。
    //   只有验签通过后才调 `handleVerifiedCallback` 改单 —— 类型上就绕不过验签。
    // ══════════════════════════════════════════════════════════════════════

    @Operation(summary = "支付回调（需携带租户）",
            description = "渠道服务器调用；按租户取验签凭据，未配置/验签失败一律拒绝（fail-closed）")
    @PostMapping("/callback/{tenantId}/{channel}")
    public ResponseEntity<String> paymentCallback(
            @Parameter(description = "租户ID（凭据按租户取）") @PathVariable Long tenantId,
            @Parameter(description = "渠道码，如 WECHAT / ALIPAY") @PathVariable String channel,
            @RequestBody(required = false) byte[] rawBody,
            HttpServletRequest request) {

        PaymentCallbackVerifier verifier = findVerifier(channel);
        if (verifier == null) {
            log.error("收到未实现渠道的支付回调，已拒绝：tenantId={}, channel={}", tenantId, channel);
            return ResponseEntity.status(401).body("UNSUPPORTED_CHANNEL");
        }
        if (!verifier.isConfigured(tenantId)) {
            log.error("渠道未配置回调验签凭据，已拒绝：tenantId={}, channel={}", tenantId, channel);
            return ResponseEntity.status(401).body(verifier.ackBody(false));
        }

        PaymentCallbackContext context = new PaymentCallbackContext(
                tenantId, channel, rawBody == null ? new byte[0] : rawBody,
                headersOf(request), request.getRemoteAddr());
        try {
            PaymentCallbackResult result = verifier.verify(context);
            paymentService.handleVerifiedCallback(tenantId, channel, result);
            return ResponseEntity.ok(verifier.ackBody(true));
        } catch (PaymentCallbackVerificationException e) {
            // 验签失败：可能是伪造，也可能是我方凭据配错 —— 两种都不该改单
            log.warn("支付回调验签失败：tenantId={}, channel={}, reason={}", tenantId, channel, e.getMessage());
            return ResponseEntity.status(401).body(verifier.ackBody(false));
        } catch (Exception e) {
            // 业务失败（找不到支付请求 / 金额不符）：回失败应答让网关重推
            log.error("支付回调业务处理失败：tenantId={}, channel={}", tenantId, channel, e);
            return ResponseEntity.ok(verifier.ackBody(false));
        }
    }

    /**
     * 旧的无租户路径。
     *
     * <p>保留 URL 以免调用方拿到 404 却不知道原因，但**一律拒绝**：
     * 没有租户就取不到该租户的验签凭据，"验不了"就不能"当成功"。
     * 请改用 {@code /callback/{tenantId}/{channel}}。</p>
     */
    @Operation(summary = "支付回调（旧路径，已停用）",
            description = "缺少租户无法取凭据，一律拒绝；请改用 /callback/{tenantId}/{channel}")
    @PostMapping("/callback/{channel}")
    public ResponseEntity<String> legacyCallback(@PathVariable String channel) {
        log.warn("支付回调走了旧的无租户路径，已拒绝：channel={}", channel);
        return ResponseEntity.status(400)
                .body("TENANT_REQUIRED: 请改用 /api/payment/callback/{tenantId}/" + channel);
    }

    private PaymentCallbackVerifier findVerifier(String channel) {
        if (channel == null) {
            return null;
        }
        return verifiers.stream()
                .filter(v -> v.channelCode() != null && v.channelCode().equalsIgnoreCase(channel))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> headersOf(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, request.getHeader(name));
        }
        return headers;
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