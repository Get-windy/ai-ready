package cn.aiedge.payment.channel;

import cn.aiedge.payment.crypto.Rsa2;
import cn.aiedge.payment.dto.PaymentChannelParam;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.support.ChannelCredentialAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 银联通道「下单签名」的**闭合回路**验证（2026-09-26）。
 *
 * <p>与 {@link AlipayChannelTest} 同一思路：自生成密钥对 → 通道用私钥签 → 用**回调验签器
 * 同一份实现**（{@link Rsa2}）与**同一套待签串口径**（值 URL 编码、只剔 {@code signature}）验。
 * </p>
 *
 * <p>银联这条链路特别容易被写错的两点，本测试单独钉死：</p>
 * <ol>
 *   <li>金额单位是**分**（{@code txnAmt}），传成"元"会导致实付金额差 100 倍；</li>
 *   <li>待签串里的**值要 URL 编码** —— 与支付宝（用原值）不同，混用就自验不过。</li>
 * </ol>
 */
class UnionPayChannelTest {

    private ChannelCredentialAccessor accessor;
    private UnionPayChannel channel;
    private String publicKeyBase64;
    private PaymentChannelParam param;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        param = new PaymentChannelParam();
        param.setEnabled(true);
        param.setMerchantNo("777290058110048");
        param.setUnionPayCertId("CERT-0001");
        param.setUnionPayMerchantPrivateKey(privateKeyBase64);
        param.setUnionPayCerts(publicKeyBase64);
        param.setNotifyUrl("https://mall.example.com/api/payment/callback/1/UNIONPAY");

        accessor = mock(ChannelCredentialAccessor.class);
        when(accessor.read(any(String.class), eq(PaymentChannelParam.class))).thenReturn(param);
        channel = new UnionPayChannel(accessor);
    }

    private PaymentRequest request(String bizNo, String amount) {
        PaymentRequest r = new PaymentRequest();
        r.setBizNo(bizNo);
        r.setBizType("SALE_ORDER");
        r.setAmount(new BigDecimal(amount));
        r.setChannel("UNIONPAY");
        return r;
    }

    private Map<String, String> queryOf(String payUrl) {
        Map<String, String> map = new LinkedHashMap<>();
        String q = payUrl.substring(payUrl.indexOf('?') + 1);
        for (String pair : q.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) {
                map.put(decode(pair.substring(0, i)), decode(pair.substring(i + 1)));
            }
        }
        return map;
    }

    private static String decode(String v) {
        return URLDecoder.decode(v, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("下单：网关 URL 的签名按「值 URL 编码」口径能被验过（与验签器同一份实现）")
    void createPayment_signatureVerifies() {
        ChannelPayResult result = channel.createPayment(request("XSDD-20260927-101", "12.34"));

        assertEquals("XSDD-20260927-101", result.channelOrderNo());
        assertNotNull(result.payUrl());

        Map<String, String> params = queryOf(result.payUrl());
        assertEquals("01", params.get("txnType"), "01 = 消费");
        assertEquals("11", params.get("signMethod"), "11 = SHA256withRSA");
        assertEquals("777290058110048", params.get("merId"));
        // ★ 金额单位是分：12.34 元 → 1234 分
        assertEquals("1234", params.get("txnAmt"), "银联金额单位是**分**，传成元会差 100 倍");
        assertEquals("156", params.get("currencyCode"));

        String content = Rsa2.buildSignContent(params, Set.of("signature"), Rsa2::urlEncode);
        assertTrue(Rsa2.verify(content, params.get("signature"), publicKeyBase64),
                "用验签器同一口径必须验得过");
    }

    @Test
    @DisplayName("金额换算按分四舍五入（19.999 元 → 2000 分）")
    void amountRoundingToFen() {
        ChannelPayResult result = channel.createPayment(request("XSDD-20260927-102", "19.999"));
        assertEquals("2000", queryOf(result.payUrl()).get("txnAmt"));
    }

    @Test
    @DisplayName("篡改金额后签名失效")
    void tamperedAmountFails() {
        ChannelPayResult result = channel.createPayment(request("XSDD-20260927-103", "50.00"));
        Map<String, String> params = queryOf(result.payUrl());
        String sign = params.get("signature");
        params.put("txnAmt", "1");
        String content = Rsa2.buildSignContent(params, Set.of("signature"), Rsa2::urlEncode);
        assertFalse(Rsa2.verify(content, sign, publicKeyBase64));
    }

    @Test
    @DisplayName("若用「不对值编码」的口径验签，必须验不过（证明编码口径确实是签名的一部分）")
    void signingWithoutEncodingWouldNotVerify() {
        ChannelPayResult result = channel.createPayment(request("XSDD-20260927-104", "8.88"));
        Map<String, String> params = queryOf(result.payUrl());
        // 用支付宝那套（不编码）去验银联的签名 —— 必须失败，
        // 否则说明「编码与否」根本没进签名，两种口径就成了偶然等价
        String wrong = Rsa2.buildSignContent(params, Set.of("signature"));
        assertFalse(Rsa2.verify(wrong, params.get("signature"), publicKeyBase64));
    }

    @Test
    @DisplayName("缺商户私钥 / 未启用时下单报错")
    void misconfiguredRejects() {
        param.setUnionPayMerchantPrivateKey(null);
        assertThrows(IllegalStateException.class,
                () -> channel.createPayment(request("XSDD-20260927-105", "1.00")));
        param.setUnionPayMerchantPrivateKey("x");
        param.setEnabled(false);
        assertThrows(IllegalStateException.class,
                () -> channel.createPayment(request("XSDD-20260927-106", "1.00")));
    }

    @Test
    @DisplayName("isAvailable 只在商户号与私钥齐备时为 true")
    void isAvailableReflectsCredentials() {
        assertTrue(channel.isAvailable());
        param.setMerchantNo(null);
        assertFalse(channel.isAvailable());
    }
}
