package cn.aiedge.payment.callback;

import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.service.SysConfigService;
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
 * 支付宝回调验签测试 —— **不需要真实支付宝凭据**。
 *
 * <p>做法：测试内**自生成一对 RSA 密钥**，用私钥按支付宝的规则签名、
 * 把公钥塞进渠道配置，再调 {@link AlipayCallbackVerifier#verify}。
 * 这样验证的是**验签逻辑本身**（待验签串怎么拼、什么该拒），
 * 而不是「能不能连上支付宝」。</p>
 *
 * <p>为什么必须有这层测试：验签是「错了也不报错、只是形同虚设」的典型代码 ——
 * 待验签串拼错（比如把 sign 也算进去、或按插入序而非字典序）会导致**所有回调都验签失败**
 * （功能坏掉，容易被发现）；但若写成「异常就 return true」这类反向错误，
 * 则会**所有回调都通过**（安全形同虚设，且线上完全看不出来）。两种都必须被测试钉死。</p>
 */
class AlipayCallbackVerifierTest {

    private static final Long TENANT_ID = 1L;

    private SysConfigService sysConfigService;
    private AlipayCallbackVerifier verifier;
    private KeyPair keyPair;
    private String appId;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        appId = "2021000000000000";

        sysConfigService = mock(SysConfigService.class);
        verifier = new AlipayCallbackVerifier(sysConfigService);
    }

    /** 把公钥写进渠道配置（模拟租户已配置支付宝渠道）。 */
    private void configureChannel(String configuredAppId, boolean enabled) {
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String json = String.format(
                "{\"appId\":\"%s\",\"merchantNo\":\"m1\",\"enabled\":%s,\"alipayPublicKey\":\"%s\"}",
                configuredAppId, enabled, publicKey);
        when(sysConfigService.getValue(eq(AlipayCallbackVerifier.CONFIG_KEY), any())).thenReturn(json);
    }

    /** 按支付宝规则构造并签名的回调报文。 */
    private String signedBody(Map<String, String> params) throws Exception {
        String content = AlipayCallbackVerifier.buildSignContent(params);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(content.getBytes(StandardCharsets.UTF_8));
        String sign = Base64.getEncoder().encodeToString(sig.sign());

        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("sign", sign);
        all.put("sign_type", "RSA2");

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
        return new PaymentCallbackContext(TENANT_ID, "ALIPAY",
                body.getBytes(StandardCharsets.UTF_8), Map.of(), "127.0.0.1");
    }

    private Map<String, String> baseParams() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("app_id", appId);
        p.put("out_trade_no", "SO-20260921-0001");
        p.put("trade_no", "2026092122001400000000000001");
        p.put("trade_status", "TRADE_SUCCESS");
        p.put("total_amount", "128.50");
        return p;
    }

    // ══════════════════ 放行路径 ══════════════════

    @Test
    @DisplayName("合法签名 → 验签通过并解析出业务字段")
    void validSignaturePasses() throws Exception {
        configureChannel(appId, true);
        PaymentCallbackResult result = verifier.verify(context(signedBody(baseParams())));

        assertEquals("SO-20260921-0001", result.merchantOrderNo());
        assertEquals("2026092122001400000000000001", result.channelOrderNo());
        assertEquals(0, new BigDecimal("128.50").compareTo(result.paidAmount()));
        assertTrue(result.success(), "TRADE_SUCCESS 应判定为支付成功");
    }

    @Test
    @DisplayName("TRADE_FINISHED 也算成功（支付宝语义）")
    void tradeFinishedIsSuccess() throws Exception {
        configureChannel(appId, true);
        Map<String, String> p = baseParams();
        p.put("trade_status", "TRADE_FINISHED");
        assertTrue(verifier.verify(context(signedBody(p))).success());
    }

    @Test
    @DisplayName("非成功状态（如 WAIT_BUYER_PAY）验签通过但 success=false")
    void nonSuccessStatusParsedAsNotPaid() throws Exception {
        configureChannel(appId, true);
        Map<String, String> p = baseParams();
        p.put("trade_status", "WAIT_BUYER_PAY");
        assertFalse(verifier.verify(context(signedBody(p))).success());
    }

    // ══════════════════ 拒绝路径 ══════════════════

    @Test
    @DisplayName("篡改金额 → 验签失败（改价重放必须被挡）")
    void tamperedAmountRejected() throws Exception {
        configureChannel(appId, true);
        String body = signedBody(baseParams());
        // 签名之后把金额改掉，模拟中间人改价
        String tampered = body.replace(
                URLEncoder.encode("128.50", StandardCharsets.UTF_8),
                URLEncoder.encode("0.01", StandardCharsets.UTF_8));
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(tampered)));
    }

    @Test
    @DisplayName("用别人的私钥签名 → 验签失败")
    void signatureFromOtherKeyRejected() throws Exception {
        configureChannel(appId, true);
        // 攻击者自己的密钥对签名
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair attacker = gen.generateKeyPair();

        Map<String, String> params = baseParams();
        String content = AlipayCallbackVerifier.buildSignContent(params);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(attacker.getPrivate());
        sig.update(content.getBytes(StandardCharsets.UTF_8));
        String forgedSign = Base64.getEncoder().encodeToString(sig.sign());

        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context("app_id=" + appId + "&out_trade_no=X&sign=" + forgedSign)));
    }

    @Test
    @DisplayName("签名合法但 app_id 不是本租户的 → 拒绝（签名只证明来自支付宝，不证明发给谁）")
    void mismatchedAppIdRejected() throws Exception {
        configureChannel("2021999999999999", true); // 本租户配的是另一个 appId
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(signedBody(baseParams()))));
    }

    @Test
    @DisplayName("缺 sign → 拒绝")
    void missingSignRejected() {
        configureChannel(appId, true);
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context("app_id=" + appId + "&out_trade_no=X&trade_status=TRADE_SUCCESS")));
    }

    @Test
    @DisplayName("渠道未配置公钥 → isConfigured=false（fail-closed 的依据）")
    void notConfiguredWithoutPublicKey() {
        when(sysConfigService.getValue(eq(AlipayCallbackVerifier.CONFIG_KEY), any()))
                .thenReturn("{\"appId\":\"x\",\"enabled\":true}");
        assertFalse(verifier.isConfigured(TENANT_ID));
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context("out_trade_no=X&sign=abc")));
    }

    @Test
    @DisplayName("渠道被停用 → isConfigured=false 且验签拒绝")
    void disabledChannelRejected() throws Exception {
        configureChannel(appId, false);
        assertFalse(verifier.isConfigured(TENANT_ID));
        assertThrows(PaymentCallbackVerificationException.class,
                () -> verifier.verify(context(signedBody(baseParams()))));
    }

    @Test
    @DisplayName("无配置行 → isConfigured=false")
    void noConfigRow() {
        when(sysConfigService.getValue(eq(AlipayCallbackVerifier.CONFIG_KEY), any())).thenReturn(null);
        assertFalse(verifier.isConfigured(TENANT_ID));
    }

    @Test
    @DisplayName("应答体符合支付宝要求（纯文本 success/failure，非 JSON）")
    void ackBodyIsPlainText() {
        assertEquals("success", verifier.ackBody(true));
        assertEquals("failure", verifier.ackBody(false));
    }

    @Test
    @DisplayName("待验签串规则：剔除 sign/sign_type 与空值，按字典序升序")
    void signContentRules() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("out_trade_no", "X1");
        p.put("sign", "SHOULD_BE_EXCLUDED");
        p.put("sign_type", "RSA2");
        p.put("app_id", "A1");
        p.put("empty_field", "");
        // 期望：app_id 在前（字典序），out_trade_no 在后；sign/sign_type/空值都不参与
        assertEquals("app_id=A1&out_trade_no=X1", AlipayCallbackVerifier.buildSignContent(p));
    }
}
