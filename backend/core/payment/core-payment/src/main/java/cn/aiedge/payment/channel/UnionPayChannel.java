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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 银联渠道适配器（**真实实现**，2026-09-26 由桩改造；按银联「全渠道」网关支付口径）。
 *
 * <h2>接口口径</h2>
 * <ul>
 *   <li>消费（下单）：{@code txnType=01 / txnSubType=01 / bizType=000201}，
 *       金额单位是**分**（{@code txnAmt}），返回网关跳转地址（参数在 query string 上）；</li>
 *   <li>交易状态查询：{@code txnType=00}；</li>
 *   <li>关闭订单：银联**没有单笔"关单"接口**，未结算交易用**消费撤销** {@code txnType=04}，
 *       已结算只能退货 —— 这里按"未结算撤销"实现，并在失败时如实抛错；</li>
 *   <li>退货（退款）：{@code txnType=04}；</li>
 *   <li>退货查询：同样走 {@code txnType=00}（按 {@code orderId}/{@code queryId}）。</li>
 * </ul>
 *
 * <h2>签名口径（必须与验签器一致）</h2>
 * 待签串 = 除 {@code signature} 外的非空参数按名升序、{@code k=v} 以 {@code &} 连接，
 * 且**值做 UTF-8 URL 编码**，算法 {@code SHA256withRSA}（{@code signMethod=11}）。
 * <p>⚠️ 这条「值要 URL 编码」取自本仓既有的 {@code UnionPayCallbackVerifier}，
 * 与部分公开示例（用原值签名）不同。**以银联文档为准** —— 若文档要求用原值，
 * 则本通道与验签器需一起改正；现状两边一致，故"自签自验"闭合，
 * 而真实回调若因此验不过，失败方向是 **fail-closed（拒绝改单）**，不会误入账。</p>
 *
 * <h2>⚠️ 边界（如实说明）</h2>
 * 未经真实网关联调（无商户号/证书、构建环境不出网）。自证方式见 {@code UnionPayChannelTest}
 * 的「签名 → 按验签规则验签」闭合回路。上生产前必须用银联测试环境跑一次真实下单+回调。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayChannel implements PaymentChannel {

    public static final String CHANNEL_CODE = "UNIONPAY";

    /** 默认网关（银联全渠道网关支付正式地址；测试环境可在渠道参数里覆盖） */
    private static final String DEFAULT_GATEWAY = "https://gateway.95516.com/gateway/api/frontTransReq.do";

    private static final String VERSION = "5.1.0";
    private static final String SIGN_METHOD = "11";   // 11 = SHA256withRSA
    private static final String ENCODING = "UTF-8";
    private static final String CURRENCY_CNY = "156"; // 156 = 人民币

    /** 不参与签名的字段（银联只剔除 signature 本身；signMethod 是参与签名的） */
    private static final Set<String> UNSIGNED = Set.of("signature");

    private static final DateTimeFormatter TXN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 网关调用超时（毫秒） */
    private static final int POST_TIMEOUT_MS = 10_000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChannelCredentialAccessor credentialAccessor;

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public String getChannelName() {
        return "银联支付";
    }

    // ─────────────────────────── 下单（消费）───────────────────────────

    @Override
    public ChannelPayResult createPayment(PaymentRequest request) {
        PaymentChannelParam param = requireConfigured();
        String orderId = request.getBizNo();
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalStateException("支付请求缺少业务单号（bizNo），无法作为银联 orderId");
        }

        Map<String, String> params = baseParams(param, "01", "01", request.getBizNo(), request.getAmount());
        params.put("bizType", "000201");
        params.put("channelType", "07");   // 07 = PC
        params.put("accessType", "0");     // 0 = 商户直连
        if (hasText(param.getNotifyUrl())) {
            params.put("backUrl", param.getNotifyUrl());
        }
        sign(params, param);

        String payUrl = gateway(param) + "?" + toQueryString(params);
        log.info("银联下单：orderId={}, amountFen={}", orderId, toFen(request.getAmount()));
        return ChannelPayResult.ofUrl(orderId, payUrl, payUrl);
    }

    // ─────────────────────────── 交易状态查询 ───────────────────────────

    @Override
    public PaymentRecord queryPayment(String channelOrderNo) {
        PaymentChannelParam param = requireConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", VERSION);
        params.put("encoding", ENCODING);
        params.put("signMethod", SIGN_METHOD);
        params.put("txnType", "00");
        params.put("txnSubType", "00");
        params.put("bizType", "000000");
        params.put("merId", param.getMerchantNo());
        params.put("orderId", channelOrderNo);
        params.put("txnTime", LocalDateTime.now().format(TXN_TIME));
        sign(params, param);

        JsonNode node = invoke(param, params);

        PaymentRecord record = new PaymentRecord();
        record.setChannel(CHANNEL_CODE);
        record.setChannelOrderNo(channelOrderNo);
        record.setCallbackData(node.toString());
        record.setCallbackTime(LocalDateTime.now());
        // respCode=00 成功；origRespCode 是原交易应答码（00 = 交易成功）
        String respCode = text(node, "respCode");
        String origRespCode = text(node, "origRespCode");
        boolean paid = "00".equals(respCode) && ("00".equals(origRespCode) || origRespCode == null);
        record.setStatus(paid ? 2 : 0);
        record.setChannelTradeNo(text(node, "queryId"));
        String amount = text(node, "txnAmt");
        if (amount != null) {
            record.setAmount(new BigDecimal(amount).movePointLeft(2)); // 分 → 元
        }
        if (!paid) {
            record.setErrorCode(respCode);
            record.setErrorMsg(text(node, "respMsg"));
        }
        return record;
    }

    // ─────────────────────────── 关闭订单 ───────────────────────────

    @Override
    public void closePayment(String channelOrderNo) {
        PaymentChannelParam param = requireConfigured();
        // 银联没有单笔关单接口：未结算交易用**消费撤销**（txnType=04）作等价处置
        Map<String, String> params = baseParams(param, "04", "00", channelOrderNo, null);
        params.put("bizType", "000201");
        params.put("channelType", "07");
        params.put("accessType", "0");
        sign(params, param);

        JsonNode node = invoke(param, params);
        if (!"00".equals(text(node, "respCode"))) {
            // 已结算/已支付会撤销失败 —— 不能吞，要让调用方知道"这笔关不掉"
            throw new IllegalStateException("银联撤销（关单）失败：" + text(node, "respCode")
                    + " " + text(node, "respMsg"));
        }
        log.info("银联撤销（关单）成功：orderId={}", channelOrderNo);
    }

    // ─────────────────────────── 退款（退货）───────────────────────────

    @Override
    public String createRefund(RefundRequest request, PaymentRequest originalPayment) {
        PaymentChannelParam param = requireConfigured();
        // 退货用**原交易的单号 + 原交易时间**；退款单号另起（幂等）
        String refundOrderId = "RF" + request.getId();
        Map<String, String> params = baseParams(param, "04", "00", refundOrderId, request.getAmount());
        params.put("bizType", "000201");
        params.put("channelType", "07");
        params.put("accessType", "0");
        // 退货必须带**原交易的查询 ID**（queryId，下单后由查询/回调取得）；
        // 没有它就退不了 —— 如实拒绝，别让请求带着空值打到网关再报个看不懂的错
        if (!hasText(originalPayment.getChannelTradeNo())) {
            throw new IllegalStateException("银联退货缺少原交易查询号（origQryId），"
                    + "请先对账/查询取得该笔的 queryId");
        }
        params.put("origQryId", originalPayment.getChannelTradeNo());
        sign(params, param);

        JsonNode node = invoke(param, params);
        if (!"00".equals(text(node, "respCode"))) {
            throw new IllegalStateException("银联退货失败：" + text(node, "respCode") + " " + text(node, "respMsg"));
        }
        String queryId = text(node, "queryId");
        log.info("银联退货已受理：refundOrderId={}, queryId={}", refundOrderId, queryId);
        return queryId == null ? refundOrderId : queryId;
    }

    @Override
    public RefundRecord queryRefund(String channelRefundNo) {
        PaymentChannelParam param = requireConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", VERSION);
        params.put("encoding", ENCODING);
        params.put("signMethod", SIGN_METHOD);
        params.put("txnType", "00");
        params.put("txnSubType", "00");
        params.put("bizType", "000000");
        params.put("merId", param.getMerchantNo());
        params.put("orderId", channelRefundNo);
        params.put("txnTime", LocalDateTime.now().format(TXN_TIME));
        sign(params, param);

        JsonNode node = invoke(param, params);
        RefundRecord record = new RefundRecord();
        record.setChannel(CHANNEL_CODE);
        record.setChannelRefundNo(channelRefundNo);
        record.setCallbackData(node.toString());
        record.setCallbackTime(LocalDateTime.now());
        String respCode = text(node, "respCode");
        String origRespCode = text(node, "origRespCode");
        record.setStatus("00".equals(respCode) && "00".equals(origRespCode) ? 2 : 0);
        String amount = text(node, "txnAmt");
        if (amount != null) {
            record.setAmount(new BigDecimal(amount).movePointLeft(2));
        }
        return record;
    }

    @Override
    public boolean isAvailable() {
        PaymentChannelParam param = credentialAccessor.read(CHANNEL_CODE, PaymentChannelParam.class);
        return param != null && Boolean.TRUE.equals(param.getEnabled())
                && hasText(param.getMerchantNo()) && hasText(param.getUnionPayMerchantPrivateKey());
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

    private PaymentChannelParam requireConfigured() {
        PaymentChannelParam param = credentialAccessor.read(CHANNEL_CODE, PaymentChannelParam.class);
        if (param == null || !Boolean.TRUE.equals(param.getEnabled())) {
            throw new IllegalStateException("银联渠道未启用或未配置");
        }
        if (!hasText(param.getMerchantNo())) {
            throw new IllegalStateException("银联渠道缺少商户号（merchantNo）");
        }
        if (!hasText(param.getUnionPayMerchantPrivateKey())) {
            throw new IllegalStateException("银联渠道缺少商户私钥（unionPayMerchantPrivateKey），无法签名");
        }
        return param;
    }

    /** 公共字段（消费/撤销/退货共用）。{@code amount} 为 null 时不上送 txnAmt（撤销场景）。 */
    private Map<String, String> baseParams(PaymentChannelParam param, String txnType, String txnSubType,
                                           String orderId, BigDecimal amount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", VERSION);
        params.put("encoding", ENCODING);
        params.put("signMethod", SIGN_METHOD);
        params.put("txnType", txnType);
        params.put("txnSubType", txnSubType);
        params.put("merId", param.getMerchantNo());
        params.put("orderId", orderId);
        params.put("txnTime", LocalDateTime.now().format(TXN_TIME));
        params.put("currencyCode", CURRENCY_CNY);
        if (amount != null) {
            params.put("txnAmt", String.valueOf(toFen(amount)));
        }
        if (hasText(param.getUnionPayCertId())) {
            params.put("certId", param.getUnionPayCertId());
        }
        return params;
    }

    /** 金额元 → 分（银联的 txnAmt 是分，整数）。 */
    private static long toFen(BigDecimal yuan) {
        return yuan.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** 签名：待签串与 {@code UnionPayCallbackVerifier} 完全同口径（值 URL 编码、只剔 signature）。 */
    private void sign(Map<String, String> params, PaymentChannelParam param) {
        String content = Rsa2.buildSignContent(new TreeMap<>(params), UNSIGNED, Rsa2::urlEncode);
        params.put("signature", Rsa2.sign(content, param.getUnionPayMerchantPrivateKey()));
    }

    /**
     * 调用银联网关。
     *
     * <p>⚠️ 银联的应答**没有签名**（与我方回调报文不同），因此这里**无法验签** ——
     * 故对查询/关单/退货这类"会改变业务状态"的调用，一律以 {@code respCode} 判定，
     * 且**失败即抛错**，绝不把"没查到"当成"已成功"。这是本通道唯一需要额外当心的地方。</p>
     */
    private JsonNode invoke(PaymentChannelParam param, Map<String, String> params) {
        String body = postForm(gateway(param), params);
        log.debug("银联应答：{}", body);
        try {
            // 银联应答是 key=value&key=value 形式
            Map<String, String> resp = new LinkedHashMap<>();
            for (String pair : body.split("&")) {
                int i = pair.indexOf('=');
                if (i > 0) {
                    resp.put(pair.substring(0, i), pair.substring(i + 1));
                }
            }
            if (resp.isEmpty()) {
                throw new IllegalStateException("银联网关应答为空或格式异常：" + body);
            }
            return MAPPER.valueToTree(resp);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("银联应答解析失败：" + e.getMessage(), e);
        }
    }

    /** 表单 POST 到网关（{@code HttpUtil.post} 只接受 Map<String,Object>，故显式构造并设超时）。 */
    private static String postForm(String url, Map<String, String> params) {
        Map<String, Object> form = new LinkedHashMap<>(params);
        return cn.hutool.http.HttpRequest.post(url)
                .form(form)
                .timeout(POST_TIMEOUT_MS)
                .execute()
                .body();
    }

    private static String gateway(PaymentChannelParam param) {
        return hasText(param.getUnionPayGateway()) ? param.getUnionPayGateway().trim() : DEFAULT_GATEWAY;
    }

    private static String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(Rsa2.urlEncode(e.getKey())).append('=').append(Rsa2.urlEncode(e.getValue()));
        }
        return sb.toString();
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

}
