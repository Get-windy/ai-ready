package cn.aiedge.monitor.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警规则
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_alert_rule")
public class AlertRule {

    /**
     * 规则ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 监控指标
     */
    private String metricName;

    /**
     * 操作符：>, <, >=, <=, ==, !=
     */
    private String operator;

    /**
     * 阈值
     */
    private Double threshold;

    /**
     * 持续时间（秒）
     */
    private Integer duration;

    /**
     * 告警级别：info, warning, critical
     */
    private String severity;

    /**
     * 通知方式：email, sms, webhook
     */
    private String notifyType;

    /**
     * 通知接收人
     */
    private String notifyTargets;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 检查是否触发告警
     *
     * @param value 指标值
     * @return 是否触发
     */
    public boolean isTriggered(double value) {
        return switch (operator) {
            case ">" -> value > threshold;
            case "<" -> value < threshold;
            case ">=" -> value >= threshold;
            case "<=" -> value <= threshold;
            case "==" -> value == threshold;
            case "!=" -> value != threshold;
            default -> false;
        };
    }
}
