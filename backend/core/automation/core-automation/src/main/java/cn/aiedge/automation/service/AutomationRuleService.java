package cn.aiedge.automation.service;

import cn.aiedge.automation.dto.*;
import cn.aiedge.automation.entity.AutomationRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface AutomationRuleService {

    AutomationRule createRule(AutomationRuleCreateRequest request);

    AutomationRule updateRule(Long id, AutomationRuleCreateRequest request);

    AutomationRule getRuleById(Long id);

    List<AutomationRule> getRulesByModel(String modelName);

    List<AutomationRule> getRulesByModelAndTrigger(String modelName, String triggerType);

    Page<AutomationRule> listRules(Integer page, Integer size, String modelName, String triggerType);

    void deleteRule(Long id);

    void activateRule(Long id);

    void deactivateRule(Long id);

    void executeOnCreate(String modelName, Long recordId, Map<String, Object> values);

    void executeOnWrite(String modelName, Long recordId, Map<String, Object> oldValues, Map<String, Object> newValues);

    void executeOnDelete(String modelName, Long recordId);

    void executeOnChange(String modelName, Long recordId, String fieldName, Object oldValue, Object newValue);

    void executeScheduledRules();

    boolean evaluateCondition(String condition, AutomationExecutionContext context);

    void executeAction(AutomationRule rule, AutomationExecutionContext context);
}