package cn.aiedge.notification.channel;

import cn.aiedge.base.spi.PlatformMailSenderFactory;
import cn.aiedge.base.spi.PlatformMailSettingsProvider;
import cn.aiedge.base.spi.PlatformMailSettingsProvider.PlatformMailSettings;
import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件渠道。
 *
 * <p><b>配置来源（2026-09-19 接线）</b>，按优先级：
 * <ol>
 *   <li><b>平台级数据库配置</b> {@code sys_mail_config}（「系统 → 平台设置 → 邮件配置」菜单 62502）
 *       经 {@link PlatformMailSettingsProvider} 读取，**保存即生效**；</li>
 *   <li><b>application yml 的 {@code spring.mail.*}</b> 作为兜底。</li>
 * </ol>
 *
 * <p><b>改造前的两个问题</b>：
 * <ul>
 *   <li>本类原先标注 {@code @ConditionalOnBean(JavaMailSender.class)} —— 而本仓库 yml 里
 *       根本没有 {@code spring.mail.host}，Spring 不会创建 {@code JavaMailSender} bean →
 *       **本渠道从不注册**，邮件通知整体不可用。现已去掉该注解，改为运行时解析。</li>
 *   <li>只读 yml、不读数据库 → 页面上保存的配置毫无作用。现已把数据库配置作为第一优先。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    /** yml 兜底发信器；未配置 spring.mail.host 时为 null */
    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> ymlMailSender;

    /** 平台级配置读取口（core-api 提供实现）；裁剪部署下可能缺失 */
    private final org.springframework.beans.factory.ObjectProvider<PlatformMailSettingsProvider> settingsProvider;

    @Value("${spring.mail.username:}")
    private String ymlFrom;

    @Override
    public SendResult send(NotificationRecord record) {
        Resolved resolved = resolve();
        if (resolved == null) {
            return SendResult.fail("邮件服务未配置（可在「系统 → 平台设置 → 邮件配置」维护，"
                    + "或在 application yml 配置 spring.mail.*）");
        }

        try {
            String receiver = record.getReceiverAddress();
            if (receiver == null || receiver.isEmpty()) {
                return SendResult.fail("收件人地址为空");
            }

            if (isHtmlContent(record.getContent())) {
                sendHtmlMail(resolved, receiver, record.getTitle(), record.getContent());
            } else {
                sendSimpleMail(resolved, receiver, record.getTitle(), record.getContent());
            }

            log.info("邮件发送成功: to={}, subject={}, source={}", receiver, record.getTitle(), resolved.source());
            return SendResult.ok();

        } catch (MessagingException e) {
            log.error("邮件发送失败: recordId={}, source={}", record.getId(), resolved.source(), e);
            return SendResult.fail("邮件发送失败: " + e.getMessage());
        }
    }

    private void sendSimpleMail(Resolved resolved, String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolved.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        resolved.sender().send(message);
    }

    private void sendHtmlMail(Resolved resolved, String to, String subject, String content)
            throws MessagingException {
        MimeMessage message = resolved.sender().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(resolved.from());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        resolved.sender().send(message);
    }

    private boolean isHtmlContent(String content) {
        if (content == null) return false;
        return content.contains("<html") || content.contains("<body") || content.contains("<div");
    }

    @Override
    public String getChannelType() {
        return NotificationTemplate.TYPE_EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return resolve() != null;
    }

    /**
     * 解析本次发信使用的发信器与发件人。
     *
     * @return 解析结果；两个来源都不可用时返回 {@code null}
     */
    private Resolved resolve() {
        PlatformMailSettingsProvider provider = settingsProvider.getIfAvailable();
        PlatformMailSettings settings = provider == null ? null : provider.currentMailSettings();
        if (settings != null && settings.usable()) {
            return new Resolved(PlatformMailSenderFactory.build(settings),
                    PlatformMailSenderFactory.resolveFrom(settings), "db:sys_mail_config");
        }
        JavaMailSender yml = ymlMailSender.getIfAvailable();
        if (yml != null) {
            return new Resolved(yml, ymlFrom == null ? "" : ymlFrom, "yml:spring.mail");
        }
        return null;
    }

    /** 本次发信解析出的发信器 + 发件人 + 来源（来源仅用于日志定位问题） */
    private record Resolved(JavaMailSender sender, String from, String source) {
    }
}
