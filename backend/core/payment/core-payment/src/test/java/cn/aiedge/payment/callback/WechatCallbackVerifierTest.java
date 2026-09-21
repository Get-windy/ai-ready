package cn.aiedge.payment.callback;

import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.service.SysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
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
 * 微信支付 APIv3 回调验签 + 解密测试 —— **不需要真实微信凭据**。
 *
 * <p>做法：测试内自生成「平台证书密钥对」与「APIv3 密钥」，按微信的真实格式
 * **自己加密 resource、自己签名报文**，再调 {@link WechatCallbackVerifier#verify}。</p>
 *
 * <p>覆盖微信相比支付宝多出来的两层（验签 + AES-GCM 解密）以及三个易漏点：
 * 时间戳重放、证书轮换（多序列号）、金额单位（分 → 元）。</p>
 */
class WechatCallbackVerifierTest {

    private static final Long TENANT_ID = 1L;
    private static final String SERIAL = "5157F09EFDC096DE15EBE81A47057A72";
    private static final String SERIAL_NEW = "7F2A1B3C4D5E6F708192A3B4C5D6E7F8";
    private static final String NONCE = "abcdefghijklmnop";
    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef"; // 32 字节

    /** 测试内部把「报文」与「签名」拼一起传递的分隔符（纯 ASCII，避免源码里出现控制字符）。 */
    private static final String SIGN_SEPARATOR = "@@SIGN@@";

    private SysConfigService sysConfigService;
    private WechatCallbackVerifier verifier;
    private KeyPair platformKeyPair;
    private KeyPair rotatedKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        platformKeyPair = gen.generateKeyPair();
        rotatedKeyPair = gen.generateKeyPair();

        sysConfigService = mock(SysConfigService.class);
        verifier = new WechatCallbackVerifier(sysConfigService);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper OM =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private static String pem(java.security.PublicKey key) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(key.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    /**
     * 配置渠道。
     *
     * <p>⚠️ 用 Jackson 构造而不是字符串拼接：`wechatPlatformCerts` 的值本身是 **JSON 文本**，
     * 拼进外层 JSON 要再转义一层（引号 + PEM 里的换行），手写转义极易写错 ——
     * 第一版就是栽在这里，表现为「配置读出来是空表 ⇒ 报未配置」。</p>
     */
    private void configureChannel(Map<String, String> certs, boolean enabled, String apiV3Key) throws Exception {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("merchantNo", "1900000109");
        config.put("enabled", enabled);
        config.put("wechatApiV3Key", apiV3Key == null ? "" : apiV3Key);
        config.put("wechatPlatformCerts", OM.writeValueAsString(certs));
        when(sysConfigService.getValue(eq(WechatCallbackVerifier.CONFIG_KEY), any()))
                .thenReturn(OM.writeValueAsString(config));
    }

    private Map<String, String> singleCert() {
        Map<String, String> certs = new LinkedHashMap<>();
        certs.put(SERIAL, pem(platformKeyPair.getPublic()));
        return certs;
    }

    private Map<String, String> rotatedCerts() {
        Map<String, String> certs = singleCert();
        certs.put(SERIAL_NEW, pem(rotatedKeyPair.getPublic()));
        return certs;
    }

    /** 按微信格式加密 resource 并签名整个报文。 */
    private String signedBody(Map<String, String> businessFields, KeyPair signWith, long timestamp) throws Exception {
        String ciphertext = encrypt(toJson(businessFields), API_V3_KEY, NONCE, "transaction");
        String body = envelope(ciphertext);
        return body + SIGN_SEPARATOR + sign(body, signWith, timestamp);
    }

    /** 用指定私钥对「时间戳\n随机串\n报文\n」签名。 */
    private String sign(String body, KeyPair signWith, long timestamp) throws Exception {
        String message = timestamp + "\n" + NONCE + "\n" + body + "\n";
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(signWith.getPrivate());
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    private String envelope(String ciphertext) {
        return "{\"id\":\"EV-20260921\",\"create_time\":\"2026-09-21T10:00:00+08:00\","
                + "\"resource_type\":\"encrypt-resource\",\"event_type\":\"TRANSACTION.SUCCESS\","
                + "\"resource\":{\"algorithm\":\"AEAD_AES_256_GCM\",\"ciphertext\":\"" + ciphertext + "\","
                + "\"nonce\":\"" + NONCE + "\",\"associated_data\":\"transaction\"}}";
    }

    private PaymentCallbackContext context(String bodyAndSign, String serial, long timestamp) {
        String[] parts = bodyAndSign.split(SIGN_SEPARATOR, 2);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Wechatpay-Timestamp", String.valueOf(timestamp));
        headers.put("Wechatpay-Nonce", NONCE);
        headers.put("Wechatpay-Signature", parts[1]);
        headers.put("Wechatpay-Serial", serial);
        return new PaymentCallbackContext(TENANT_ID, "WECHAT",
                parts[0].getBytes(StandardCharsets.UTF_8), headers, "127.0.0.1");
    }

    private Map<String, String> businessFields() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("out_trade_no", "SO-20260921-0001");
        m.put("transaction_id", "4200001234202609211234567890");
        m.put("trade_state", "SUCCESS");
        m.put("amount.total", "12850"); // 单位：分
        return m;
    }

    /** 把扁平的 key 还原成嵌套 JSON（amount.total → amount:{total}）。 */
    private static String toJson(Map<String, String> flat) {
        StringBuilder nested = new StringBuilder();
        StringBuilder top = new StringBuilder();
        for (Map.Entry<String, String> e : flat.entrySet()) {
            if (e.getKey().startsWith("amount.")) {
                if (nested.length() > 0) {
                    nested.append(',');
                }
                nested.append('"').append(e.getKey().substring("amount.".length())).append("\":")
                      .append(e.getValue());
            } else {
                if (top.length() > 0) {
                    top.append(',');
                }
                top.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            }
        }
        return "{" + top + ",\"amount\":{" + nested + "}}";
    }

    private static String encrypt(String plain, String key, String nonce, String aad) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    // ══════════════════ 放行路径 ══════════════════

    @Test
    @DisplayName("验签通过 + GCM 解密成功 → 解析出订单号与金额（分→元）")
    void validCallbackPasses() throws Exception {
        configureChannel(singleCert(), true, API_V3_KEY);
        long ts = now();
        PaymentCallbackResult result = verifier.verify(context(signedBody(businessFields(), platformKeyPair, ts), SERIAL, ts));

        assertEquals("SO-20260921-0001", result.merchantOrderNo());
        assertEquals("4200001234202609211234567890", result.channelOrderNo());
        assertTrue(result.success());
        // 12850 分 → 128.50 元
        assertEquals(0, new BigDecimal("128.50").compareTo(result.paidAmount()));
    }

    @Test
    @DisplayName("证书轮换：新旧序列号并存时，用新证书签名的回调也能验过")
    void rotatedCertificateAccepted() throws Exception {
        configureChannel(rotatedCerts(), true, API_V3_KEY);

        long ts = now();
        PaymentCallbackResult result = verifier.verify(
                context(signedBody(businessFields(), rotatedKeyPair, ts), SERIAL_NEW, ts));
        assertEquals("SO-20260921-0001", result.merchantOrderNo());
    }

    @Test
    @DisplayName("非成功状态（trade_state=NOTPAY）解出但 success=false")
    void notPaidStateParsed() throws Exception {
        configureChannel(singleCert(), true, API_V3_KEY);
        long ts = now();
        Map<String, String> f = businessFields();
        f.put("trade_state", "NOTPAY");
        assertFalse(verifier.verify(context(signedBody(f, platformKeyPair, ts), SERIAL, ts)).success());
    }

    // ══════════════════ 拒绝路径 ══════════════════

    @Test
    @DisplayName("时间戳超容差 → 拒绝（防重放：截获的合法回调不能无限复用）")
    void staleTimestampRejected() throws Exception {
        configureChannel(singleCert(), true, API_V3_KEY);
        long old = now() - 3600; // 1 小时前
        String body = signedBody(businessFields(), platformKeyPair, old);
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(body, SERIAL, old)));
    }

    @Test
    @DisplayName("未知证书序列号 → 拒绝（轮换未配置 / 伪造）")
    void unknownSerialRejected() throws Exception {
        configureChannel(singleCert(), true, API_V3_KEY);
        long ts = now();
        String body = signedBody(businessFields(), platformKeyPair, ts);
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(body, "UNKNOWN_SERIAL_0000", ts)));
    }

    @Test
    @DisplayName("签名合法但密文被改 → GCM 校验失败 → 拒绝（只看签名会漏掉这层）")
    void tamperedCiphertextRejected() throws Exception {
        configureChannel(singleCert(), true, API_V3_KEY);
        long ts = now();
        // 先把密文改一个字符，**再对改后的报文签名** —— 于是签名是合法的，
        // 只有 AES-GCM 的完整性校验能挡下它。这正是「验签过了不代表报文没被改」的用例。
        String tamperedBody = envelope("AAAA" + "tamperedCiphertextNotBase64Gcm");
        String signature = sign(tamperedBody, platformKeyPair, ts);
        String bodyAndSign = tamperedBody + "@@SIGN@@" + signature;
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(bodyAndSign, SERIAL, ts)));
    }

    @Test
    @DisplayName("APIv3 密钥配错 → 解密失败 → 拒绝")
    void wrongApiV3KeyRejected() throws Exception {
        String certs = "{\"" + SERIAL + "\":\"" + pem(platformKeyPair.getPublic()).replace("\n", "\\n") + "\"}";
        configureChannel(singleCert(), true, "ffffffffffffffffffffffffffffffff"); // 换了密钥
        long ts = now();
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(signedBody(businessFields(), platformKeyPair, ts), SERIAL, ts)));
    }

    @Test
    @DisplayName("缺签名头 → 拒绝")
    void missingHeadersRejected() throws Exception {
        configureChannel(singleCert(), true, API_V3_KEY);
        PaymentCallbackContext ctx = new PaymentCallbackContext(
                TENANT_ID, "WECHAT", "{}".getBytes(StandardCharsets.UTF_8), Map.of(), "127.0.0.1");
        assertThrows(PaymentCallbackVerificationException.class, () -> verifier.verify(ctx));
    }

    @Test
    @DisplayName("APIv3 密钥长度不是 32 字节 → isConfigured=false（fail-closed）")
    void shortApiV3KeyNotConfigured() throws Exception {
        configureChannel(singleCert(), true, "tooshort");
        assertFalse(verifier.isConfigured(TENANT_ID));
    }

    @Test
    @DisplayName("未配置平台证书 → isConfigured=false")
    void missingCertsNotConfigured() throws Exception {
        configureChannel(new LinkedHashMap<>(), true, API_V3_KEY);
        assertFalse(verifier.isConfigured(TENANT_ID));
    }

    @Test
    @DisplayName("渠道停用 → isConfigured=false 且验签拒绝")
    void disabledChannelRejected() throws Exception {
        String certs = "{\"" + SERIAL + "\":\"" + pem(platformKeyPair.getPublic()).replace("\n", "\\n") + "\"}";
        configureChannel(singleCert(), false, API_V3_KEY);
        assertFalse(verifier.isConfigured(TENANT_ID));
        long ts = now();
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(signedBody(businessFields(), platformKeyPair, ts), SERIAL, ts)));
    }

    @Test
    @DisplayName("应答体是微信要求的 JSON 形状（非纯文本 success）")
    void ackBodyIsWechatJson() {
        assertTrue(verifier.ackBody(true).contains("\"code\":\"SUCCESS\""));
        assertFalse(verifier.ackBody(false).contains("\"code\":\"SUCCESS\""));
    }
}
