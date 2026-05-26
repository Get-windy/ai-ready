package com.aiready.notification.service;

import com.aiready.notification.entity.EmailRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 邮件服务接口
 */
public interface EmailService extends IService<EmailRecord> {

    /**
     * 发送简单邮件
     */
    void sendSimpleEmail(String toEmail, String subject, String content);

    /**
     * 发送HTML邮件
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlContent);

    /**
     * 发送带抄送的邮件
     */
    void sendEmailWithCc(String toEmail, String subject, String content, String[] cc);

    /**
     * 批量发送邮件
     */
    void sendBatchEmail(List<String> toEmails, String subject, String content);

    /**
     * 重试发送失败的邮件
     */
    void retryFailedEmails();

    /**
     * 获取邮件发送记录
     */
    List<EmailRecord> getEmailRecords(Long notificationId);
}
