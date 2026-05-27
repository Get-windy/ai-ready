package cn.aiedge.webhook.dto;

import lombok.Data;

@Data
public class WebhookCreateRequest {

    private String webhookName;

    private String modelName;

    private String triggerEvent;

    private String targetUrl;

    private String method;

    private String headers;

    private String secretKey;

    private Boolean active;

    private Integer retryCount;

    private Integer maxRetry;

    private Integer timeout;

    private String description;
}