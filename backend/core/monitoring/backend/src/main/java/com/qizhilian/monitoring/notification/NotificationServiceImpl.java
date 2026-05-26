package com.qizhilian.monitoring.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qizhilian.monitoring.entity.AlertHistoryEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${notification.dingtalk.webhook:}")
    private String dingtalkWebhook;

    @Value("${notification.wecom.webhook:}")
    private String wecomWebhook;

    @Override
    public void sendNotification(AlertHistoryEntity alert, String channels, String receivers) {
        try {
            List<String> channelList = parseJsonList(channels);
            for (String channel : channelList) {
                switch (channel.toLowerCase()) {
                    case "email" -> sendEmail(alert, receivers);
                    case "dingtalk" -> sendDingTalk(alert, receivers);
                    case "wecom", "wechat", "enterprise_wechat" -> sendWeCom(alert, receivers);
                    default -> log.warn("Unknown notification channel: {}", channel);
                }
            }
            alert.setNotificationStatus("sent");
        } catch (Exception e) {
            log.error("Notification failed: {}", e.getMessage());
            alert.setNotificationStatus("failed");
        }
    }

    @Override
    public void sendEmail(AlertHistoryEntity alert, String receivers) {
        try {
            List<String> emails = parseJsonList(receivers);
            if (emails.isEmpty() || mailFrom.isEmpty()) {
                log.warn("Email not configured or no receivers");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(emails.toArray(new String[0]));
            message.setSubject(alert.getAlertTitle());
            message.setText(alert.getAlertContent());
            mailSender.send(message);
            log.info("Email sent to {}", emails);
        } catch (Exception e) {
            log.error("Email send failed: {}", e.getMessage());
        }
    }

    @Override
    public void sendDingTalk(AlertHistoryEntity alert, String receivers) {
        try {
            if (dingtalkWebhook == null || dingtalkWebhook.isEmpty()) {
                log.warn("DingTalk webhook not configured");
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, String> markdown = new HashMap<>();
            markdown.put("title", alert.getAlertTitle());
            markdown.put("text", "## " + alert.getAlertTitle() + "\n\n" + alert.getAlertContent());
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(dingtalkWebhook, request, String.class);
            log.info("DingTalk notification sent");
        } catch (Exception e) {
            log.error("DingTalk send failed: {}", e.getMessage());
        }
    }

    @Override
    public void sendWeCom(AlertHistoryEntity alert, String receivers) {
        try {
            if (wecomWebhook == null || wecomWebhook.isEmpty()) {
                log.warn("WeCom webhook not configured");
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, String> markdown = new HashMap<>();
            markdown.put("content", "## " + alert.getAlertTitle() + "\n\n" + alert.getAlertContent());
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(wecomWebhook, request, String.class);
            log.info("WeCom notification sent");
        } catch (Exception e) {
            log.error("WeCom send failed: {}", e.getMessage());
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", json);
            return List.of();
        }
    }
}
