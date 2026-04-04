package cn.aiedge.notification.channel;

import cn.aiedge.notification.entity.NotificationRecord;
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
 * 邮件渠道
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Override
    public SendResult send(NotificationRecord record) {
        if (!isAvailable()) {
            return SendResult.failure("邮件服务未配置");
        }

        try {
            String receiver = record.getReceiverAddress();
            if (receiver == null || receiver.isEmpty()) {
                return SendResult.failure("收件人地址为空");
            }

            // 判断是否HTML内容
            if (isHtmlContent(record.getContent())) {
                sendHtmlMail(receiver, record.getTitle(), record.getContent());
            } else {
                sendSimpleMail(receiver, record.getTitle(), record.getContent());
            }

            log.info("邮件发送成功: to={}, subject={}", receiver, record.getTitle());
            return SendResult.success();

        } catch (MessagingException e) {
            log.error("邮件发送失败: recordId={}", record.getId(), e);
            return SendResult.failure("邮件发送失败: " + e.getMessage());
        }
    }

    private void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    private void sendHtmlMail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
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
        return mailSender != null && from != null && !from.isEmpty();
    }
}
