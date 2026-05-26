package cn.aiedge.automation.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AutomationExecutionContext {

    private String modelName;

    private Long recordId;

    private String triggerType;

    private Map<String, Object> oldValues;

    private Map<String, Object> newValues;

    private Map<String, Object> changedFields;

    private Long userId;

    private String userName;
}