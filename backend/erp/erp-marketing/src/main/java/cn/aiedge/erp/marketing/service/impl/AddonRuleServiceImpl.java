package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.AddonRule;
import cn.aiedge.erp.marketing.mapper.AddonRuleMapper;
import cn.aiedge.erp.marketing.service.AddonRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddonRuleServiceImpl extends ServiceImpl<AddonRuleMapper, AddonRule>
        implements AddonRuleService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        AddonRule rule = mustGet(id);
        if (rule.getStatus() == AddonRule.STATUS_ENABLED) {
            throw new IllegalArgumentException("规则已启用，无需重复操作");
        }
        if (rule.getEndTime() != null && rule.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("规则已过期，不能启用");
        }
        rule.setStatus(AddonRule.STATUS_ENABLED);
        updateById(rule);
        log.info("加价购规则已启用: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        AddonRule rule = mustGet(id);
        if (rule.getStatus() == AddonRule.STATUS_DISABLED) {
            throw new IllegalArgumentException("规则已停用，无需重复操作");
        }
        rule.setStatus(AddonRule.STATUS_DISABLED);
        updateById(rule);
        log.info("加价购规则已停用: id={}", id);
    }

    private AddonRule mustGet(Long id) {
        AddonRule rule = getById(id);
        if (rule == null) {
            throw new IllegalArgumentException("加价购规则不存在: " + id);
        }
        return rule;
    }
}
