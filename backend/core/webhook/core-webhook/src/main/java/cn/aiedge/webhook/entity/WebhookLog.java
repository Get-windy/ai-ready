package cn.aiedge.webhook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_webhook_log")
public class WebhookLog {

    @TableId(type = IdType.AUTO)
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

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}