package cn.aiedge.base.spi;

import cn.aiedge.base.spi.PlatformMailSettingsProvider.PlatformMailSettings;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 由**平台级邮件配置**（`sys_mail_config`）构造一个可用的 {@link JavaMailSenderImpl}。
 *
 * <p>抽出来的原因：`core-base` 的 {@code EmailSenderImpl} 与 `core-notification` 的
 * {@code EmailChannel} 都要做同一件事，两边各写一份必然走样（超时/加密映射一旦不一致，
 * 就会出现"一个渠道能发、另一个发不出"的怪象）。
 *
 * <p>注意：`JavaMailSenderImpl` **只保存配置、不持有连接**（连接在每次 send 时建立），
 * 因此按次构造的开销可以忽略，不需要缓存 —— 也就顺带得到了「页面保存后**下一次发信即生效**」
 * 的热生效语义，不必再引入缓存失效机制。
 */
public final class PlatformMailSenderFactory {

    private PlatformMailSenderFactory() {
    }

    /**
     * 按平台配置构造发信器。
     *
     * @param s 平台邮件配置（调用方需先确认 {@link PlatformMailSettings#usable()}）
     * @return 配置好的发信器
     */
    public static JavaMailSenderImpl build(PlatformMailSettings s) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.host());
        sender.setPort(s.port() == null || s.port() <= 0 ? 465 : s.port());
        sender.setUsername(s.username());
        sender.setPassword(s.password());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        // 超时必须有：否则 SMTP 不可达时会挂住发送线程（MessageSendTask 是串行消费的）
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        String encryption = s.encryption() == null ? "" : s.encryption().trim().toUpperCase();
        switch (encryption) {
            case "SSL", "SMTPS" -> props.put("mail.smtp.ssl.enable", "true");
            case "TLS", "STARTTLS" -> props.put("mail.smtp.starttls.enable", "true");
            default -> {
                // 空值或未知值 → 按明文处理；端口 465 却配成明文是常见误配，
                // 但这里不猜用户意图（猜错会把"配错了"变成"看起来能发其实发不出"）
            }
        }
        return sender;
    }

    /**
     * 解析发件人地址：优先 `from_address`，为空则回退到认证用户名
     * （多数 SMTP 服务要求 From 与认证账号一致，回退比留空更实用）。
     */
    public static String resolveFrom(PlatformMailSettings s) {
        String from = s.fromAddress();
        if (from != null && !from.isBlank()) {
            return from.trim();
        }
        return s.username() == null ? "" : s.username().trim();
    }
}
