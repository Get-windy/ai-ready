package cn.aiedge.permission.service.impl;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.RecordRule;
import cn.aiedge.permission.enums.PermissionType;
import cn.aiedge.permission.mapper.RecordRuleMapper;
import cn.aiedge.permission.service.RecordRuleService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordRuleServiceImpl implements RecordRuleService {

    private final RecordRuleMapper ruleMapper;

    @Override
    @Transactional
    public RecordRule createRule(RecordRuleCreateRequest request) {
        RecordRule rule = new RecordRule();
        rule.setRuleCode("RR" + IdUtil.fastSimpleUUID().substring(0, 8));
        rule.setRuleName(request.getRuleName());
        rule.setModelName(request.getModelName());
        rule.setDomain(request.getDomain());
        rule.setActive(request.getActive() != null ? request.getActive() : true);
        rule.setGlobal(request.getGlobal() != null ? request.getGlobal() : false);
        rule.setGroupId(request.getGroupId());
        rule.setUserId(request.getUserId());
        rule.setPermRead(request.getPermRead() != null && request.getPermRead() ? 1 : 0);
        rule.setPermWrite(request.getPermWrite() != null && request.getPermWrite() ? 1 : 0);
        rule.setPermCreate(request.getPermCreate() != null && request.getPermCreate() ? 1 : 0);
        rule.setPermDelete(request.getPermDelete() != null && request.getPermDelete() ? 1 : 0);
        rule.setDescription(request.getDescription());
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public RecordRule updateRule(Long id, RecordRuleCreateRequest request) {
        RecordRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + id);
        }
        rule.setRuleName(request.getRuleName());
        rule.setModelName(request.getModelName());
        rule.setDomain(request.getDomain());
        rule.setActive(request.getActive());
        rule.setGlobal(request.getGlobal());
        rule.setGroupId(request.getGroupId());
        rule.setUserId(request.getUserId());
        rule.setPermRead(request.getPermRead() != null && request.getPermRead() ? 1 : 0);
        rule.setPermWrite(request.getPermWrite() != null && request.getPermWrite() ? 1 : 0);
        rule.setPermCreate(request.getPermCreate() != null && request.getPermCreate() ? 1 : 0);
        rule.setPermDelete(request.getPermDelete() != null && request.getPermDelete() ? 1 : 0);
        rule.setDescription(request.getDescription());
        ruleMapper.updateById(rule);
        return rule;
    }

    @Override
    public RecordRule getRuleById(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    public List<RecordRule> getRulesByModel(String modelName) {
        return ruleMapper.selectByModel(modelName);
    }

    @Override
    public List<RecordRule> getRulesByUser(Long userId) {
        return ruleMapper.selectByUser(userId);
    }

    @Override
    public List<RecordRule> getRulesByGroup(Long groupId) {
        return ruleMapper.selectByModelAndGroup(null, groupId);
    }

    @Override
    public Page<RecordRule> listRules(Integer page, Integer size, String modelName, Long userId, Long groupId) {
        Page<RecordRule> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<RecordRule> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(modelName)) {
            wrapper.eq(RecordRule::getModelName, modelName);
        }
        if (userId != null) {
            wrapper.eq(RecordRule::getUserId, userId);
        }
        if (groupId != null) {
            wrapper.eq(RecordRule::getGroupId, groupId);
        }
        wrapper.orderByDesc(RecordRule::getCreateTime);
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
        RecordRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            rule.setActive(true);
            ruleMapper.updateById(rule);
        }
    }

    @Override
    @Transactional
    public void deactivateRule(Long id) {
        RecordRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            rule.setActive(false);
            ruleMapper.updateById(rule);
        }
    }

    @Override
    public String buildDomainFilter(String modelName, PermissionContext context) {
        List<RecordRule> allRules = new ArrayList<>();

        List<RecordRule> globalRules = ruleMapper.selectGlobalRules(modelName);
        allRules.addAll(globalRules);

        if (context.getUserId() != null) {
            List<RecordRule> userRules = ruleMapper.selectByModelAndUser(modelName, context.getUserId());
            allRules.addAll(userRules);
        }

        if (context.getGroupIds() != null && !context.getGroupIds().isEmpty()) {
            String groupIds = context.getGroupIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            List<RecordRule> groupRules = ruleMapper.selectByGroups(groupIds);
            allRules.addAll(groupRules.stream()
                    .filter(r -> r.getModelName().equals(modelName))
                    .collect(Collectors.toList()));
        }

        if (allRules.isEmpty()) {
            return null;
        }

        List<String> domains = allRules.stream()
                .filter(r -> hasPermission(r, context.getOperation()))
                .map(RecordRule::getDomain)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

        if (domains.isEmpty()) {
            return null;
        }

        if (domains.size() == 1) {
            return domains.get(0);
        }

        return "[\"|\", " + domains.stream().collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public boolean checkRecordAccess(String modelName, Long recordId, String operation, PermissionContext context) {
        List<RecordRule> rules = getApplicableRules(modelName, context);

        for (RecordRule rule : rules) {
            if (!hasPermission(rule, operation)) {
                continue;
            }

            if (rule.getDomain() == null || rule.getDomain().isEmpty()) {
                return true;
            }

            if (evaluateDomain(rule.getDomain(), context)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Long> filterRecords(String modelName, List<Long> recordIds, String operation, PermissionContext context) {
        return recordIds.stream()
                .filter(id -> checkRecordAccess(modelName, id, operation, context))
                .collect(Collectors.toList());
    }

    @Override
    public boolean evaluateDomain(String domain, PermissionContext context) {
        if (domain == null || domain.isEmpty()) {
            return true;
        }

        try {
            JSONArray domainArray = JSONUtil.parseArray(domain);
            return evaluateDomainArray(domainArray, context);
        } catch (Exception e) {
            log.warn("Domain评估失败: {}", domain, e);
            return false;
        }
    }

    private List<RecordRule> getApplicableRules(String modelName, PermissionContext context) {
        List<RecordRule> rules = new ArrayList<>();

        List<RecordRule> globalRules = ruleMapper.selectGlobalRules(modelName);
        rules.addAll(globalRules);

        if (context.getUserId() != null) {
            List<RecordRule> userRules = ruleMapper.selectByModelAndUser(modelName, context.getUserId());
            rules.addAll(userRules);
        }

        if (context.getGroupIds() != null && !context.getGroupIds().isEmpty()) {
            for (Long groupId : context.getGroupIds()) {
                List<RecordRule> groupRules = ruleMapper.selectByModelAndGroup(modelName, groupId);
                rules.addAll(groupRules);
            }
        }

        return rules;
    }

    private boolean hasPermission(RecordRule rule, String operation) {
        switch (operation.toLowerCase()) {
            case "read":
                return rule.getPermRead() == 1;
            case "write":
                return rule.getPermWrite() == 1;
            case "create":
                return rule.getPermCreate() == 1;
            case "delete":
                return rule.getPermDelete() == 1;
            default:
                return false;
        }
    }

    private boolean evaluateDomainArray(JSONArray domain, PermissionContext context) {
        if (domain.isEmpty()) {
            return true;
        }

        Object first = domain.get(0);
        if (first instanceof String) {
            String operator = (String) first;
            if ("|".equals(operator)) {
                return evaluateDomainArray((JSONArray) domain.get(1), context) ||
                       evaluateDomainArray((JSONArray) domain.get(2), context);
            } else if ("&".equals(operator)) {
                return evaluateDomainArray((JSONArray) domain.get(1), context) &&
                       evaluateDomainArray((JSONArray) domain.get(2), context);
            } else if ("!".equals(operator)) {
                return !evaluateDomainArray((JSONArray) domain.get(1), context);
            }
        }

        if (domain.size() >= 3) {
            String field = domain.getStr(0);
            String operator = domain.getStr(1);
            Object value = domain.get(2);

            Object fieldValue = context.getRecordData().get(field);
            return evaluateCondition(fieldValue, operator, value);
        }

        return true;
    }

    private boolean evaluateCondition(Object fieldValue, String operator, Object expectedValue) {
        if (fieldValue == null) {
            return "is null".equals(operator) || "=".equals(operator) && expectedValue == null;
        }

        switch (operator) {
            case "=":
                return fieldValue.equals(expectedValue);
            case "!=":
                return !fieldValue.equals(expectedValue);
            case ">":
                return compareNumbers(fieldValue, expectedValue) > 0;
            case "<":
                return compareNumbers(fieldValue, expectedValue) < 0;
            case ">=":
                return compareNumbers(fieldValue, expectedValue) >= 0;
            case "<=":
                return compareNumbers(fieldValue, expectedValue) <= 0;
            case "like":
                return fieldValue.toString().contains(expectedValue.toString());
            case "ilike":
                return fieldValue.toString().toLowerCase().contains(expectedValue.toString().toLowerCase());
            case "in":
                return JSONUtil.parseArray(expectedValue).contains(fieldValue);
            case "not in":
                return !JSONUtil.parseArray(expectedValue).contains(fieldValue);
            case "is not null":
                return fieldValue != null;
            default:
                return false;
        }
    }

    private int compareNumbers(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            double aVal = ((Number) a).doubleValue();
            double bVal = ((Number) b).doubleValue();
            return Double.compare(aVal, bVal);
        }
        return 0;
    }
}