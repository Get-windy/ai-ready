package cn.aiedge.payment.callback;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

/**
 * 支付渠道凭据的**格式**校验。
 *
 * <h2>为什么「非空」不够</h2>
 * 渠道是否生效，判定依据必须是「凭据**能用**」，而不是「字段填了东西」：
 * <ul>
 *   <li>填了半截公钥、填成私钥、把 PEM 头尾丢了、粘了多余空白 —— 从「非空」看都是配了，
 *       但验签必然失败。此时渠道如果还显示「已启用/可用」，管理员会以为配好了，
 *       直到真收到回调才发现 —— 而那时订单已经挂在那了。</li>
 *   <li>所以 {@code isConfigured} 一律走**真解析**：能不能构造出 RSA 公钥。
 *       解析不出来就等同于「未配置」⇒ 渠道不启用、不生效。</li>
 * </ul>
 *
 * <p>注意这里只校验**格式**，不校验**归属**（这张公钥是不是本商户的）。
 * 归属只能在验签时由真实回调证明 —— 格式校验的价值是把「明显的错」挡在配置阶段。</p>
 */
@Slf4j
public final class PaymentCredentialValidator {

    private PaymentCredentialValidator() {
    }

    /**
     * 能否解析为合法的 RSA 公钥（接受带 PEM 头尾或纯 Base64 两种写法）。
     *
     * @return true = 可用；false = 未填 / 解析失败，两者对调用方的含义相同：**不可用**
     */
    public static boolean isValidRsaPublicKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            String base64 = key.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            byte[] bytes = Base64.getDecoder().decode(base64);
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(bytes));
            // generatePublic 对部分畸形输入不抛异常，这里再用算法名兜一道
            return publicKey != null && "RSA".equalsIgnoreCase(publicKey.getAlgorithm());
        } catch (Exception e) {
            // Debug 级别：配置页每次列表都会调用，非法值不值得打 Warn 刷日志
            log.debug("公钥解析失败（按未配置处理）：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 证书表是否**每一张**都可解析。
     *
     * <p>取「全部有效」而非「至少一张有效」：证书表里混进一张坏证书时，
     * 若按「至少一张」放行，那张坏证书对应的 serial/certId 一旦被使用就会验签失败 ——
     * 表现为「时好时坏」，比直接判不可用难查得多。</p>
     */
    public static boolean allCertsValid(Map<String, String> certs) {
        if (certs == null || certs.isEmpty()) {
            return false;
        }
        return certs.values().stream().allMatch(PaymentCredentialValidator::isValidRsaPublicKey);
    }

    /** 便捷重载：对一组 PEM 文本判「全部有效」。 */
    public static boolean allCertsValid(Collection<String> pems) {
        if (pems == null || pems.isEmpty()) {
            return false;
        }
        return pems.stream().allMatch(PaymentCredentialValidator::isValidRsaPublicKey);
    }

    /** 微信 APIv3 密钥必须是 32 字节（AES-256）。 */
    public static boolean isValidApiV3Key(String key) {
        return key != null && key.getBytes(java.nio.charset.StandardCharsets.UTF_8).length == 32;
    }
}
