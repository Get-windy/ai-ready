package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.entity.FinanceAccount;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务账户Service接口
 */
public interface FinanceAccountService {
    
    /**
     * 分页查询财务账户列表
     */
    List<FinanceAccount> listAccounts(Integer status, String accountType);
    
    /**
     * 查询账户统计信息
     */
    Object getAccountStatistics();
    
    /**
     * 启用/停用账户
     */
    boolean updateAccountStatus(Long id, Integer status);
    
    /**
     * 更新账户余额
     */
    boolean updateAccountBalance(Long accountId, BigDecimal amount);
}
