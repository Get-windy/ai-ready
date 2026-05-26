package cn.aiedge.base.service.message.impl;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.service.message.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件发送实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSenderImpl implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String fromEmail;

    @Override
    public boolean send(SysMessage message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return sendText(message.getReceiverContact(), message.getTitle(), message.getContent());
        }
        
        // 如果内容包含HTML标签，使用HTML发送
        if (message.getContent().contains("<") && message.getContent().contains(">")) {
            return sendHtml(message.getReceiverContact(), message.getTitle(), message.getContent());
        }
        
        return sendText(message.getReceiverContact(), message.getTitle(), message.getContent());
    }

    @Override
    public boolean sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
            return true;
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: to={}, subject={}", to, subject, e);
            return false;
        }
    }

    @Override
    public boolean sendText(String to, String subject, String textContent) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(textContent);
            
            mailSender.send(message);
            log.info("文本邮件发送成功: to={}, subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("文本邮件发送失败: to={}, subject={}", to, subject, e);
            return false;
        }
    }
}
