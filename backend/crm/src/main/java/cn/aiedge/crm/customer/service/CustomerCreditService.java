package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.entity.Customer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CustomerCreditService {
    
    Customer setCreditLimit(Long customerId, BigDecimal creditLimit);
    
    BigDecimal getCreditLimit(Long customerId);
    
    BigDecimal getCurrentDebt(Long customerId);
    
    BigDecimal getAvailableCredit(Long customerId);
    
    boolean checkCreditAvailable(Long customerId, BigDecimal newAmount);
    
    Map<String, Object> getCreditStatus(Long customerId);
    
    List<Map<String, Object>> getCreditWarningList();
    
    List<Map<String, Object>> getOverCreditList();
    
    void updateCustomerDebt(Long customerId);
    
    void batchUpdateDebt();
    
    Map<String, Object> getCreditStatistics();
    
    Customer freezeCredit(Long customerId, String reason);
    
    Customer unfreezeCredit(Long customerId);
    
    boolean isCreditFrozen(Long customerId);
}