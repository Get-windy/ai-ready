package cn.aiedge.automation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_automation_log")
public class AutomationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private String modelName;

    private Long recordId;

    private String triggerType;

    private String actionType;

    private Boolean success;

    private String errorMessage;

    private String executionDetails;

    private LocalDateTime executionTime;

    private Long executionDuration;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}