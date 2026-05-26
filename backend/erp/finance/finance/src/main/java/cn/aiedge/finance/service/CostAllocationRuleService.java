package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.CostAllocationRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CostAllocationRuleService extends IService<CostAllocationRule> {
    
    List<CostAllocationRule> listAllEnabled(Long tenantId);
    
    List<CostAllocationRule> listByFromCenter(Long tenantId, Long fromCenterId);
    
    CostAllocationRule getByCode(Long tenantId, String ruleCode);
    
    Page<CostAllocationRule> pageList(Long tenantId, Long fromCenterId, Integer enabled, Page<CostAllocationRule> page);
    
    boolean createRule(CostAllocationRule rule);
    
    boolean updateRule(CostAllocationRule rule);
    
    boolean deleteRule(Long tenantId, Long ruleId);
    
    boolean enableRule(Long tenantId, Long ruleId);
    
    boolean disableRule(Long tenantId, Long ruleId);
}