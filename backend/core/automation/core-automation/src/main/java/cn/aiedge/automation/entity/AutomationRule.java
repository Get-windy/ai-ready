package cn.aiedge.automation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_automation_rule")
public class AutomationRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String modelName;

    private String triggerType;

    private String triggerCondition;

    private String actionType;

    private String actionConfig;

    private Boolean active;

    private Integer priority;

    private Boolean stopAfterExecute;

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