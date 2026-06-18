package cn.aiedge.automation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AutomationRuleDTO {

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}