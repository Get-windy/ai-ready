package cn.aiedge.payment.callback;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.payment.PaymentCallbackVerifier;
import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.payment.dto.PaymentChannelParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 APIv3 回调验签 + 解密实现。
 *
 * <h2>微信 V3 的回调比支付宝多两层</h2>
 * <ol>
 *   <li><b>验签</b>：对 {@code 时间戳\n随机串\n原始报文\n}（注意**结尾的 \n**）用
 *       <b>平台证书私钥</b>签名 —— 商户要用 {@code Wechatpay-Serial} 指定的
 *       <b>平台证书公钥</b>验。签名对象是**字节级原始 body**，不能用反序列化后的对象重拼。</li>
 *   <li><b>解密</b>：报文里的业务字段不是明文，而是
 *       {@code resource.ciphertext}（Base64）用 <b>APIv3 密钥</b>做
 *       <b>AES-256-GCM</b> 加密，{@code resource.nonce} 是 IV、{@code associated_data} 是 AAD。
 *       只验签不解密拿不到订单号，只解密不验签等于谁都能伪造。</li>
 * </ol>
 *
 * <h2>三个容易漏的点（都按「漏了会怎样」写在实现里）</h2>
 * <ul>
 *   <li><b>时间戳容差</b>：不校验时间戳，则一条**被截获的合法回调**可以被无限重放。
 *       这里按微信的建议给 ±5 分钟容差，超出即拒。</li>
 *   <li><b>证书轮换</b>：平台证书会定期轮换、轮换期新旧并存。
 *       故凭据是「序列号 → 公钥」的**映射**（见 {@code wechatPlatformCerts}），
 *       只配一张证书的话，轮换当天全部回调验签失败。</li>
 *   <li><b>金额单位</b>：微信 {@code amount.total} 是**分**，本系统订单金额是**元**，
 *       必须换算后再比对，否则会当成「金额不符」把正常回调全拒掉。</li>
 * </ul>
 *
 * <h2>应答体</h2>
 * 微信要求 JSON：成功 {@code {"code":"SUCCESS","message":"成功"}}；
 * 失败用非 SUCCESS 的 code，微信会按策略重推。故 {@link #ackBody(boolean)} 返回 JSON 而非纯文本。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatCallbackVerifier implements PaymentCallbackVerifier {

    public static final String CHANNEL_CODE = "WECHAT";

    /** 渠道参数在 sys_project_config 中的键（小写渠道码，见 PaymentConfigCatalog） */
    public static final String CONFIG_KEY = "payment.channel.wechat";

    private static final String HEADER_TIMESTAMP = "Wechatpay-Timestamp";
    private static final String HEADER_NONCE = "Wechatpay-Nonce";
    private static final String HEADER_SIGNATURE = "Wechatpay-Signature";
    private static final String HEADER_SERIAL = "Wechatpay-Serial";

    /** 时间戳容差（秒）：微信建议 5 分钟。防重放，不是防时钟漂移。 */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300L;

    /** 微信语义下的支付成功 */
    private static final String TRADE_STATE_SUCCESS = "SUCCESS";

    /** APIv3 密钥长度固定 32 字节（AES-256） */
    private static final int API_V3_KEY_LENGTH = 32;

    private final SysConfigService sysConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean isConfigured(Long tenantId) {
        PaymentChannelParam param = loadParam(tenantId);
        return param != null
                && Boolean.TRUE.equals(param.getEnabled())
                && isValidApiV3Key(param.getWechatApiV3Key())
                && !parseCerts(param.getWechatPlatformCerts()).isEmpty();
    }

    @Override
    public PaymentCallbackResult verify(PaymentCallbackContext context) {
        PaymentChannelParam param = loadParam(context.tenantId());
        if (param == null || !Boolean.TRUE.equals(param.getEnabled())) {
            throw new PaymentCallbackVerificationException("微信支付渠道未启用，拒绝回调");
        }
        if (!isValidApiV3Key(param.getWechatApiV3Key())) {
            throw new PaymentCallbackVerificationException("微信 APIv3 密钥未配置或长度不是 32 字节");
        }
        Map<String, String> certs = parseCerts(param.getWechatPlatformCerts());
        if (certs.isEmpty()) {
            throw new PaymentCallbackVerificationException("微信平台证书未配置，无法验签");
        }

        String timestamp = headerIgnoreCase(context, HEADER_TIMESTAMP);
        String nonce = headerIgnoreCase(context, HEADER_NONCE);
        String signature = headerIgnoreCase(context, HEADER_SIGNATURE);
        String serial = headerIgnoreCase(context, HEADER_SERIAL);
        if (isBlank(timestamp) || isBlank(nonce) || isBlank(signature) || isBlank(serial)) {
            throw new PaymentCallbackVerificationException("回调缺少微信签名头（Timestamp/Nonce/Signature/Serial）");
        }

        assertTimestampFresh(timestamp);

        String certPem = certs.get(serial);
        if (certPem == null) {
            // 轮换期常见的两种原因：① 平台刚换成新证书而本租户还没配；② 伪造。
            // 两种都不能放行，且要把 serial 打进日志便于运维定位。
            throw new PaymentCallbackVerificationException(
                    "回调使用了未知的平台证书序列号：" + serial + "（已配置 " + certs.size() + " 张）");
        }

        // ⚠️ 签名对象是「时间戳\n随机串\n原始报文\n」——结尾那个 \n 不能少
        String message = timestamp + "\n" + nonce + "\n" + context.rawBodyAsString() + "\n";
        if (!verifyRsa2(message, signature, certPem)) {
            throw new PaymentCallbackVerificationException("微信支付回调验签失败");
        }

        String plainResource = decryptResource(context.rawBodyAsString(), param.getWechatApiV3Key());

        return parseDecrypted(plainResource);
    }

    @Override
    public String ackBody(boolean success) {
        // 微信要求 JSON 应答；失败用非 SUCCESS 的 code，微信会按策略重推
        return success
                ? "{\"code\":\"SUCCESS\",\"message\":\"成功\"}"
                : "{\"code\":\"FAIL\",\"message\":\"验签或处理失败\"}";
    }

    // ─────────────────────────── 验签与解密 ───────────────────────────

    /** 时间戳容差校验：超出 ±5 分钟即拒（防重放）。 */
    private void assertTimestampFresh(String timestamp) {
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            throw new PaymentCallbackVerificationException("回调时间戳非法：" + timestamp);
        }
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long diff = Math.abs(nowSeconds - ts);
        if (diff > TIMESTAMP_TOLERANCE_SECONDS) {
            throw new PaymentCallbackVerificationException(
                    "回调时间戳超出容差（相差 " + diff + " 秒，上限 " + TIMESTAMP_TOLERANCE_SECONDS + " 秒），疑似重放");
        }
    }

    /**
     * AES-256-GCM 解密 {@code resource}，返回明文 JSON 文本。
     */
    private String decryptResource(String body, String apiV3Key) {
        JsonNode resource;
        try {
            resource = objectMapper.readTree(body).path("resource");
        } catch (Exception e) {
            throw new PaymentCallbackVerificationException("回调报文不是合法 JSON：" + e.getMessage(), e);
        }
        String ciphertext = resource.path("ciphertext").asText(null);
        String nonce = resource.path("nonce").asText(null);
        String associatedData = resource.path("associated_data").asText("");
        if (isBlank(ciphertext) || isBlank(nonce)) {
            throw new PaymentCallbackVerificationException("回调 resource 缺少 ciphertext 或 nonce");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            if (!associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败通常意味着 APIv3 密钥配错，或报文本就是伪造的
            throw new PaymentCallbackVerificationException("回调 resource 解密失败：" + e.getMessage(), e);
        }
    }

    /** 解析解密后的业务明文，转成渠道无关结果。 */
    private PaymentCallbackResult parseDecrypted(String plain) {
        JsonNode node;
        try {
            node = objectMapper.readTree(plain);
        } catch (Exception e) {
            throw new PaymentCallbackVerificationException("解密后的报文体不是合法 JSON：" + e.getMessage(), e);
        }
        String tradeState = node.path("trade_state").asText("");
        boolean success = TRADE_STATE_SUCCESS.equalsIgnoreCase(tradeState);

        // ⚠️ 微信 amount.total 单位是「分」，本系统订单金额是「元」，必须换算
        BigDecimal paidAmount = null;
        JsonNode total = node.path("amount").path("total");
        if (total.isNumber()) {
            paidAmount = BigDecimal.valueOf(total.asLong())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        return new PaymentCallbackResult(
                node.path("out_trade_no").asText(null),
                node.path("transaction_id").asText(null),
                paidAmount,
                success,
                plain);
    }

    static boolean verifyRsa2(String message, String signBase64, String publicKeyPem) {
        try {
            byte[] keyBytes = Base64.getDecoder()
                    .decode(publicKeyPem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", ""));
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            log.warn("微信 RSA2 验签异常，按不通过处理：{}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────── 凭据读取 ───────────────────────────

    /** 按租户读取渠道参数；回调无会话，用临时租户，**必须 finally 清理**。 */
    private PaymentChannelParam loadParam(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        MyBatisPlusConfig.setTempTenantId(tenantId);
        try {
            String raw = sysConfigService.getValue(CONFIG_KEY, null);
            if (isBlank(raw)) {
                return null;
            }
            return objectMapper.readValue(raw, PaymentChannelParam.class);
        } catch (Exception e) {
            log.warn("读取微信支付渠道配置失败：tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        } finally {
            MyBatisPlusConfig.clearTempTenantId();
        }
    }

    /** 解析「序列号 → PEM 公钥」映射；解析失败返回空表（fail-closed）。 */
    private Map<String, String> parseCerts(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (isBlank(json)) {
            return map;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) {
                return map;
            }
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (!isBlank(e.getKey()) && !isBlank(e.getValue().asText())) {
                    map.put(e.getKey(), e.getValue().asText());
                }
            }
        } catch (Exception e) {
            log.warn("微信平台证书表不是合法 JSON，按未配置处理：{}", e.getMessage());
        }
        return map;
    }

    private boolean isValidApiV3Key(String key) {
        return key != null && key.getBytes(StandardCharsets.UTF_8).length == API_V3_KEY_LENGTH;
    }

    /** 请求头大小写不敏感查找（微信实际发的就是 Wechatpay-Timestamp 这种形式）。 */
    private String headerIgnoreCase(PaymentCallbackContext ctx, String name) {
        String direct = ctx.header(name);
        if (direct != null) {
            return direct;
        }
        if (ctx.headers() == null) {
            return null;
        }
        for (Map.Entry<String, String> e : ctx.headers().entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
