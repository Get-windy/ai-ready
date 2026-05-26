package cn.aiedge.base.service.message.impl;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.service.message.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailSenderImpl implements EmailSender {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String fromEmail;

    @Override
    public boolean send(SysMessage message) {
        if (mailSender == null) {
            log.warn("邮件发送器未配置，无法发送邮件: to={}", message.getReceiverContact());
            return false;
        }
        
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
        if (mailSender == null) {
            log.warn("邮件发送器未配置，无法发送HTML邮件: to={}", to);
            return false;
        }
        
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
        if (mailSender == null) {
            log.warn("邮件发送器未配置，无法发送文本邮件: to={}", to);
            return false;
        }
        
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