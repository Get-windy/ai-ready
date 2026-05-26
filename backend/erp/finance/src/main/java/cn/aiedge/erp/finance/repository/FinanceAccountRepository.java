package cn.aiedge.erp.finance.repository;

import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 财务账户Repository接口
 */
@Repository
public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long>, JpaSpecificationExecutor<FinanceAccount> {
    
    /**
     * 根据账户名称查询
     */
    List<FinanceAccount> findByAccountNameLike(String accountName);
    
    /**
     * 根据账户类型查询
     */
    List<FinanceAccount> findByAccountType(Integer accountType);
    
    /**
     * 根据状态查询
     */
    List<FinanceAccount> findByStatus(Integer status);
    
    /**
     * 根据租户ID查询
     */
    List<FinanceAccount> findByTenantId(String tenantId);
    
    /**
     * 查询所有启用的账户
     */
    List<FinanceAccount> findByStatusAndDeletedFlag(Integer status, Integer deletedFlag);
}
