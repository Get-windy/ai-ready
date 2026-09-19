package cn.aiedge.base.service.message.impl;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.service.message.EmailSender;
import cn.aiedge.base.spi.PlatformMailSenderFactory;
import cn.aiedge.base.spi.PlatformMailSettingsProvider;
import cn.aiedge.base.spi.PlatformMailSettingsProvider.PlatformMailSettings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件发送通道。
 *
 * <p><b>配置来源（2026-09-19 接线）</b>，按优先级：
 * <ol>
 *   <li><b>平台级数据库配置</b> {@code sys_mail_config}（「系统 → 平台设置 → 邮件配置」菜单 62502 维护）
 *       —— 经 {@link PlatformMailSettingsProvider} 读取；**保存即生效**（每次发信实时读取）；</li>
 *   <li><b>application yml 的 {@code spring.mail.*} + {@code spring.mail.from}**
 *       —— 作为兜底（未建平台配置、或平台配置读取失败时）。</li>
 * </ol>
 *
 * <p>接线前只读 yml，而本仓库 yml 里根本没有 {@code spring.mail.host} →
 * `JavaMailSender` bean 不存在 → 邮件发送**恒返回 false**，而页面上保存的配置毫无作用。
 */
@Slf4j
@Component
public class EmailSenderImpl implements EmailSender {

    /** yml 兜底发信器；未配置 spring.mail.host 时 Spring 不会创建该 bean → 为 null */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    /** 平台级配置读取口（core-api 提供实现）；单测或裁剪部署下可能缺失 → 允许为 null */
    @Autowired(required = false)
    private PlatformMailSettingsProvider platformMailSettingsProvider;

    @Value("${spring.mail.from:}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String ymlUsername;

    @Override
    public boolean send(SysMessage message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return sendText(message.getReceiverContact(), message.getTitle(), message.getContent());
        }

        if (message.getContent().contains("<") && message.getContent().contains(">")) {
            return sendHtml(message.getReceiverContact(), message.getTitle(), message.getContent());
        }

        return sendText(message.getReceiverContact(), message.getTitle(), message.getContent());
    }

    @Override
    public boolean sendHtml(String to, String subject, String htmlContent) {
        Resolved resolved = resolve();
        if (resolved == null) {
            log.warn("邮件发送器未配置，无法发送HTML邮件: to={}（可在「系统 → 平台设置 → 邮件配置」维护，"
                    + "或在 application yml 配置 spring.mail.*）", to);
            return false;
        }

        try {
            MimeMessage message = resolved.sender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolved.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            resolved.sender().send(message);
            log.info("HTML邮件发送成功: to={}, subject={}, source={}", to, subject, resolved.source());
            return true;
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: to={}, subject={}, source={}", to, subject, resolved.source(), e);
            return false;
        }
    }

    @Override
    public boolean sendText(String to, String subject, String textContent) {
        Resolved resolved = resolve();
        if (resolved == null) {
            log.warn("邮件发送器未配置，无法发送文本邮件: to={}（可在「系统 → 平台设置 → 邮件配置」维护，"
                    + "或在 application yml 配置 spring.mail.*）", to);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolved.from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(textContent);

            resolved.sender().send(message);
            log.info("文本邮件发送成功: to={}, subject={}, source={}", to, subject, resolved.source());
            return true;
        } catch (Exception e) {
            log.error("文本邮件发送失败: to={}, subject={}, source={}", to, subject, resolved.source(), e);
            return false;
        }
    }

    /**
     * 解析本次发信使用的发信器与发件人。
     *
     * @return 解析结果；两个来源都不可用时返回 {@code null}
     */
    private Resolved resolve() {
        PlatformMailSettings settings = null;
        if (platformMailSettingsProvider != null) {
            settings = platformMailSettingsProvider.currentMailSettings();
        }
        if (settings != null && settings.usable()) {
            return new Resolved(PlatformMailSenderFactory.build(settings),
                    PlatformMailSenderFactory.resolveFrom(settings), "db:sys_mail_config");
        }
        if (mailSender != null) {
            String from = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : ymlUsername;
            return new Resolved(mailSender, from == null ? "" : from, "yml:spring.mail");
        }
        return null;
    }

    /** 本次发信解析出的发信器 + 发件人 + 来源（来源仅用于日志定位问题） */
    private record Resolved(JavaMailSender sender, String from, String source) {
    }
}
