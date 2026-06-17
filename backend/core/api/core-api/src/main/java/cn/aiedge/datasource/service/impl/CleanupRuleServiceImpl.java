package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.mapper.CleanupRuleMapper;
import cn.aiedge.datasource.model.CleanupRule;
import cn.aiedge.datasource.service.CleanupRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据清理规则服务实现
 */
@Service
@RequiredArgsConstructor
public class CleanupRuleServiceImpl implements CleanupRuleService {

    private final CleanupRuleMapper cleanupRuleMapper;

    @Override
    public List<CleanupRule> list(Long tenantId) {
        LambdaQueryWrapper<CleanupRule> wrapper = new LambdaQueryWrapper<CleanupRule>()
                .eq(CleanupRule::getDeleted, false)
                .eq(tenantId != null, CleanupRule::getTenantId, tenantId)
                .orderByDesc(CleanupRule::getCreateTime);
        return cleanupRuleMapper.selectList(wrapper);
    }

    @Override
    public CleanupRule create(CleanupRule rule, Long tenantId, String createBy) {
        LocalDateTime now = LocalDateTime.now();
        rule.setTenantId(tenantId);
        rule.setCreateBy(createBy);
        rule.setUpdateBy(createBy);
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        rule.setDeleted(false);
        if (rule.getStatus() == null) rule.setStatus("stopped");
        cleanupRuleMapper.insert(rule);
        return rule;
    }

    @Override
    public CleanupRule update(Long id, CleanupRule rule, Long tenantId, String updateBy) {
        CleanupRule existing = cleanupRuleMapper.selectById(id);
        if (existing == null || existing.getDeleted()) return null;
        if (rule.getRuleName() != null) existing.setRuleName(rule.getRuleName());
        if (rule.getTargetTable() != null) existing.setTargetTable(rule.getTargetTable());
        if (rule.getConditionColumn() != null) existing.setConditionColumn(rule.getConditionColumn());
        if (rule.getRetentionDays() != null) existing.setRetentionDays(rule.getRetentionDays());
        if (rule.getCronExpression() != null) existing.setCronExpression(rule.getCronExpression());
        if (rule.getStatus() != null) existing.setStatus(rule.getStatus());
        if (rule.getDescription() != null) existing.setDescription(rule.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateBy(updateBy);
        if (tenantId != null) existing.setTenantId(tenantId);
        cleanupRuleMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        CleanupRule existing = cleanupRuleMapper.selectById(id);
        if (existing == null || existing.getDeleted()) return false;
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        return cleanupRuleMapper.updateById(existing) > 0;
    }

    @Override
    public boolean execute(Long id) {
        CleanupRule rule = cleanupRuleMapper.selectById(id);
        if (rule == null || rule.getDeleted()) return false;
        rule.setStatus("running");
        rule.setUpdateTime(LocalDateTime.now());
        return cleanupRuleMapper.updateById(rule) > 0;
    }
}
