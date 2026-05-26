package cn.aiedge.automation.service.impl;

import cn.aiedge.automation.dto.*;
import cn.aiedge.automation.entity.AutomationLog;
import cn.aiedge.automation.entity.AutomationRule;
import cn.aiedge.automation.enums.ActionType;
import cn.aiedge.automation.enums.TriggerType;
import cn.aiedge.automation.mapper.AutomationLogMapper;
import cn.aiedge.automation.mapper.AutomationRuleMapper;
import cn.aiedge.automation.service.AutomationRuleService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationRuleServiceImpl implements AutomationRuleService {

    private final AutomationRuleMapper ruleMapper;
    private final AutomationLogMapper logMapper;

    @Override
    @Transactional
    public AutomationRule createRule(AutomationRuleCreateRequest request) {
        AutomationRule rule = new AutomationRule();
        rule.setRuleCode("AR" + IdUtil.fastSimpleUUID().substring(0, 8));
        rule.setRuleName(request.getRuleName());
        rule.setModelName(request.getModelName());
        rule.setTriggerType(request.getTriggerType());
        rule.setTriggerCondition(request.getTriggerCondition());
        rule.setActionType(request.getActionType());
        rule.setActionConfig(request.getActionConfig());
        rule.setActive(request.getActive() != null ? request.getActive() : true);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setStopAfterExecute(request.getStopAfterExecute() != null ? request.getStopAfterExecute() : false);
        rule.setDescription(request.getDescription());
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public AutomationRule updateRule(Long id, AutomationRuleCreateRequest request) {
        AutomationRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + id);
        }
        rule.setRuleName(request.getRuleName());
        rule.setModelName(request.getModelName());
        rule.setTriggerType(request.getTriggerType());
        rule.setTriggerCondition(request.getTriggerCondition());
        rule.setActionType(request.getActionType());
        rule.setActionConfig(request.getActionConfig());
        rule.setActive(request.getActive());
        rule.setPriority(request.getPriority());
        rule.setStopAfterExecute(request.getStopAfterExecute());
        rule.setDescription(request.getDescription());
        ruleMapper.updateById(rule);
        return rule;
    }

    @Override
    public AutomationRule getRuleById(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    public List<AutomationRule> getRulesByModel(String modelName) {
        return ruleMapper.selectByModel(modelName);
    }

    @Override
    public List<AutomationRule> getRulesByModelAndTrigger(String modelName, String triggerType) {
        return ruleMapper.selectByModelAndTrigger(modelName, triggerType);
    }

    @Override
    public Page<AutomationRule> listRules(Integer page, Integer size, String modelName, String triggerType) {
        Page<AutomationRule> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<AutomationRule> wrapper = new LambdaQueryWrapper<>();
        if (modelName != null && !modelName.isEmpty()) {
            wrapper.eq(AutomationRule::getModelName, modelName);
        }
        if (triggerType != null && !triggerType.isEmpty()) {
            wrapper.eq(AutomationRule::getTriggerType, triggerType);
        }
        wrapper.orderByDesc(AutomationRule::getPriority);
        return ruleMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void activateRule(Long id) {
        AutomationRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            rule.setActive(true);
            ruleMapper.updateById(rule);
        }
    }

    @Override
    @Transactional
    public void deactivateRule(Long id) {
        AutomationRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            rule.setActive(false);
            ruleMapper.updateById(rule);
        }
    }

    @Override
    @Transactional
    public void executeOnCreate(String modelName, Long recordId, Map<String, Object> values) {
        List<AutomationRule> rules = ruleMapper.selectByModelAndTrigger(modelName, TriggerType.ON_CREATE.getCode());
        AutomationExecutionContext context = new AutomationExecutionContext();
        context.setModelName(modelName);
        context.setRecordId(recordId);
        context.setTriggerType(TriggerType.ON_CREATE.getCode());
        context.setNewValues(values);
        context.setChangedFields(values);

        executeRules(rules, context);
    }

    @Override
    @Transactional
    public void executeOnWrite(String modelName, Long recordId, Map<String, Object> oldValues, Map<String, Object> newValues) {
        List<AutomationRule> rules = ruleMapper.selectByModelAndTrigger(modelName, TriggerType.ON_WRITE.getCode());
        
        Map<String, Object> changedFields = new HashMap<>();
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            Object oldValue = oldValues.get(entry.getKey());
            if (!equals(oldValue, entry.getValue())) {
                changedFields.put(entry.getKey(), entry.getValue());
            }
        }

        if (changedFields.isEmpty()) {
            return;
        }

        AutomationExecutionContext context = new AutomationExecutionContext();
        context.setModelName(modelName);
        context.setRecordId(recordId);
        context.setTriggerType(TriggerType.ON_WRITE.getCode());
        context.setOldValues(oldValues);
        context.setNewValues(newValues);
        context.setChangedFields(changedFields);

        executeRules(rules, context);

        for (Map.Entry<String, Object> entry : changedFields.entrySet()) {
            executeOnChange(modelName, recordId, entry.getKey(), oldValues.get(entry.getKey()), entry.getValue());
        }
    }

    @Override
    @Transactional
    public void executeOnDelete(String modelName, Long recordId) {
        List<AutomationRule> rules = ruleMapper.selectByModelAndTrigger(modelName, TriggerType.ON_DELETE.getCode());
        AutomationExecutionContext context = new AutomationExecutionContext();
        context.setModelName(modelName);
        context.setRecordId(recordId);
        context.setTriggerType(TriggerType.ON_DELETE.getCode());

        executeRules(rules, context);
    }

    @Override
    @Transactional
    public void executeOnChange(String modelName, Long recordId, String fieldName, Object oldValue, Object newValue) {
        List<AutomationRule> rules = ruleMapper.selectByModelAndTrigger(modelName, TriggerType.ON_CHANGE.getCode());
        
        for (AutomationRule rule : rules) {
            JSONObject condition = JSONUtil.parseObj(rule.getTriggerCondition());
            String triggerField = condition.getStr("field");
            if (triggerField != null && triggerField.equals(fieldName)) {
                AutomationExecutionContext context = new AutomationExecutionContext();
                context.setModelName(modelName);
                context.setRecordId(recordId);
                context.setTriggerType(TriggerType.ON_CHANGE.getCode());
                Map<String, Object> oldValues = new HashMap<>();
                oldValues.put(fieldName, oldValue);
                Map<String, Object> newValues = new HashMap<>();
                newValues.put(fieldName, newValue);
                context.setOldValues(oldValues);
                context.setNewValues(newValues);

                if (evaluateCondition(rule.getTriggerCondition(), context)) {
                    executeAction(rule, context);
                    logExecution(rule, context, true, null);
                    if (rule.getStopAfterExecute()) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void executeScheduledRules() {
        List<AutomationRule> rules = ruleMapper.selectScheduledRules();
        for (AutomationRule rule : rules) {
            try {
                AutomationExecutionContext context = new AutomationExecutionContext();
                context.setTriggerType(TriggerType.ON_TIME.getCode());
                executeAction(rule, context);
                logExecution(rule, context, true, null);
            } catch (Exception e) {
                log.error("执行定时规则失败: {}", rule.getRuleName(), e);
                AutomationExecutionContext context = new AutomationExecutionContext();
                logExecution(rule, context, false, e.getMessage());
            }
        }
    }

    @Override
    public boolean evaluateCondition(String condition, AutomationExecutionContext context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            JSONObject conditionJson = JSONUtil.parseObj(condition);
            String type = conditionJson.getStr("type");

            if ("field_change".equals(type)) {
                String field = conditionJson.getStr("field");
                Object oldValue = context.getOldValues().get(field);
                Object newValue = context.getNewValues().get(field);
                return !equals(oldValue, newValue);
            }

            if ("field_value".equals(type)) {
                String field = conditionJson.getStr("field");
                String operator = conditionJson.getStr("operator");
                Object expectedValue = conditionJson.get("value");
                Object actualValue = context.getNewValues().get(field);
                return evaluateOperator(actualValue, operator, expectedValue);
            }

            if ("state_change".equals(type)) {
                String fromState = conditionJson.getStr("from_state");
                String toState = conditionJson.getStr("to_state");
                Object oldState = context.getOldValues().get("state");
                Object newState = context.getNewValues().get("state");
                return (fromState == null || fromState.equals(oldState)) && toState.equals(newState);
            }

            return true;
        } catch (Exception e) {
            log.warn("条件评估失败: {}", condition, e);
            return false;
        }
    }

    @Override
    public void executeAction(AutomationRule rule, AutomationExecutionContext context) {
        JSONObject actionConfig = JSONUtil.parseObj(rule.getActionConfig());
        String actionType = rule.getActionType();

        switch (actionType) {
            case "WRITE":
                executeWriteAction(actionConfig, context);
                break;
            case "SEND_MESSAGE":
                executeSendMessageAction(actionConfig, context);
                break;
            case "SEND_NOTIFICATION":
                executeSendNotificationAction(actionConfig, context);
                break;
            case "SET_STATE":
                executeSetStateAction(actionConfig, context);
                break;
            case "CREATE_ACTIVITY":
                executeCreateActivityAction(actionConfig, context);
                break;
            default:
                log.warn("未知的动作类型: {}", actionType);
        }
    }

    private void executeRules(List<AutomationRule> rules, AutomationExecutionContext context) {
        for (AutomationRule rule : rules) {
            try {
                if (evaluateCondition(rule.getTriggerCondition(), context)) {
                    executeAction(rule, context);
                    logExecution(rule, context, true, null);
                    if (rule.getStopAfterExecute()) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("执行规则失败: {}", rule.getRuleName(), e);
                logExecution(rule, context, false, e.getMessage());
            }
        }
    }

    private void executeWriteAction(JSONObject config, AutomationExecutionContext context) {
        log.info("执行WRITE动作: model={}, record={}, config={}", 
                context.getModelName(), context.getRecordId(), config);
    }

    private void executeSendMessageAction(JSONObject config, AutomationExecutionContext context) {
        log.info("执行SEND_MESSAGE动作: model={}, record={}, config={}", 
                context.getModelName(), context.getRecordId(), config);
    }

    private void executeSendNotificationAction(JSONObject config, AutomationExecutionContext context) {
        log.info("执行SEND_NOTIFICATION动作: model={}, record={}, config={}", 
                context.getModelName(), context.getRecordId(), config);
    }

    private void executeSetStateAction(JSONObject config, AutomationExecutionContext context) {
        log.info("执行SET_STATE动作: model={}, record={}, config={}", 
                context.getModelName(), context.getRecordId(), config);
    }

    private void executeCreateActivityAction(JSONObject config, AutomationExecutionContext context) {
        log.info("执行CREATE_ACTIVITY动作: model={}, record={}, config={}", 
                context.getModelName(), context.getRecordId(), config);
    }

    private void logExecution(AutomationRule rule, AutomationExecutionContext context, boolean success, String errorMessage) {
        AutomationLog log = new AutomationLog();
        log.setRuleId(rule.getId());
        log.setRuleCode(rule.getRuleCode());
        log.setRuleName(rule.getRuleName());
        log.setModelName(context.getModelName());
        log.setRecordId(context.getRecordId());
        log.setTriggerType(context.getTriggerType());
        log.setActionType(rule.getActionType());
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setExecutionTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private boolean evaluateOperator(Object actual, String operator, Object expected) {
        if (actual == null) return false;
        
        switch (operator) {
            case "=":
                return actual.equals(expected);
            case "!=":
                return !actual.equals(expected);
            case ">":
                return compareNumbers(actual, expected) > 0;
            case "<":
                return compareNumbers(actual, expected) < 0;
            case ">=":
                return compareNumbers(actual, expected) >= 0;
            case "<=":
                return compareNumbers(actual, expected) <= 0;
            case "contains":
                return actual.toString().contains(expected.toString());
            case "in":
                return JSONUtil.parseArray(expected).contains(actual);
            default:
                return false;
        }
    }

    private int compareNumbers(Object a, Object b) {
        double aVal = ((Number) a).doubleValue();
        double bVal = ((Number) b).doubleValue();
        return Double.compare(aVal, bVal);
    }
}