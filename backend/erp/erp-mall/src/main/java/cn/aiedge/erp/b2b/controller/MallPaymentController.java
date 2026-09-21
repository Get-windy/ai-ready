package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.payment.PaymentCallbackVerifier;
import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.service.MallPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商城支付控制器。
 *
 * <h2>2026-09-21 回调端点重做（原实现有两个问题）</h2>
 * <ol>
 *   <li><b>无验签</b>：原 {@code POST /callback} 直接吃 {@code Map} 报文，
 *       只要 {@code out_trade_no} + {@code trade_status=SUCCESS} 就把订单标成已支付。
 *       该端点当时因未登录被 Sa-Token 拦成 401 而不可利用，但**一旦有人为「让网关能回调」
 *       把它加进匿名白名单，就立刻变成「任何人 POST 一个单号即可白嫖」**。</li>
 *   <li><b>应答体不符合渠道要求</b>：原实现返回平台自己的 {@code ApiResponse} JSON，
 *       而微信要求应答 {@code SUCCESS}、支付宝要求 {@code success}，
 *       否则网关会当成失败**无限重推**。</li>
 * </ol>
 *
 * <h2>现在的口径</h2>
 * <ul>
 *   <li>回调路径携带 <b>租户 + 渠道</b>：{@code POST /api/v1/mall/payments/callback/{tenantId}/{channel}}。
 *       渠道由租户自选（微信 / 支付宝 / 都接），凭据按租户取 —— 故租户必须出现在路径上
 *       （同时也让每个租户可以配置各自的 notify_url）。</li>
 *   <li>验签交给 {@link PaymentCallbackVerifier} 的某个实现；
 *       **没有任何实现认领该渠道 → 一律拒绝**（fail-closed），不会「默认放行」。</li>
 *   <li>验签失败 → 401；业务失败（订单不存在 / 金额不符）→ 返回该渠道的**失败应答**，
 *       让网关按自己的策略重推。</li>
 * </ul>
 *
 * <h2>⚠️ 启用步骤（三步，缺一不可）</h2>
 * <ol>
 *   <li>在 {@code core-payment} 实现 {@link PaymentCallbackVerifier}（微信 APIv3 或 支付宝 RSA2），
 *       注册成 Spring Bean；</li>
 *   <li>为租户配置该渠道的商户号与密钥，并让 {@code isConfigured(tenantId)} 返回 true；</li>
 *   <li><b>最后</b>才把 {@code /api/v1/mall/payments/callback/**} 加进
 *       {@code SaTokenConfig} 的匿名白名单 —— 顺序反过来就是在没有验签的前提下敞开端点。</li>
 * </ol>
 * 当前处于第 0 步（无任何实现），故本端点**故意不在白名单里**，外部不可达。
 */
@Slf4j
@Tag(name = "商城支付", description = "支付处理、回调等接口")
@RestController
@RequestMapping("/api/v1/mall/payments")
@RequiredArgsConstructor
public class MallPaymentController {

    private final MallPaymentService mallPaymentService;

    /** 平台提供的验签实现集合；按渠道分发（无实现 ⇒ 拒绝） */
    private final List<PaymentCallbackVerifier> verifiers;

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

    /**
     * 支付渠道回调。
     *
     * <p>返回类型是 {@code ResponseEntity<String>} 而非 {@code ApiResponse} ——
     * 应答体必须由渠道实现决定（微信 {@code SUCCESS} / 支付宝 {@code success}），
     * 包成平台自己的 JSON 会被网关当成失败。</p>
     */
    @Operation(summary = "支付回调", description = "支付系统回调处理（需携带租户与渠道）")
    @PostMapping("/callback/{tenantId}/{channel}")
    public ResponseEntity<String> paymentCallback(
            @Parameter(description = "租户ID（渠道由租户自选，凭据按租户取）") @PathVariable Long tenantId,
            @Parameter(description = "渠道码，如 WECHAT / ALIPAY") @PathVariable String channel,
            @RequestBody byte[] rawBody,
            HttpServletRequest request) {

        PaymentCallbackVerifier verifier = verifiers.stream()
                .filter(v -> v.channelCode() != null && v.channelCode().equalsIgnoreCase(channel))
                .findFirst()
                .orElse(null);

        if (verifier == null) {
            // fail-closed：没有任何实现认领该渠道。绝不「默认放行」——
            // 那正是原实现「无验签即改单」的翻版。
            log.error("收到未实现渠道的支付回调，已拒绝：tenantId={}, channel={}", tenantId, channel);
            return ResponseEntity.status(401).body("UNSUPPORTED_CHANNEL");
        }
        if (!verifier.isConfigured(tenantId)) {
            log.error("渠道未配置凭据，已拒绝回调：tenantId={}, channel={}", tenantId, channel);
            return ResponseEntity.status(401).body(verifier.ackBody(false));
        }

        PaymentCallbackContext context = new PaymentCallbackContext(
                tenantId, channel, rawBody, headersOf(request), request.getRemoteAddr());

        try {
            PaymentCallbackResult result = verifier.verify(context);
            mallPaymentService.handleVerifiedCallback(tenantId, result);
            return ResponseEntity.ok(verifier.ackBody(true));
        } catch (PaymentCallbackVerificationException e) {
            // 验签失败：可能是伪造，也可能是我方密钥配错。两种都不该改单。
            log.warn("支付回调验签失败：tenantId={}, channel={}, reason={}", tenantId, channel, e.getMessage());
            return ResponseEntity.status(401).body(verifier.ackBody(false));
        } catch (Exception e) {
            // 业务失败（订单不存在 / 金额不符）：返回失败应答让网关重推。
            // ⚠️ 不能返回 ackBody(true) —— 那会让网关认为已处理成功而不再重试，订单永远停在未支付。
            log.error("支付回调业务处理失败：tenantId={}, channel={}", tenantId, channel, e);
            return ResponseEntity.ok(verifier.ackBody(false));
        }
    }

    private Map<String, String> headersOf(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
