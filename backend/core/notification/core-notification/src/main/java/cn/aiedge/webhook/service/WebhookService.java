package cn.aiedge.webhook.service;

import cn.aiedge.webhook.dto.*;
import cn.aiedge.webhook.entity.Webhook;
import cn.aiedge.webhook.entity.WebhookLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface WebhookService {

    Webhook createWebhook(WebhookCreateRequest request);

    Webhook updateWebhook(Long id, WebhookCreateRequest request);

    Webhook getWebhookById(Long id);

    List<Webhook> getWebhooksByModel(String modelName);

    List<Webhook> getWebhooksByModelAndEvent(String modelName, String triggerEvent);

    Page<Webhook> listWebhooks(Integer page, Integer size, String modelName, String triggerEvent);

    void deleteWebhook(Long id);

    void activateWebhook(Long id);

    void deactivateWebhook(Long id);

    void triggerWebhook(String modelName, String triggerEvent, Long recordId, Map<String, Object> data);

    void triggerWebhook(String modelName, String triggerEvent, Long recordId, Map<String, Object> data, Map<String, Object> oldData);

    WebhookLog executeWebhook(Webhook webhook, WebhookPayload payload);

    void retryFailedWebhooks();

    List<WebhookLog> getWebhookLogs(Long webhookId, int limit);

    Page<WebhookLog> listLogs(Integer page, Integer size, Long webhookId, Boolean success);

    String generateSignature(Webhook webhook, WebhookPayload payload);

    boolean verifySignature(String secretKey, String payload, String signature);
}