package cn.aiedge.payment.channel;

import cn.aiedge.payment.crypto.Rsa2;
import cn.aiedge.payment.dto.PaymentChannelParam;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.RefundRecord;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.support.ChannelCredentialAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 支付宝渠道适配器（**真实实现**，2026-09-26 由桩改造）。
 *
 * <h2>接口口径（依据支付宝开放平台文档）</h2>
 * <ul>
 *   <li>下单：{@code alipay.trade.page.pay}（电脑网站支付）—— 返回**收银台 URL**，
 *       参数在 query string 里，客户端直接跳转即可；</li>
 *   <li>查单：{@code alipay.trade.query}（按 {@code out_trade_no}）；</li>
 *   <li>关单：{@code alipay.trade.close}；</li>
 *   <li>退款：{@code alipay.trade.refund}（幂等键 {@code out_request_no}）；</li>
 *   <li>退款查询：{@code alipay.trade.fastpay.refund.query}。</li>
 * </ul>
 *
 * <h2>签名/验签</h2>
 * 用**应用私钥**做 {@code SHA256withRSA}（RSA2），待签串 = 除 {@code sign}/{@code sign_type}
 * 外的非空参数按名升序、{@code k=v} 以 {@code &} 连接；响应验签用**支付宝公钥**。
 * 两者都走 {@link Rsa2} —— 与回调验签器（{@code AlipayCallbackVerifier}）**同一套实现**，
 * 保证「我们签出去的，按同一口径一定验得过」。
 *
 * <h2>⚠️ 边界（如实说明）</h2>
 * 报文构造与签名按开放平台文档实现，但**未经真实网关联调**（无商户号/证书，构建环境不出网）。
 * 自证方式见 {@code AlipayChannelTest}：用自生成密钥对走「签名 → 按验签规则验签」的闭合回路。
 * 上生产前必须用沙箱密钥跑一次真实下单 + 回调。**别把"实现了"当成"已验证能收款"。**
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayChannel implements PaymentChannel {

    public static final String CHANNEL_CODE = "ALIPAY";

    /** 默认网关（沙箱为 https://openapi-sandbox.dl.alipaydev.com/gateway.do，可在渠道参数里覆盖） */
    private static final String DEFAULT_GATEWAY = "https://openapi.alipay.com/gateway.do";

    private static final String METHOD_PAGE_PAY = "alipay.trade.page.pay";
    private static final String METHOD_QUERY = "alipay.trade.query";
    private static final String METHOD_CLOSE = "alipay.trade.close";
    private static final String METHOD_REFUND = "alipay.trade.refund";
    private static final String METHOD_REFUND_QUERY = "alipay.trade.fastpay.refund.query";

    /** 不参与签名的字段 */
    private static final Set<String> UNSIGNED = Set.of("sign", "sign_type");

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 网关调用超时（毫秒）—— 支付链路不能无限等待 */
    private static final int POST_TIMEOUT_MS = 10_000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChannelCredentialAccessor credentialAccessor;

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public String getChannelName() {
        return "支付宝";
    }

    // ─────────────────────────── 下单 ───────────────────────────

    @Override
    public ChannelPayResult createPayment(PaymentRequest request) {
        PaymentChannelParam param = requireConfigured();
        String outTradeNo = request.getBizNo();
        if (outTradeNo == null || outTradeNo.isBlank()) {
            throw new IllegalStateException("支付请求缺少业务单号（bizNo），无法作为支付宝 out_trade_no");
        }

        // biz_content：支付宝业务参数（金额单位是**元**，两位小数）
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", outTradeNo);
        biz.put("total_amount", request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        biz.put("subject", buildSubject(request));
        biz.put("product_code", "FAST_INSTANT_TRADE_PAY");

        Map<String, String> params = commonParams(param, METHOD_PAGE_PAY, toJson(biz), param.getNotifyUrl());
        params.put("sign", sign(params, param));

        // 收银台地址：参数在 query string 上（值需 URL 编码）
        String payUrl = gateway(param) + "?" + toQueryString(params);
        log.info("支付宝下单：outTradeNo={}, amount={}", outTradeNo, request.getAmount());
        // channelOrderNo 取**商户单号**（= 提交给支付宝的 out_trade_no），后续查单/关单/退款都以它为键
        return ChannelPayResult.ofUrl(outTradeNo, payUrl, payUrl);
    }

    // ─────────────────────────── 查单 ───────────────────────────

    @Override
    public PaymentRecord queryPayment(String channelOrderNo) {
        PaymentChannelParam param = requireConfigured();
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", channelOrderNo);
        JsonNode node = invoke(param, METHOD_QUERY, toJson(biz));

        PaymentRecord record = new PaymentRecord();
        record.setChannel(CHANNEL_CODE);
        record.setChannelOrderNo(channelOrderNo);
        record.setCallbackData(node.toString());
        record.setCallbackTime(LocalDateTime.now());
        // trade_status：TRADE_SUCCESS / TRADE_FINISHED = 已支付；WAIT_BUYER_PAY = 未付；TRADE_CLOSED = 已关
        String status = text(node, "trade_status");
        record.setStatus("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status) ? 2 : 0);
        record.setChannelTradeNo(text(node, "trade_no"));
        String amount = text(node, "total_amount");
        if (amount != null) {
            record.setAmount(new BigDecimal(amount));
        }
        if (record.getStatus() != 2) {
            record.setErrorCode(text(node, "sub_code") != null ? text(node, "sub_code") : text(node, "code"));
            record.setErrorMsg(text(node, "sub_msg") != null ? text(node, "sub_msg") : text(node, "msg"));
        }
        return record;
    }

    // ─────────────────────────── 关单 ───────────────────────────

    @Override
    public void closePayment(String channelOrderNo) {
        PaymentChannelParam param = requireConfigured();
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", channelOrderNo);
        JsonNode node = invoke(param, METHOD_CLOSE, toJson(biz));
        // 关单失败（例如交易已支付）要让调用方知道 —— 不能吞掉
        if (!"10000".equals(text(node, "code"))) {
            throw new IllegalStateException("支付宝关单失败：" + text(node, "sub_msg") + "/" + text(node, "msg"));
        }
        log.info("支付宝关单成功：outTradeNo={}", channelOrderNo);
    }

    // ─────────────────────────── 退款 ───────────────────────────

    @Override
    public String createRefund(RefundRequest request, PaymentRequest originalPayment) {
        PaymentChannelParam param = requireConfigured();
        String outTradeNo = originalPayment.getBizNo();
        // out_request_no 是**退款幂等键**：同一笔支付用同一个值重复请求不会重复退款
        String outRequestNo = "RF" + request.getId();

        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", outTradeNo);
        biz.put("refund_amount", request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        biz.put("out_request_no", outRequestNo);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            biz.put("refund_reason", request.getReason());
        }
        JsonNode node = invoke(param, METHOD_REFUND, toJson(biz));
        if (!"10000".equals(text(node, "code"))) {
            throw new IllegalStateException("支付宝退款失败：" + text(node, "sub_msg") + "/" + text(node, "msg"));
        }
        String tradeNo = text(node, "trade_no");
        log.info("支付宝退款已受理：outTradeNo={}, outRequestNo={}, tradeNo={}", outTradeNo, outRequestNo, tradeNo);
        // 渠道退款号 = 渠道交易号 + 我方退款请求号（后续查询要把两者都带上）
        return tradeNo == null ? outRequestNo : tradeNo + "-" + outRequestNo;
    }

    @Override
    public RefundRecord queryRefund(String channelRefundNo) {
        PaymentChannelParam param = requireConfigured();
        // 入参形如 "<trade_no>-<out_request_no>"（见 createRefund），拆开分别传
        String tradeNo = null;
        String outRequestNo = channelRefundNo;
        int idx = channelRefundNo == null ? -1 : channelRefundNo.lastIndexOf('-');
        if (idx > 0) {
            tradeNo = channelRefundNo.substring(0, idx);
            outRequestNo = channelRefundNo.substring(idx + 1);
        }
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_request_no", outRequestNo);
        if (tradeNo != null) {
            biz.put("trade_no", tradeNo);
        }
        JsonNode node = invoke(param, METHOD_REFUND_QUERY, toJson(biz));

        RefundRecord record = new RefundRecord();
        record.setChannel(CHANNEL_CODE);
        record.setChannelRefundNo(channelRefundNo);
        record.setCallbackData(node.toString());
        record.setCallbackTime(LocalDateTime.now());
        // 退款查询：有 refund_amount 即该笔退款已发生（全额场景下等价于成功）
        String refundAmount = text(node, "refund_amount");
        record.setStatus(refundAmount != null ? 2 : 0);
        if (refundAmount != null) {
            record.setAmount(new BigDecimal(refundAmount));
        }
        return record;
    }

    @Override
    public boolean isAvailable() {
        // 只有「凭据齐备」才算可用：没配 appId / 私钥时下单必然失败。
        // 早先恒 true 会让前端把一个点了就报错的渠道显示成"可用"。
        PaymentChannelParam param = credentialAccessor.read(CHANNEL_CODE, PaymentChannelParam.class);
        return param != null && Boolean.TRUE.equals(param.getEnabled())
                && hasText(param.getAppId()) && hasText(param.getAlipayPrivateKey());
    }

    @Override
    public BigDecimal getMinAmount() {
        return BigDecimal.valueOf(0.01);
    }

    @Override
    public BigDecimal getMaxAmount() {
        return BigDecimal.valueOf(50000);
    }

    // ─────────────────────────── 内部实现 ───────────────────────────

    /** 渠道参数（本租户）；未启用/缺关键项抛错 —— 绝不静默走假下单。 */
    private PaymentChannelParam requireConfigured() {
        PaymentChannelParam param = credentialAccessor.read(CHANNEL_CODE, PaymentChannelParam.class);
        if (param == null || !Boolean.TRUE.equals(param.getEnabled())) {
            throw new IllegalStateException("支付宝渠道未启用或未配置");
        }
        if (!hasText(param.getAppId())) {
            throw new IllegalStateException("支付宝渠道缺少 appId");
        }
        if (!hasText(param.getAlipayPrivateKey())) {
            throw new IllegalStateException("支付宝渠道缺少应用私钥（alipayPrivateKey），无法签名");
        }
        return param;
    }

    private Map<String, String> commonParams(PaymentChannelParam param, String method,
                                            String bizContent, String notifyUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", param.getAppId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        if (hasText(notifyUrl)) {
            params.put("notify_url", notifyUrl);
        }
        return params;
    }

    /** 签名：待签串用**原值**（不 URL 编码），签完再编码进 query / 表单。 */
    private String sign(Map<String, String> params, PaymentChannelParam param) {
        String content = Rsa2.buildSignContent(new TreeMap<>(params), UNSIGNED);
        return Rsa2.sign(content, param.getAlipayPrivateKey());
    }

    /** 调用网关并取响应节点：应答形如 {@code {"alipay_trade_query_response":{...},"sign":"..."}}。 */
    private JsonNode invoke(PaymentChannelParam param, String method, String bizContent) {
        Map<String, String> params = commonParams(param, method, bizContent, param.getNotifyUrl());
        params.put("sign", sign(params, param));
        String body = postForm(gateway(param), params);
        log.debug("支付宝 {} 应答：{}", method, body);
        try {
            JsonNode root = MAPPER.readTree(body);
            // 响应键 = 方法名把点换成下划线 + "_response"
            String key = method.replace('.', '_') + "_response";
            JsonNode node = root.get(key);
            if (node == null) {
                throw new IllegalStateException("支付宝应答缺少节点 " + key + "：" + body);
            }
            // ⚠️ 响应必须验签：不验就等于"任何能返回 JSON 的东西都能让系统改单"
            verifyResponse(root, key, body, param);
            return node;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("支付宝应答解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 用支付宝公钥验证应答签名。
     *
     * <p>待验串按支付宝规定取**响应体里该节点的原文**（不重新序列化 —— 重新序列化的
     * 空白/字段顺序差异会让签名对不上）。</p>
     */
    private void verifyResponse(JsonNode root, String nodeKey, String body, PaymentChannelParam param) {
        String sign = text(root, "sign");
        if (!hasText(param.getAlipayPublicKey())) {
            // 未配公钥 ⇒ 验不了 ⇒ fail-closed（与回调侧同一原则：宁可不认，也不认伪造）
            throw new IllegalStateException("支付宝渠道未配置支付宝公钥（alipayPublicKey），无法验证应答签名");
        }
        if (!hasText(sign)) {
            throw new IllegalStateException("支付宝应答缺少 sign 字段");
        }
        String content = extractRawNode(body, nodeKey);
        if (!Rsa2.verify(content, sign, param.getAlipayPublicKey())) {
            throw new IllegalStateException("支付宝应答验签失败（可能是伪造或公钥配置错误）");
        }
    }

    /** 从原始报文里**原样抠出**某个 JSON 节点的文本（避免重新序列化导致签名不一致）。 */
    private static String extractRawNode(String body, String nodeKey) {
        String marker = "\"" + nodeKey + "\"";
        int k = body.indexOf(marker);
        if (k < 0) {
            throw new IllegalStateException("应答里找不到节点 " + nodeKey);
        }
        int start = body.indexOf('{', k);
        int depth = 0;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return body.substring(start, i + 1);
                }
            }
        }
        throw new IllegalStateException("应答节点 " + nodeKey + " 不完整");
    }

    /**
     * 表单 POST 到网关。
     *
     * <p>用 hutool 的 {@code HttpRequest}（{@code HttpUtil.post} 只接受 {@code Map<String,Object>}，
     * 与我们的 {@code Map<String,String>} 不匹配）；显式设置**超时**——网关卡住时不能让
     * 支付请求线程无限等待。</p>
     */
    private static String postForm(String url, Map<String, String> params) {
        Map<String, Object> form = new LinkedHashMap<>(params);
        return cn.hutool.http.HttpRequest.post(url)
                .form(form)
                .timeout(POST_TIMEOUT_MS)
                .execute()
                .body();
    }

    private static String gateway(PaymentChannelParam param) {
        return hasText(param.getAlipayGateway()) ? param.getAlipayGateway().trim() : DEFAULT_GATEWAY;
    }

    private static String buildSubject(PaymentRequest request) {
        String subject = request.getRemark();
        if (subject == null || subject.isBlank()) {
            subject = "订单支付 " + request.getBizNo();
        }
        // 支付宝 subject 上限 256 字符
        return subject.length() > 200 ? subject.substring(0, 200) : subject;
    }

    private static String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    private static String encode(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("构造支付宝 biz_content 失败：" + e.getMessage(), e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
