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
 * 支付宝通道「下单签名」的**闭合回路**验证（2026-09-26）。
 *
 * <p><b>为什么用这种方式验</b>：真接支付宝需要商户号与证书，构建环境既没有也不出网 ——
 * 但"能不能收款"里最容易被写错、又最难在事后发现的，是**签名口径**：
 * 待签串拼错一个字符，真实联调时表现为"{@code sign check fail}"，而本地一切正常。
 *
 * <p>所以这里用**自生成密钥对**做闭环：把私钥配给通道去签名，再用对应公钥按**与回调验签器
 * 完全相同的那份实现**（{@link Rsa2}）去验签。它证明的是：</p>
 * <ol>
 *   <li>下单产生的收银台 URL 里的 {@code sign} 与参数**自洽**；</li>
 *   <li>签名口径与验签口径是同一套（同一份 Rsa2 实现）；</li>
 *   <li>参数被改动后签名必然不通过（伪造会被挡住）。</li>
 * </ol>
 * <p><b>它不能证明</b>：与支付宝真实网关的一致（那要沙箱密钥 + 出网，见类注释的边界说明）。</p>
 */
class AlipayChannelTest {

    private ChannelCredentialAccessor accessor;
    private AlipayChannel channel;
    private String privateKeyBase64;
    private String publicKeyBase64;
    private PaymentChannelParam param;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        param = new PaymentChannelParam();
        param.setEnabled(true);
        param.setAppId("2021000000000000");
        param.setAlipayPrivateKey(privateKeyBase64);
        param.setAlipayPublicKey(publicKeyBase64);
        param.setNotifyUrl("https://mall.example.com/api/payment/callback/1/ALIPAY");

        accessor = mock(ChannelCredentialAccessor.class);
        when(accessor.read(any(String.class), eq(PaymentChannelParam.class))).thenReturn(param);
        channel = new AlipayChannel(accessor);
    }

    private PaymentRequest request(String bizNo, String amount) {
        PaymentRequest r = new PaymentRequest();
        r.setBizNo(bizNo);
        r.setBizType("SALE_ORDER");
        r.setAmount(new BigDecimal(amount));
        r.setChannel("ALIPAY");
        r.setRemark("商城订单支付");
        return r;
    }

    /** 从收银台 URL 的 query 里取参数（值已 URL 编码，需解码）。 */
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

    // ───────────────────────── 核心：签名自洽 ─────────────────────────

    @Test
    @DisplayName("下单：收银台 URL 里的签名能被同一套口径验过（闭合回路）")
    void createPayment_signatureVerifies() {
        PaymentRequest req = request("XSDD-20260927-001", "12.34");
        ChannelPayResult result = channel.createPayment(req);

        // 渠道单号 = 商户单号（后续查单/关单/退款都以它为键）
        assertEquals("XSDD-20260927-001", result.channelOrderNo());
        assertNotNull(result.payUrl(), "支付宝必须返回收银台 URL");

        Map<String, String> params = queryOf(result.payUrl());
        assertEquals("alipay.trade.page.pay", params.get("method"));
        assertEquals("2021000000000000", params.get("app_id"));
        String bizContent = params.get("biz_content");
        assertTrue(bizContent.contains("\"out_trade_no\":\"XSDD-20260927-001\""), bizContent);
        assertTrue(bizContent.contains("\"total_amount\":\"12.34\""), "金额必须两位小数（元）：" + bizContent);
        assertEquals("RSA2", params.get("sign_type"));

        String content = Rsa2.buildSignContent(params, Set.of("sign", "sign_type"));
        assertTrue(Rsa2.verify(content, params.get("sign"), publicKeyBase64),
                "用同一口径必须验签通过 —— 否则真实网关会报 sign check fail");
    }

    @Test
    @DisplayName("篡改任一参数后，原签名必须验不过")
    void tamperedParamsFailVerification() {
        ChannelPayResult result = channel.createPayment(request("XSDD-20260927-002", "100.00"));
        Map<String, String> params = queryOf(result.payUrl());
        String sign = params.get("sign");

        // 把金额改成 0.01（经典篡改：改价）
        params.put("biz_content", params.get("biz_content").replace("100.00", "0.01"));
        String tampered = Rsa2.buildSignContent(params, Set.of("sign", "sign_type"));
        assertFalse(Rsa2.verify(tampered, sign, publicKeyBase64), "改价后签名必须失效");
    }

    @Test
    @DisplayName("换一把公钥就验不过（防止拿别人的签名冒充）")
    void wrongPublicKeyFails() throws Exception {
        ChannelPayResult result = channel.createPayment(request("XSDD-20260927-003", "5.00"));
        Map<String, String> params = queryOf(result.payUrl());
        String content = Rsa2.buildSignContent(params, Set.of("sign", "sign_type"));

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String otherPublic = Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPublic().getEncoded());
        assertFalse(Rsa2.verify(content, params.get("sign"), otherPublic));
    }

    // ───────────────────────── 配置与失败方向 ─────────────────────────

    @Test
    @DisplayName("缺应用私钥时：下单直接报错，绝不「签个空的」往下走")
    void missingPrivateKeyFailsFast() {
        param.setAlipayPrivateKey(null);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> channel.createPayment(request("XSDD-20260927-004", "1.00")));
        assertTrue(e.getMessage().contains("私钥"), e.getMessage());
    }

    @Test
    @DisplayName("渠道未启用时下单报错")
    void disabledChannelRejects() {
        param.setEnabled(false);
        assertThrows(IllegalStateException.class,
                () -> channel.createPayment(request("XSDD-20260927-005", "1.00")));
    }

    @Test
    @DisplayName("isAvailable 只在凭据齐备时为 true")
    void isAvailableReflectsCredentials() {
        assertTrue(channel.isAvailable());
        param.setAlipayPrivateKey(null);
        assertFalse(channel.isAvailable(), "没私钥就不可能下单成功，不能显示为「可用」");
        param.setAlipayPrivateKey(privateKeyBase64);
        param.setAppId(null);
        assertFalse(channel.isAvailable());
    }

    @Test
    @DisplayName("缺 bizNo 时拒绝（没有 out_trade_no 就没法对账）")
    void missingBizNoRejects() {
        PaymentRequest req = request("XSDD-20260927-006", "1.00");
        req.setBizNo(null);
        assertThrows(IllegalStateException.class, () -> channel.createPayment(req));
    }
}
