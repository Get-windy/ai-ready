package com.aiready.notification.service.impl;

import com.aiready.notification.entity.EmailRecord;
import com.aiready.notification.mapper.EmailRecordMapper;
import com.aiready.notification.service.EmailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl extends ServiceImpl<EmailRecordMapper, EmailRecord> 
        implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailRecordMapper emailRecordMapper;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        EmailRecord record = new EmailRecord();
        record.setToEmail(toEmail);
        record.setSubject(subject);
        record.setContent(content);
        record.setStatus(1);
        save(record);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            
            record.setStatus(2);
            record.setSendTime(LocalDateTime.now());
            updateById(record);
            
            log.info("邮件发送成功: {}", toEmail);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
            record.setStatus(3);
            record.setFailReason(e.getMessage());
            updateById(record);
        }
    }

    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        EmailRecord record = new EmailRecord();
        record.setToEmail(toEmail);
        record.setSubject(subject);
        record.setContent(htmlContent);
        record.setStatus(1);
        save(record);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            record.setStatus(2);
            record.setSendTime(LocalDateTime.now());
            updateById(record);
            
            log.info("HTML邮件发送成功: {}", toEmail);
        } catch (Exception e) {
            log.error("HTML邮件发送失败: {}", e.getMessage());
            record.setStatus(3);
            record.setFailReason(e.getMessage());
            updateById(record);
        }
    }

    @Override
    public void sendEmailWithCc(String toEmail, String subject, String content, String[] cc) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            if (cc != null && cc.length > 0) {
                message.setCc(cc);
            }
            
            mailSender.send(message);
            log.info("邮件发送成功: {}", toEmail);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
        }
    }

    @Override
    public void sendBatchEmail(List<String> toEmails, String subject, String content) {
        for (String toEmail : toEmails) {
            sendSimpleEmail(toEmail, subject, content);
        }
    }

    @Override
    public void retryFailedEmails() {
        LambdaQueryWrapper<EmailRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailRecord::getStatus, 3)
               .lt(EmailRecord::getRetryCount, 3)
               .eq(EmailRecord::getDeleted, 0);
        
        List<EmailRecord> failedRecords = list(wrapper);
        
        for (EmailRecord record : failedRecords) {
            try {
                record.setRetryCount(record.getRetryCount() + 1);
                record.setStatus(1);
                updateById(record);
                
                sendSimpleEmail(record.getToEmail(), record.getSubject(), record.getContent());
            } catch (Exception e) {
                log.error("重试发送邮件失败: {}", e.getMessage());
                record.setFailReason(e.getMessage());
                updateById(record);
            }
        }
    }

    @Override
    public List<EmailRecord> getEmailRecords(Long notificationId) {
        LambdaQueryWrapper<EmailRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailRecord::getNotificationId, notificationId)
               .eq(EmailRecord::getDeleted, 0)
               .orderByDesc(EmailRecord::getCreateTime);
        return list(wrapper);
    }
}
