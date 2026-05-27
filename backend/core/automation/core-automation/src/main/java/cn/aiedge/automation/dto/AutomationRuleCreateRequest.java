package cn.aiedge.automation.dto;

import lombok.Data;

@Data
public class AutomationRuleCreateRequest {

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
}