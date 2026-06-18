package cn.aiedge.webhook.service.impl;

import cn.aiedge.webhook.dto.*;
import cn.aiedge.webhook.entity.Webhook;
import cn.aiedge.webhook.entity.WebhookLog;
import cn.aiedge.webhook.enums.HttpMethod;
import cn.aiedge.webhook.mapper.WebhookLogMapper;
import cn.aiedge.webhook.mapper.WebhookMapper;
import cn.aiedge.webhook.service.WebhookService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookMapper webhookMapper;
    private final WebhookLogMapper logMapper;

    @Override
    @Transactional
    public Webhook createWebhook(WebhookCreateRequest request) {
        Webhook webhook = new Webhook();
        webhook.setWebhookCode("WH" + IdUtil.fastSimpleUUID().substring(0, 8));
        webhook.setWebhookName(request.getWebhookName());
        webhook.setModelName(request.getModelName());
        webhook.setTriggerEvent(request.getTriggerEvent());
        webhook.setTargetUrl(request.getTargetUrl());
        webhook.setMethod(request.getMethod() != null ? request.getMethod() : "POST");
        webhook.setHeaders(request.getHeaders());
        webhook.setSecretKey(request.getSecretKey());
        webhook.setActive(request.getActive() != null ? request.getActive() : true);
        webhook.setRetryCount(request.getRetryCount() != null ? request.getRetryCount() : 0);
        webhook.setMaxRetry(request.getMaxRetry() != null ? request.getMaxRetry() : 3);
        webhook.setTimeout(request.getTimeout() != null ? request.getTimeout() : 30000);
        webhook.setDescription(request.getDescription());
        webhookMapper.insert(webhook);
        return webhook;
    }

    @Override
    @Transactional
    public Webhook updateWebhook(Long id, WebhookCreateRequest request) {
        Webhook webhook = webhookMapper.selectById(id);
        if (webhook == null) {
            throw new RuntimeException("Webhook不存在: " + id);
        }
        webhook.setWebhookName(request.getWebhookName());
        webhook.setModelName(request.getModelName());
        webhook.setTriggerEvent(request.getTriggerEvent());
        webhook.setTargetUrl(request.getTargetUrl());
        webhook.setMethod(request.getMethod());
        webhook.setHeaders(request.getHeaders());
        webhook.setSecretKey(request.getSecretKey());
        webhook.setActive(request.getActive());
        webhook.setRetryCount(request.getRetryCount());
        webhook.setMaxRetry(request.getMaxRetry());
        webhook.setTimeout(request.getTimeout());
        webhook.setDescription(request.getDescription());
        webhookMapper.updateById(webhook);
        return webhook;
    }

    @Override
    public Webhook getWebhookById(Long id) {
        return webhookMapper.selectById(id);
    }

    @Override
    public List<Webhook> getWebhooksByModel(String modelName) {
        return webhookMapper.selectByModel(modelName);
    }

    @Override
    public List<Webhook> getWebhooksByModelAndEvent(String modelName, String triggerEvent) {
        return webhookMapper.selectByModelAndEvent(modelName, triggerEvent);
    }

    @Override
    public Page<Webhook> listWebhooks(Integer page, Integer size, String modelName, String triggerEvent) {
        Page<Webhook> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Webhook> wrapper = new LambdaQueryWrapper<>();
        if (modelName != null && !modelName.isEmpty()) {
            wrapper.eq(Webhook::getModelName, modelName);
        }
        if (triggerEvent != null && !triggerEvent.isEmpty()) {
            wrapper.eq(Webhook::getTriggerEvent, triggerEvent);
        }
        wrapper.orderByDesc(Webhook::getCreateTime);
        return webhookMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void deleteWebhook(Long id) {
        webhookMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void activateWebhook(Long id) {
        Webhook webhook = webhookMapper.selectById(id);
        if (webhook != null) {
            webhook.setActive(true);
            webhookMapper.updateById(webhook);
        }
    }

    @Override
    @Transactional
    public void deactivateWebhook(Long id) {
        Webhook webhook = webhookMapper.selectById(id);
        if (webhook != null) {
            webhook.setActive(false);
            webhookMapper.updateById(webhook);
        }
    }

    @Override
    @Async
    public void triggerWebhook(String modelName, String triggerEvent, Long recordId, Map<String, Object> data) {
        triggerWebhook(modelName, triggerEvent, recordId, data, null);
    }

    @Override
    @Async
    public void triggerWebhook(String modelName, String triggerEvent, Long recordId, Map<String, Object> data, Map<String, Object> oldData) {
        List<Webhook> webhooks = webhookMapper.selectByModelAndEvent(modelName, triggerEvent);

        for (Webhook webhook : webhooks) {
            WebhookPayload payload = new WebhookPayload();
            payload.setWebhookCode(webhook.getWebhookCode());
            payload.setModelName(modelName);
            payload.setTriggerEvent(triggerEvent);
            payload.setRecordId(recordId);
            payload.setData(data);
            payload.setOldData(oldData);
            payload.setTimestamp(LocalDateTime.now());

            if (oldData != null && data != null) {
                Map<String, Object> changedFields = new HashMap<>();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    Object oldValue = oldData.get(entry.getKey());
                    if (!equals(oldValue, entry.getValue())) {
                        changedFields.put(entry.getKey(), entry.getValue());
                    }
                }
                payload.setChangedFields(changedFields);
            }

            if (webhook.getSecretKey() != null && !webhook.getSecretKey().isEmpty()) {
                payload.setSignature(generateSignature(webhook, payload));
            }

            executeWebhook(webhook, payload);
        }
    }

    @Override
    public WebhookLog executeWebhook(Webhook webhook, WebhookPayload payload) {
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setWebhookId(webhook.getId());
        webhookLog.setWebhookCode(webhook.getWebhookCode());
        webhookLog.setWebhookName(webhook.getWebhookName());
        webhookLog.setModelName(payload.getModelName());
        webhookLog.setTriggerEvent(payload.getTriggerEvent());
        webhookLog.setRecordId(payload.getRecordId());
        webhookLog.setRequestBody(JSONUtil.toJsonStr(payload));
        webhookLog.setTriggerTime(LocalDateTime.now());
        webhookLog.setRetryAttempt(webhook.getRetryCount());

        try {
            HttpRequest request = createHttpRequest(webhook, payload);
            HttpResponse response = request.timeout(webhook.getTimeout()).execute();

            webhookLog.setResponseStatus(String.valueOf(response.getStatus()));
            webhookLog.setResponseBody(response.body());
            webhookLog.setSuccess(response.isOk());
            webhookLog.setExecutionTime(LocalDateTime.now());
            webhookLog.setExecutionDuration(
                webhookLog.getExecutionTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() - 
                webhookLog.getTriggerTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            );

            if (!response.isOk()) {
                webhookLog.setErrorMessage("HTTP状态码: " + response.getStatus());
            }

            logMapper.insert(webhookLog);
            return webhookLog;

        } catch (Exception e) {
            webhookLog.setSuccess(false);
            webhookLog.setErrorMessage(e.getMessage());
            webhookLog.setExecutionTime(LocalDateTime.now());
            logMapper.insert(webhookLog);

            if (webhook.getRetryCount() < webhook.getMaxRetry()) {
                webhook.setRetryCount(webhook.getRetryCount() + 1);
                webhookMapper.updateById(webhook);
            }

            log.error("Webhook执行失败: {}", webhook.getWebhookName(), e);
            return webhookLog;
        }
    }

    @Override
    @Transactional
    public void retryFailedWebhooks() {
        List<WebhookLog> pendingRetries = logMapper.selectPendingRetries(10);

        for (WebhookLog webhookLog : pendingRetries) {
            Webhook webhook = webhookMapper.selectById(webhookLog.getWebhookId());
            if (webhook != null && webhook.getActive()) {
                WebhookPayload payload = JSONUtil.toBean(webhookLog.getRequestBody(), WebhookPayload.class);
                executeWebhook(webhook, payload);
            }
        }
    }

    @Override
    public List<WebhookLog> getWebhookLogs(Long webhookId, int limit) {
        return logMapper.selectByWebhook(webhookId, limit);
    }

    @Override
    public Page<WebhookLog> listLogs(Integer page, Integer size, Long webhookId, Boolean success) {
        Page<WebhookLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<WebhookLog> wrapper = new LambdaQueryWrapper<>();
        if (webhookId != null) {
            wrapper.eq(WebhookLog::getWebhookId, webhookId);
        }
        if (success != null) {
            wrapper.eq(WebhookLog::getSuccess, success);
        }
        wrapper.orderByDesc(WebhookLog::getTriggerTime);
        return logMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public String generateSignature(Webhook webhook, WebhookPayload payload) {
        String payloadJson = JSONUtil.toJsonStr(payload);
        return SecureUtil.hmacSha256(webhook.getSecretKey()).digestHex(payloadJson);
    }

    @Override
    public boolean verifySignature(String secretKey, String payload, String signature) {
        String expectedSignature = SecureUtil.hmacSha256(secretKey).digestHex(payload);
        return expectedSignature.equals(signature);
    }

    private HttpRequest createHttpRequest(Webhook webhook, WebhookPayload payload) {
        String url = webhook.getTargetUrl();
        String method = webhook.getMethod();
        String body = JSONUtil.toJsonStr(payload);

        HttpRequest request;
        if (HttpMethod.GET.getName().equals(method)) {
            request = HttpRequest.get(url);
        } else if (HttpMethod.POST.getName().equals(method)) {
            request = HttpRequest.post(url).body(body);
        } else if (HttpMethod.PUT.getName().equals(method)) {
            request = HttpRequest.put(url).body(body);
        } else if (HttpMethod.DELETE.getName().equals(method)) {
            request = HttpRequest.delete(url).body(body);
        } else {
            request = HttpRequest.post(url).body(body);
        }

        if (webhook.getHeaders() != null && !webhook.getHeaders().isEmpty()) {
            JSONObject headers = JSONUtil.parseObj(webhook.getHeaders());
            for (String key : headers.keySet()) {
                request.header(key, headers.getStr(key));
            }
        }

        request.header("Content-Type", "application/json");
        request.header("X-Webhook-Code", webhook.getWebhookCode());
        request.header("X-Webhook-Event", webhook.getTriggerEvent());
        request.header("X-Webhook-Signature", payload.getSignature());

        return request;
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}