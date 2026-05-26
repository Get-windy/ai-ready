package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.mapper.CustomerMapper;
import cn.aiedge.crm.customer.service.CustomerCreditService;
import cn.aiedge.crm.customer.service.CustomerService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCreditServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerCreditService {

    private final CustomerService customerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Customer setCreditLimit(Long customerId, BigDecimal creditLimit) {
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        
        customer.setCreditLimit(creditLimit);
        updateById(customer);
        
        log.info("设置客户信用额度: {} - {}", customer.getCustomerName(), creditLimit);
        return getById(customerId);
    }

    @Override
    public BigDecimal getCreditLimit(Long customerId) {
        Customer customer = customerService.getById(customerId);
        if (customer == null || customer.getCreditLimit() == null) {
            return BigDecimal.ZERO;
        }
        return customer.getCreditLimit();
    }

    @Override
    public BigDecimal getCurrentDebt(Long customerId) {
        Customer customer = customerService.getById(customerId);
        if (customer == null || customer.getCurrentDebt() == null) {
            return BigDecimal.ZERO;
        }
        return customer.getCurrentDebt();
    }

    @Override
    public BigDecimal getAvailableCredit(Long customerId) {
        BigDecimal creditLimit = getCreditLimit(customerId);
        BigDecimal currentDebt = getCurrentDebt(customerId);
        
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("999999999.99");
        }
        
        return creditLimit.subtract(currentDebt).max(BigDecimal.ZERO);
    }

    @Override
    public boolean checkCreditAvailable(Long customerId, BigDecimal newAmount) {
        if (isCreditFrozen(customerId)) {
            return false;
        }
        
        BigDecimal availableCredit = getAvailableCredit(customerId);
        return availableCredit.compareTo(newAmount) >= 0;
    }

    @Override
    public Map<String, Object> getCreditStatus(Long customerId) {
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("customerId", customerId);
        status.put("customerName", customer.getCustomerName());
        status.put("creditLimit", customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO);
        status.put("currentDebt", customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO);
        status.put("availableCredit", getAvailableCredit(customerId));
        
        BigDecimal usageRate = BigDecimal.ZERO;
        if (customer.getCreditLimit() != null && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
            usageRate = debt.divide(customer.getCreditLimit(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        status.put("usageRate", usageRate);
        
        String creditStatus = "normal";
        if (usageRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            creditStatus = "warning";
        } else if (usageRate.compareTo(BigDecimal.valueOf(100)) >= 0) {
            creditStatus = "overdue";
        }
        status.put("creditStatus", creditStatus);
        
        return status;
    }

    @Override
    public List<Map<String, Object>> getCreditWarningList() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0)
               .isNotNull(Customer::getCreditLimit)
               .gt(Customer::getCreditLimit, BigDecimal.ZERO);
        
        List<Customer> customers = list(wrapper);
        List<Map<String, Object>> warningList = new ArrayList<>();
        
        for (Customer customer : customers) {
            BigDecimal creditLimit = customer.getCreditLimit();
            BigDecimal currentDebt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
            
            BigDecimal usageRate = currentDebt.divide(creditLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            
            if (usageRate.compareTo(BigDecimal.valueOf(80)) >= 0 && usageRate.compareTo(BigDecimal.valueOf(100)) < 0) {
                Map<String, Object> warning = new HashMap<>();
                warning.put("customerId", customer.getId());
                warning.put("customerName", customer.getCustomerName());
                warning.put("creditLimit", creditLimit);
                warning.put("currentDebt", currentDebt);
                warning.put("availableCredit", creditLimit.subtract(currentDebt));
                warning.put("usageRate", usageRate);
                warningList.add(warning);
            }
        }
        
        warningList.sort((a, b) -> ((BigDecimal) b.get("usageRate")).compareTo((BigDecimal) a.get("usageRate")));
        return warningList;
    }

    @Override
    public List<Map<String, Object>> getOverCreditList() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0)
               .isNotNull(Customer::getCreditLimit)
               .gt(Customer::getCreditLimit, BigDecimal.ZERO);
        
        List<Customer> customers = list(wrapper);
        List<Map<String, Object>> overCreditList = new ArrayList<>();
        
        for (Customer customer : customers) {
            BigDecimal creditLimit = customer.getCreditLimit();
            BigDecimal currentDebt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
            
            if (currentDebt.compareTo(creditLimit) > 0) {
                Map<String, Object> overCredit = new HashMap<>();
                overCredit.put("customerId", customer.getId());
                overCredit.put("customerName", customer.getCustomerName());
                overCredit.put("creditLimit", creditLimit);
                overCredit.put("currentDebt", currentDebt);
                overCredit.put("overAmount", currentDebt.subtract(creditLimit));
                overCredit.put("usageRate", currentDebt.divide(creditLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
                overCreditList.add(overCredit);
            }
        }
        
        overCreditList.sort((a, b) -> ((BigDecimal) b.get("overAmount")).compareTo((BigDecimal) a.get("overAmount")));
        return overCreditList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerDebt(Long customerId) {
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            return;
        }
        
        BigDecimal totalDebt = calculateTotalDebt(customerId);
        customer.setCurrentDebt(totalDebt);
        updateById(customer);
        
        log.info("更新客户欠款: {} - {}", customer.getCustomerName(), totalDebt);
    }

    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateDebt() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0);
        
        List<Customer> customers = list(wrapper);
        for (Customer customer : customers) {
            try {
                updateCustomerDebt(customer.getId());
            } catch (Exception e) {
                log.error("更新客户欠款失败: {}", customer.getCustomerName(), e);
            }
        }
        
        log.info("批量更新客户欠款完成，共{}个客户", customers.size());
    }

    @Override
    public Map<String, Object> getCreditStatistics() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0)
               .isNotNull(Customer::getCreditLimit)
               .gt(Customer::getCreditLimit, BigDecimal.ZERO);
        
        List<Customer> customers = list(wrapper);
        
        BigDecimal totalCreditLimit = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        int warningCount = 0;
        int overCreditCount = 0;
        int normalCount = 0;
        
        for (Customer customer : customers) {
            BigDecimal creditLimit = customer.getCreditLimit();
            BigDecimal debt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
            
            totalCreditLimit = totalCreditLimit.add(creditLimit);
            totalDebt = totalDebt.add(debt);
            
            BigDecimal usageRate = debt.divide(creditLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            
            if (usageRate.compareTo(BigDecimal.valueOf(100)) >= 0) {
                overCreditCount++;
            } else if (usageRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
                warningCount++;
            } else {
                normalCount++;
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCreditLimit", totalCreditLimit);
        stats.put("totalDebt", totalDebt);
        stats.put("totalAvailableCredit", totalCreditLimit.subtract(totalDebt));
        stats.put("customerCount", customers.size());
        stats.put("normalCount", normalCount);
        stats.put("warningCount", warningCount);
        stats.put("overCreditCount", overCreditCount);
        
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Customer freezeCredit(Long customerId, String reason) {
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        
        customer.setStatus(4);
        customer.setStatusDesc("信用冻结: " + reason);
        updateById(customer);
        
        log.info("冻结客户信用: {} - 原因: {}", customer.getCustomerName(), reason);
        return getById(customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Customer unfreezeCredit(Long customerId) {
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        
        customer.setStatus(1);
        customer.setStatusDesc("正常");
        updateById(customer);
        
        log.info("解冻客户信用: {}", customer.getCustomerName());
        return getById(customerId);
    }

    @Override
    public boolean isCreditFrozen(Long customerId) {
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            return true;
        }
        return customer.getStatus() == 4;
    }

    private BigDecimal calculateTotalDebt(Long customerId) {
        return BigDecimal.ZERO;
    }
}