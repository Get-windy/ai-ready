package cn.aiedge.monitor.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_alert_history")
public class AlertHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private String ruleName;

    private String metricName;

    private Double metricValue;

    private Double threshold;

    private String operator;

    private String severity;

    private String message;

    private Boolean acknowledged;

    private String acknowledgedBy;

    private LocalDateTime acknowledgedAt;

    private Boolean resolved;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private String resolution;

    private Long tenantId;

    private LocalDateTime alertTime;
}
