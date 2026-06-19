package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysSodRule;
import cn.aiedge.base.mapper.SysSodRuleMapper;
import cn.aiedge.base.service.SysSodRuleService;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职责分离规则服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSodRuleServiceImpl extends ServiceImpl<SysSodRuleMapper, SysSodRule>
        implements SysSodRuleService {

    private final SysSodRuleMapper sysSodRuleMapper;

    @Override
    public List<SysSodRule> getActiveRules() {
        return sysSodRuleMapper.selectActiveRules();
    }

    @Override
    public List<SysSodRule> validateRoleAssignment(Long userId, List<Long> newRoleIds) {
        if (newRoleIds == null || newRoleIds.isEmpty()) {
            return List.of();
        }

        // 1. 找出新角色自身的互斥冲突
        List<Long> selfConflicts = findConflictingRoleIds(newRoleIds);
        if (!selfConflicts.isEmpty()) {
            // 新角色列表本身就有互斥，立即拒绝
            return getActiveRules().stream()
                    .filter(rule -> containsAny(parseConflictRoleIds(rule.getConflictRoleIds()), selfConflicts))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public List<Long> findConflictingRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        Set<Long> conflicting = new HashSet<>();
        List<SysSodRule> rules = getActiveRules();

        for (SysSodRule rule : rules) {
            List<Long> conflictIds = parseConflictRoleIds(rule.getConflictRoleIds());
            // 检查 roleIds 中是否有两个或以上属于同一个互斥集合
            List<Long> matched = roleIds.stream()
                    .filter(conflictIds::contains)
                    .collect(Collectors.toList());
            if (matched.size() > 1) {
                conflicting.addAll(matched);
            }
        }
        return new ArrayList<>(conflicting);
    }

    @Override
    public boolean hasConflict(List<Long> roleIdsA, List<Long> roleIdsB) {
        if (roleIdsA == null || roleIdsA.isEmpty() || roleIdsB == null || roleIdsB.isEmpty()) {
            return false;
        }

        List<SysSodRule> rules = getActiveRules();
        for (SysSodRule rule : rules) {
            List<Long> conflictIds = parseConflictRoleIds(rule.getConflictRoleIds());
            boolean aInConflict = roleIdsA.stream().anyMatch(conflictIds::contains);
            boolean bInConflict = roleIdsB.stream().anyMatch(conflictIds::contains);
            if (aInConflict && bInConflict) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 JSON 数组格式的互斥角色ID
     */
    private List<Long> parseConflictRoleIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSONUtil.parseArray(json).stream()
                    .map(o -> Long.valueOf(o.toString()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析互斥角色ID失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 检查集合是否包含任一元素
     */
    private boolean containsAny(List<Long> list, List<Long> candidates) {
        return candidates.stream().anyMatch(list::contains);
    }
}
