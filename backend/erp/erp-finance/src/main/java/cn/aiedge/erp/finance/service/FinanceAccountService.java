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
     * 查询账户统计信息（支持与列表同口径的过滤条件；全部为 null 时统计全部账户）
     *
     * @param status       状态 0-停用 1-启用（null=全部）
     * @param accountType  账户类型 1-银行 2-现金 3-内部 4-外部（null=全部）
     * @param accountLevel 账户等级 1-基本 2-一般 3-专用（null=全部）
     * @param currency     币种（null=全部）
     * @param keyword      账户名称/开户银行/银行账号模糊匹配（null/空=不限）
     */
    Object getAccountStatistics(Integer status, Integer accountType, Integer accountLevel, String currency, String keyword);
    
    /**
     * 启用/停用账户
     */
    boolean updateAccountStatus(Long id, Integer status);
    
    /**
     * 更新账户余额
     */
    boolean updateAccountBalance(Long accountId, BigDecimal amount);
}
