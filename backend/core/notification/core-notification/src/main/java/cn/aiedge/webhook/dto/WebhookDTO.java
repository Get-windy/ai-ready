package cn.aiedge.webhook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WebhookDTO {

    private Long id;

    private String webhookCode;

    private String webhookName;

    private String modelName;

    private String triggerEvent;

    private String targetUrl;

    private String method;

    private String headers;

    private Boolean active;

    private Integer retryCount;

    private Integer maxRetry;

    private Integer timeout;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}