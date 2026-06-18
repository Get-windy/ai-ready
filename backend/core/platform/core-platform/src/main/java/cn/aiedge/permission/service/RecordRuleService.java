package cn.aiedge.permission.service;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.RecordRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

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

    boolean checkRecordAccess(String modelName, Long recordId, String operation, PermissionContext context);

    List<Long> filterRecords(String modelName, List<Long> recordIds, String operation, PermissionContext context);

    boolean evaluateDomain(String domain, PermissionContext context);
}