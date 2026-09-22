package cn.aiedge.permission.service;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.RecordRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Set;

public interface RecordRuleService {

    RecordRule createRule(RecordRuleCreateRequest request);

    RecordRule updateRule(Long id, RecordRuleCreateRequest request);

    RecordRule getRuleById(Long id);

    List<RecordRule> getRulesByModel(String modelName);

    List<RecordRule> getRulesByUser(Long userId);

    List<RecordRule> getRulesByGroup(Long groupId);

    Page<RecordRule> listRules(Integer page, Integer size, String modelName, Long userId, Long groupId);

    void deleteRule(Long id);

    void activateRule(Long id);

    void deactivateRule(Long id);

    String buildDomainFilter(String modelName, PermissionContext context);

    /**
     * 供数据层拦截器消费：按「读」操作构建某模型的 domain 过滤表达式字符串。
     *
     * <p>与 {@link #buildDomainFilter} 同一套规则装配逻辑，只是把上下文压成三个参数，
     * 免去拦截器自己拼 {@link PermissionContext}。无规则或无权时返回 {@code null}。</p>
     *
     * @param modelName 模型名（本仓约定取 SQL 主表名）
     * @param userId    当前用户ID
     * @param groupIds  当前用户所属组（本仓即角色ID）列表，可为 null
     */
    String buildReadDomainFilter(String modelName, Long userId, List<Long> groupIds);

    /**
     * 「已配置启用中记录规则」的模型名集合（小写）。
     *
     * <p><b>集合为空时拦截器直接返回</b> —— 表 0 行即无规则，对查询零影响。</p>
     */
    Set<String> getEnabledRecordRuleModels();

    boolean checkRecordAccess(String modelName, Long recordId, String operation, PermissionContext context);

    List<Long> filterRecords(String modelName, List<Long> recordIds, String operation, PermissionContext context);

    boolean evaluateDomain(String domain, PermissionContext context);
}