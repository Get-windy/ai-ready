package cn.aiedge.payment.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 「**未配置 / 配置了无效字段 ⇒ 渠道不生效**」的语义测试（2026-09-21）。
 *
 * <p>用户明确要求：三个线上渠道只要没配相关密钥字段，就默认不启用不生效；
 * 配了无效值同样不生效。</p>
 *
 * <p>这条语义的判据是渠道的 {@code isConfigured} —— 它同时决定：
 * ① 回调是否被拒；② 「支付配置」页里该渠道是否显示为可用。</p>
 *
 * <p>本测试的重点是**「无效」而不是「为空」**：填了半截公钥、把私钥当公钥填、
 * 证书表里混一张坏证书 —— 这些从「字段非空」看都是配了，但实际必然验签失败。
 * 只测「空值不生效」会漏掉这整类。</p>
 */
class PaymentCredentialValidationTest {

    private static final Long TENANT_ID = 1L;

    private static final com.fasterxml.jackson.databind.ObjectMapper OM =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private TenantChannelCredentialReader credentialReader;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        credentialReader = mock(TenantChannelCredentialReader.class);
    }

    private static String publicKeyPem(java.security.Key key) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(key.getEncoded()) + "\n-----END PUBLIC KEY-----";
    }

    /** 私钥的 Base64（PKCS#8）—— 用来验证「把私钥填进公钥字段」会被判无效。 */
    private String privateKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    // ══════════════════ 校验器本身 ══════════════════

    @Test
    @DisplayName("公钥校验：合法 PEM 通过；空/垃圾/私钥 一律不通过")
    void publicKeyValidation() {
        assertTrue(PaymentCredentialValidator.isValidRsaPublicKey(publicKeyPem(keyPair.getPublic())));
        // 纯 Base64（无 PEM 头尾）也应接受 —— 各渠道粘贴习惯不同
        assertTrue(PaymentCredentialValidator.isValidRsaPublicKey(
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())));

        assertFalse(PaymentCredentialValidator.isValidRsaPublicKey(null), "null 应判无效");
        assertFalse(PaymentCredentialValidator.isValidRsaPublicKey(""), "空串应判无效");
        assertFalse(PaymentCredentialValidator.isValidRsaPublicKey("   "), "空白应判无效");
        assertFalse(PaymentCredentialValidator.isValidRsaPublicKey("not-a-key"), "垃圾串应判无效");
        assertFalse(PaymentCredentialValidator.isValidRsaPublicKey("-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----"),
                "Base64 合法但内容不是密钥，应判无效");
        assertFalse(PaymentCredentialValidator.isValidRsaPublicKey(privateKeyBase64()),
                "**把私钥填进公钥字段**应判无效（这是最常见的配错）");
    }

    @Test
    @DisplayName("APIv3 密钥必须 32 字节")
    void apiV3KeyValidation() {
        assertTrue(PaymentCredentialValidator.isValidApiV3Key("0123456789abcdef0123456789abcdef"));
        assertFalse(PaymentCredentialValidator.isValidApiV3Key("tooshort"));
        assertFalse(PaymentCredentialValidator.isValidApiV3Key(""));
        assertFalse(PaymentCredentialValidator.isValidApiV3Key(null));
    }

    @Test
    @DisplayName("证书表「全部有效」才通过：混一张坏证书即整体不可用")
    void certTableRequiresAllValid() {
        Map<String, String> good = Map.of("S1", publicKeyPem(keyPair.getPublic()));
        assertTrue(PaymentCredentialValidator.allCertsValid(good));

        Map<String, String> mixed = new LinkedHashMap<>();
        mixed.put("S1", publicKeyPem(keyPair.getPublic()));
        mixed.put("S2", "broken");
        assertFalse(PaymentCredentialValidator.allCertsValid(mixed),
                "混进坏证书应整体判不可用（否则会「时好时坏」，比直接失败更难查）");

        assertFalse(PaymentCredentialValidator.allCertsValid(Map.of()), "空表应判无效");
        assertFalse(PaymentCredentialValidator.allCertsValid((Map<String, String>) null), "null 应判无效");
    }

    // ══════════════════ 三个渠道：无效字段 ⇒ 不生效 ══════════════════

    @Test
    @DisplayName("支付宝：公钥字段填了无效内容 → isConfigured=false（不生效）")
    void alipayInvalidKeyNotConfigured() throws Exception {
        AlipayCallbackVerifier verifier = new AlipayCallbackVerifier(credentialReader);

        mockConfig(AlipayCallbackVerifier.CONFIG_KEY, Map.of("enabled", true, "alipayPublicKey", "not-a-valid-key"));
        assertFalse(verifier.isConfigured(TENANT_ID), "无效公钥必须等于未配置");

        mockConfig(AlipayCallbackVerifier.CONFIG_KEY, Map.of("enabled", true, "alipayPublicKey", privateKeyBase64()));
        assertFalse(verifier.isConfigured(TENANT_ID), "把私钥当公钥填也必须等于未配置");

        mockConfig(AlipayCallbackVerifier.CONFIG_KEY,
                Map.of("enabled", true, "alipayPublicKey", publicKeyPem(keyPair.getPublic())));
        assertTrue(verifier.isConfigured(TENANT_ID), "合法公钥才生效");
    }

    @Test
    @DisplayName("微信：APIv3 密钥长度不对 / 证书表混坏证书 → isConfigured=false")
    void wechatInvalidCredentialNotConfigured() throws Exception {
        WechatCallbackVerifier verifier = new WechatCallbackVerifier(credentialReader);
        String goodCerts = OM.writeValueAsString(Map.of("S1", publicKeyPem(keyPair.getPublic())));

        mockConfig(WechatCallbackVerifier.CONFIG_KEY,
                Map.of("enabled", true, "wechatApiV3Key", "tooshort", "wechatPlatformCerts", goodCerts));
        assertFalse(verifier.isConfigured(TENANT_ID), "APIv3 密钥长度不对必须不生效");

        mockConfig(WechatCallbackVerifier.CONFIG_KEY, Map.of("enabled", true,
                "wechatApiV3Key", "0123456789abcdef0123456789abcdef",
                "wechatPlatformCerts", "{\"S1\":\"broken\"}"));
        assertFalse(verifier.isConfigured(TENANT_ID), "证书表含坏证书必须不生效");

        mockConfig(WechatCallbackVerifier.CONFIG_KEY, Map.of("enabled", true,
                "wechatApiV3Key", "0123456789abcdef0123456789abcdef",
                "wechatPlatformCerts", goodCerts));
        assertTrue(verifier.isConfigured(TENANT_ID), "密钥 + 合法证书表才生效");
    }

    @Test
    @DisplayName("银联：证书表无效 → isConfigured=false；合法才生效")
    void unionPayInvalidCertNotConfigured() throws Exception {
        UnionPayCallbackVerifier verifier = new UnionPayCallbackVerifier(credentialReader);

        mockConfig(UnionPayCallbackVerifier.CONFIG_KEY,
                Map.of("enabled", true, "unionPayCerts", "{\"C1\":\"broken\"}"));
        assertFalse(verifier.isConfigured(TENANT_ID), "坏证书必须不生效");

        mockConfig(UnionPayCallbackVerifier.CONFIG_KEY, Map.of("enabled", true,
                "unionPayCerts", OM.writeValueAsString(Map.of("C1", publicKeyPem(keyPair.getPublic())))));
        assertTrue(verifier.isConfigured(TENANT_ID), "合法证书才生效");
    }

    @Test
    @DisplayName("三个渠道：完全未配置 → 一律不生效（默认不启用）")
    void nothingConfiguredMeansIneffective() {
        when(credentialReader.read(any(), any())).thenReturn(null);
        assertFalse(new AlipayCallbackVerifier(credentialReader).isConfigured(TENANT_ID));
        assertFalse(new WechatCallbackVerifier(credentialReader).isConfigured(TENANT_ID));
        assertFalse(new UnionPayCallbackVerifier(credentialReader).isConfigured(TENANT_ID));
    }

    @Test
    @DisplayName("显式停用（enabled=false）即便凭据合法也不生效")
    void explicitlyDisabledNotEffective() throws Exception {
        AlipayCallbackVerifier verifier = new AlipayCallbackVerifier(credentialReader);
        mockConfig(AlipayCallbackVerifier.CONFIG_KEY,
                Map.of("enabled", false, "alipayPublicKey", publicKeyPem(keyPair.getPublic())));
        assertFalse(verifier.isConfigured(TENANT_ID));
    }

    /** 把「渠道参数 JSON」写进 mock 的配置服务。 */
    private void mockConfig(String key, Map<String, Object> config) throws Exception {
        when(credentialReader.read(eq(TENANT_ID), eq(key))).thenReturn(OM.writeValueAsString(config));
    }
}
