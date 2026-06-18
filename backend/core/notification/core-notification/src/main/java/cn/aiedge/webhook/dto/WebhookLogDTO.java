package cn.aiedge.webhook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WebhookLogDTO {

    private Long id;

    private Long webhookId;

    private String webhookCode;

    private String webhookName;

    private String modelName;

    private String triggerEvent;

    private Long recordId;

    private String requestBody;

    private String responseStatus;

    private String responseBody;

    private Boolean success;

    private String errorMessage;

    private Integer retryAttempt;

    private LocalDateTime triggerTime;

    private LocalDateTime executionTime;

    private Long executionDuration;
}