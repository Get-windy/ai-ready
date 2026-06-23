package cn.aiedge.erp.customer.service;

import cn.aiedge.erp.customer.entity.CustomerLevel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CustomerLevelService extends IService<CustomerLevel> {
    
    List<CustomerLevel> listAll(Long tenantId);
    
    List<CustomerLevel> listEnabled(Long tenantId);
    
    CustomerLevel getByCode(Long tenantId, String levelCode);
    
    Page<CustomerLevel> pageList(Long tenantId, String levelName, String levelCode, Boolean enabled, Page<CustomerLevel> page);
    
    boolean createLevel(CustomerLevel level);
    
    boolean updateLevel(CustomerLevel level);
    
    boolean deleteLevel(Long tenantId, Long levelId);
    
    boolean enableLevel(Long tenantId, Long levelId);
    
    boolean disableLevel(Long tenantId, Long levelId);
    
    CustomerLevel calculateCustomerLevel(Long tenantId, Double totalAmount, Integer frequency, Integer paymentRate);
    
    boolean assignLevelToCustomer(Long tenantId, Long customerId);
    
    List<CustomerLevel> getLevelPriceRules(Long tenantId, Long levelId);
    
    boolean updateLevelPriceRule(Long tenantId, Long levelId, Long productId, Double specialPrice);
}