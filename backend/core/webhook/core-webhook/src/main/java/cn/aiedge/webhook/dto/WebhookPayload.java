package cn.aiedge.webhook.dto;

import lombok.Data;

import java.util.Map;

@Data
public class WebhookPayload {

    private String webhookCode;

    private String modelName;

    private String triggerEvent;

    private Long recordId;

    private Map<String, Object> data;

    private Map<String, Object> oldData;

    private Map<String, Object> changedFields;

    private LocalDateTime timestamp;

    private String signature;
}