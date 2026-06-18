package cn.aiedge.webhook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_webhook")
public class Webhook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String webhookCode;

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

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}