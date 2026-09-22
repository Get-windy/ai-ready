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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordRuleServiceImpl implements RecordRuleService {

    /** 「启用中规则的模型名」缓存 TTL：与数据权限表缓存同量级，改配置后最多滞后这么久 */
    private static final long ENABLED_MODELS_CACHE_TTL_MS = 30_000L;

    private final RecordRuleMapper ruleMapper;

    /** 启用中规则的模型名快照（小写）。volatile：被拦截器多线程读，写后不再改内容 */
    private volatile Set<String> enabledModelsCache = Set.of();
    private volatile long enabledModelsExpireAt = 0L;
    /** 快照归属的租户：sys_record_rule 按租户隔离，缓存必须按租户区分，否则 A 租户先加载会让 B 租户的规则在 TTL 内不生效（漏过滤） */
    private volatile Long enabledModelsTenantId = null;

    /**
     * 加载启用模型集合的防重入闸。
     *
     * <p>本方法要查 {@code sys_record_rule}，而那条查询自己也会经过记录规则拦截器 —— 若不加锁，
     * 拦截器 → 本方法 → 拦截器 → 本方法 会无限递归（数据权限侧在 {@code sys_data_scope} 上
     * 实踩过 StackOverflowError）。加载中直接返回上一次快照。</p>
     */
    private final AtomicBoolean loadingEnabledModels = new AtomicBoolean(false);

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
        invalidateEnabledModelsCache();
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
        invalidateEnabledModelsCache();
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
        invalidateEnabledModelsCache();
    }

    @Override
    @Transactional
    public void activateRule(Long id) {
        RecordRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            rule.setActive(true);
            ruleMapper.updateById(rule);
            invalidateEnabledModelsCache();
        }
    }

    @Override
    @Transactional
    public void deactivateRule(Long id) {
        RecordRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            rule.setActive(false);
            ruleMapper.updateById(rule);
            invalidateEnabledModelsCache();
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
    public String buildReadDomainFilter(String modelName, Long userId, List<Long> groupIds) {
        if (StrUtil.isBlank(modelName)) {
            return null;
        }
        PermissionContext context = new PermissionContext();
        context.setModelName(modelName);
        context.setUserId(userId);
        if (groupIds != null) {
            // 复制一份：调用方（拦截器）传的是不可变集合，且本对象只读，避免共享可变引用
            context.setGroupIds(new ArrayList<>(groupIds));
        }
        context.setOperation("read");
        return buildDomainFilter(modelName, context);
    }

    @Override
    public Set<String> getEnabledRecordRuleModels() {
        long now = System.currentTimeMillis();
        // 只读 Sa-Token 会话取租户，不查库（查库会经拦截器递归）；缓存按租户区分
        Long tenantId = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        if (now < enabledModelsExpireAt && java.util.Objects.equals(tenantId, enabledModelsTenantId)) {
            return enabledModelsCache;
        }
        if (!loadingEnabledModels.compareAndSet(false, true)) {
            // 正在加载（多半是拦截器自己的查询触发的）→ 返回上一次快照，绝不递归
            return enabledModelsCache;
        }
        try {
            List<String> models = ruleMapper.selectActiveModelNames();
            enabledModelsCache = models == null ? Set.of() : models.stream()
                    .filter(StrUtil::isNotBlank)
                    .map(m -> m.trim().toLowerCase())
                    .collect(Collectors.toSet());
            enabledModelsTenantId = tenantId;
        } catch (Exception e) {
            // 加载失败**不清空**上一次快照：安全策略宁可沿用旧配置，也不因一次数据库抖动就整体放开过滤
            log.debug("加载「启用中记录规则的模型」失败，沿用上一次快照: {}", e.getMessage());
        } finally {
            enabledModelsExpireAt = now + ENABLED_MODELS_CACHE_TTL_MS;
            loadingEnabledModels.set(false);
        }
        return enabledModelsCache;
    }

    /** 规则发生增删改后让缓存立即失效，配置改动最多滞后一次查询 */
    private void invalidateEnabledModelsCache() {
        enabledModelsExpireAt = 0L;
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