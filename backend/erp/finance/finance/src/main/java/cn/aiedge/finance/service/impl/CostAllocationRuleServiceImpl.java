package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.CostAllocationRule;
import cn.aiedge.finance.mapper.CostAllocationRuleMapper;
import cn.aiedge.finance.service.CostAllocationRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CostAllocationRuleServiceImpl extends ServiceImpl<CostAllocationRuleMapper, CostAllocationRule> implements CostAllocationRuleService {
    
    @Override
    public List<CostAllocationRule> listAllEnabled(Long tenantId) {
        return baseMapper.listAllEnabled(tenantId);
    }
    
    @Override
    public List<CostAllocationRule> listByFromCenter(Long tenantId, Long fromCenterId) {
        return baseMapper.listByFromCenter(tenantId, fromCenterId);
    }
    
    @Override
    public CostAllocationRule getByCode(Long tenantId, String ruleCode) {
        return baseMapper.getByCode(tenantId, ruleCode);
    }
    
    @Override
    public Page<CostAllocationRule> pageList(Long tenantId, Long fromCenterId, Integer enabled, Page<CostAllocationRule> page) {
        LambdaQueryWrapper<CostAllocationRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostAllocationRule::getTenantId, tenantId)
               .eq(CostAllocationRule::getDeleted, 0);
        if (fromCenterId != null) {
            wrapper.eq(CostAllocationRule::getFromCenterId, fromCenterId);
        }
        if (enabled != null) {
            wrapper.eq(CostAllocationRule::getEnabled, enabled);
        }
        wrapper.orderByAsc(CostAllocationRule::getPriority);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRule(CostAllocationRule rule) {
        CostAllocationRule existing = this.getByCode(rule.getTenantId(), rule.getRuleCode());
        if (existing != null) {
            throw new RuntimeException("分配规则编码已存在");
        }
        rule.setEnabled(1);
        return this.save(rule);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRule(CostAllocationRule rule) {
        return this.updateById(rule);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRule(Long tenantId, Long ruleId) {
        return this.removeById(ruleId);
    }
    
    @Override
    public boolean enableRule(Long tenantId, Long ruleId) {
        CostAllocationRule rule = this.getById(ruleId);
        if (rule == null) {
            throw new RuntimeException("分配规则不存在");
        }
        rule.setEnabled(1);
        return this.updateById(rule);
    }
    
    @Override
    public boolean disableRule(Long tenantId, Long ruleId) {
        CostAllocationRule rule = this.getById(ruleId);
        if (rule == null) {
            throw new RuntimeException("分配规则不存在");
        }
        rule.setEnabled(0);
        return this.updateById(rule);
    }
}