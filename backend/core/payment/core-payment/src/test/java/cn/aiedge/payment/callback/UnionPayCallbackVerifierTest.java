package cn.aiedge.payment.callback;

import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 银联异步通知验签测试 —— **不需要真实银联证书**。
 *
 * <p>用自生成 RSA 密钥对签名，重点验证银联与支付宝的**关键差异**：
 * 待验签串里 value 要 URL 编码（支付宝是用解码后的原值），
 * 以及 {@code signMethod} 决定摘要算法、{@code certId} 决定用哪张证书。</p>
 */
class UnionPayCallbackVerifierTest {

    private static final Long TENANT_ID = 1L;
    private static final String CERT_ID = "68759529225";

    private static final com.fasterxml.jackson.databind.ObjectMapper OM =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private TenantChannelCredentialReader credentialReader;
    private UnionPayCallbackVerifier verifier;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();

        credentialReader = mock(TenantChannelCredentialReader.class);
        verifier = new UnionPayCallbackVerifier(credentialReader);
    }

    private static String pem(java.security.PublicKey key) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(key.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private void configureChannel(Map<String, String> certs, boolean enabled) throws Exception {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("merchantNo", "777290058110048");
        config.put("enabled", enabled);
        config.put("unionPayCerts", OM.writeValueAsString(certs));
        when(credentialReader.read(eq(TENANT_ID), eq(UnionPayCallbackVerifier.CONFIG_KEY)))
                .thenReturn(OM.writeValueAsString(config));
    }

    private Map<String, String> singleCert() {
        Map<String, String> certs = new LinkedHashMap<>();
        certs.put(CERT_ID, pem(keyPair.getPublic()));
        return certs;
    }

    private Map<String, String> baseParams() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("orderId", "SO-20260921-0001");
        p.put("queryId", "202609211234567890123");
        p.put("txnAmt", "12850");       // 单位：分
        p.put("respCode", "00");        // 00 = 交易成功
        p.put("certId", CERT_ID);
        p.put("signMethod", "11");      // RSA-SHA256
        return p;
    }

    /** 按银联规则签名并拼出 form-urlencoded 报文。 */
    private String signedBody(Map<String, String> params, String algorithm, KeyPair signWith) throws Exception {
        String content = UnionPayCallbackVerifier.buildSignContent(params);
        Signature sig = Signature.getInstance(algorithm);
        sig.initSign(signWith.getPrivate());
        sig.update(content.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(sig.sign());

        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("signature", signature);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : all.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private PaymentCallbackContext context(String body) {
        return new PaymentCallbackContext(TENANT_ID, "UNIONPAY",
                body.getBytes(StandardCharsets.UTF_8), Map.of(), "127.0.0.1");
    }

    // ══════════════════ 放行路径 ══════════════════

    @Test
    @DisplayName("合法签名（SHA256）→ 通过，金额由分换元")
    void validSignaturePasses() throws Exception {
        configureChannel(singleCert(), true);
        PaymentCallbackResult result = verifier.verify(
                context(signedBody(baseParams(), "SHA256withRSA", keyPair)));

        assertEquals("SO-20260921-0001", result.merchantOrderNo());
        assertEquals("202609211234567890123", result.channelOrderNo());
        assertTrue(result.success());
        assertEquals(0, new BigDecimal("128.50").compareTo(result.paidAmount()));
    }

    @Test
    @DisplayName("signMethod=01（SHA-1）同样支持")
    void sha1SignatureSupported() throws Exception {
        configureChannel(singleCert(), true);
        // ⚠️ signMethod 必须与真实签名算法一致：声明 11(SHA256) 却用 SHA1 签，
        // 验签方会按 SHA256 去验 —— 必然失败。第一版用例就是栽在这（恰好证明该断言有效）。
        Map<String, String> p = baseParams();
        p.put("signMethod", "01"); // RSA-SHA1
        PaymentCallbackResult result = verifier.verify(
                context(signedBody(p, "SHA1withRSA", keyPair)));
        assertEquals("SO-20260921-0001", result.merchantOrderNo());
    }

    @Test
    @DisplayName("respCode != 00 → 验签通过但 success=false")
    void nonSuccessRespCode() throws Exception {
        configureChannel(singleCert(), true);
        Map<String, String> p = baseParams();
        p.put("respCode", "05"); // 交易失败
        assertFalse(verifier.verify(context(signedBody(p, "SHA256withRSA", keyPair))).success());
    }

    // ══════════════════ 拒绝路径 ══════════════════

    @Test
    @DisplayName("篡改金额 → 验签失败")
    void tamperedAmountRejected() throws Exception {
        configureChannel(singleCert(), true);
        String body = signedBody(baseParams(), "SHA256withRSA", keyPair);
        String tampered = body.replace(URLEncoder.encode("12850", StandardCharsets.UTF_8),
                URLEncoder.encode("1", StandardCharsets.UTF_8));
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(tampered)));
    }

    @Test
    @DisplayName("未配置的 certId → 拒绝（证书换发未配置 / 伪造）")
    void unknownCertIdRejected() throws Exception {
        configureChannel(singleCert(), true);
        Map<String, String> p = baseParams();
        p.put("certId", "00000000000");
        String body = signedBody(p, "SHA256withRSA", keyPair);
        assertThrows(PaymentCallbackVerificationException.class, () -> verifier.verify(context(body)));
    }

    @Test
    @DisplayName("缺 signMethod → 拒绝（不猜默认算法）")
    void missingSignMethodRejected() throws Exception {
        configureChannel(singleCert(), true);
        Map<String, String> p = baseParams();
        p.remove("signMethod");
        String body = signedBody(p, "SHA256withRSA", keyPair);
        assertThrows(PaymentCallbackVerificationException.class, () -> verifier.verify(context(body)));
    }

    @Test
    @DisplayName("未知 signMethod → 拒绝")
    void unknownSignMethodRejected() throws Exception {
        configureChannel(singleCert(), true);
        Map<String, String> p = baseParams();
        p.put("signMethod", "99");
        String body = signedBody(p, "SHA256withRSA", keyPair);
        assertThrows(PaymentCallbackVerificationException.class, () -> verifier.verify(context(body)));
    }

    @Test
    @DisplayName("缺 signature → 拒绝")
    void missingSignatureRejected() throws Exception {
        configureChannel(singleCert(), true);
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context("orderId=X&respCode=00&certId=" + CERT_ID + "&signMethod=11")));
    }

    @Test
    @DisplayName("用别的私钥签名 → 拒绝")
    void otherKeyRejected() throws Exception {
        configureChannel(singleCert(), true);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair attacker = gen.generateKeyPair();
        String body = signedBody(baseParams(), "SHA256withRSA", attacker);
        assertThrows(PaymentCallbackVerificationException.class, () -> verifier.verify(context(body)));
    }

    @Test
    @DisplayName("未配置证书 → isConfigured=false（fail-closed）")
    void notConfiguredWithoutCerts() throws Exception {
        configureChannel(new LinkedHashMap<>(), true);
        assertFalse(verifier.isConfigured(TENANT_ID));
    }

    @Test
    @DisplayName("渠道停用 → isConfigured=false")
    void disabledChannel() throws Exception {
        configureChannel(singleCert(), false);
        assertFalse(verifier.isConfigured(TENANT_ID));
    }

    // ══════════════════ 规则断言 ══════════════════

    @Test
    @DisplayName("待验签串规则：剔除 signature、按 key 升序、value 做 URL 编码")
    void signContentRules() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("orderId", "SO 1");        // 含空格 → 编码为 +/%20
        p.put("signature", "EXCLUDED");
        p.put("certId", "C1");
        String content = UnionPayCallbackVerifier.buildSignContent(p);
        // key 升序：certId < orderId；signature 被剔除；空格被编码（不能是原样空格）
        assertTrue(content.startsWith("certId=C1&orderId="), "实际=" + content);
        assertFalse(content.contains(" "), "值必须 URL 编码，不能出现原始空格");
        assertFalse(content.contains("signature"));
    }

    @Test
    @DisplayName("应答体为银联约定的 ok / fail")
    void ackBody() {
        assertEquals("ok", verifier.ackBody(true));
        assertEquals("fail", verifier.ackBody(false));
    }
}
