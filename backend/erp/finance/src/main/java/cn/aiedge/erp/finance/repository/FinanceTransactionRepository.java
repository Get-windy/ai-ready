package cn.aiedge.erp.finance.repository;

import cn.aiedge.erp.finance.model.entity.FinanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务交易Repository接口
 */
@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long>, JpaSpecificationExecutor<FinanceTransaction> {
    
    /**
     * 根据交易编号查询
     */
    FinanceTransaction findByTransactionNo(String transactionNo);
    
    /**
     * 根据交易类型查询
     */
    List<FinanceTransaction> findByTransactionType(Integer transactionType);
    
    /**
     * 根据状态查询
     */
    List<FinanceTransaction> findByStatus(Integer status);
    
    /**
     * 根据交易时间范围查询
     */
    List<FinanceTransaction> findByTransactionTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 根据业务类型和业务ID查询
     */
    List<FinanceTransaction> findByBizTypeAndBizId(Integer bizType, Long bizId);
    
    /**
     * 根据租户ID查询
     */
    List<FinanceTransaction> findByTenantId(String tenantId);
    
    /**
     * 查询最近的交易记录
     */
    List<FinanceTransaction> findByDeletedFlagOrderByTransactionTimeDesc(Integer deletedFlag);
}
