package cn.aiedge.transaction.service.impl;

import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;
import cn.aiedge.transaction.service.TccTransactionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TCC模式事务管理器实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TccTransactionManagerImpl implements TccTransactionManager {

    @Override
    public boolean tryPhase(TransactionContext transactionContext) {
        log.info("Starting TCC Try phase for transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行Try阶段的业务逻辑
            // 预留资源，检查业务活动的可行性
            // 这里应该调用实际的业务方法
            
            log.info("TCC Try phase completed successfully for transaction: {}", transactionContext.getGlobalTxId());
            return true;
        } catch (Exception e) {
            log.error("TCC Try phase failed for transaction: {}", transactionContext.getGlobalTxId(), e);
            return false;
        }
    }

    @Override
    public boolean confirmPhase(TransactionContext transactionContext) {
        log.info("Starting TCC Confirm phase for transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行Confirm阶段的业务逻辑
            // 真正提交业务活动，使用Try阶段预留的资源
            // 这里应该调用实际的业务方法
            
            log.info("TCC Confirm phase completed successfully for transaction: {}", transactionContext.getGlobalTxId());
            return true;
        } catch (Exception e) {
            log.error("TCC Confirm phase failed for transaction: {}", transactionContext.getGlobalTxId(), e);
            return false;
        }
    }

    @Override
    public boolean cancelPhase(TransactionContext transactionContext) {
        log.info("Starting TCC Cancel phase for transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行Cancel阶段的业务逻辑
            // 释放Try阶段预留的资源
            // 这里应该调用实际的业务方法
            
            log.info("TCC Cancel phase completed successfully for transaction: {}", transactionContext.getGlobalTxId());
            return true;
        } catch (Exception e) {
            log.error("TCC Cancel phase failed for transaction: {}", transactionContext.getGlobalTxId(), e);
            return false;
        }
    }

    @Override
    public TransactionStatus executeTccTransaction(TransactionContext transactionContext) {
        log.info("Executing TCC transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行Try阶段
            if (!tryPhase(transactionContext)) {
                log.warn("TCC Try phase failed, cancelling transaction: {}", transactionContext.getGlobalTxId());
                cancelPhase(transactionContext);
                return TransactionStatus.FAILED;
            }

            // Try阶段成功，执行Confirm阶段
            if (confirmPhase(transactionContext)) {
                log.info("TCC transaction completed successfully: {}", transactionContext.getGlobalTxId());
                return TransactionStatus.SUCCESS;
            } else {
                log.error("TCC Confirm phase failed, cancelling transaction: {}", transactionContext.getGlobalTxId());
                cancelPhase(transactionContext);
                return TransactionStatus.FAILED;
            }
        } catch (Exception e) {
            log.error("Unexpected error during TCC transaction: {}", transactionContext.getGlobalTxId(), e);
            try {
                cancelPhase(transactionContext);
            } catch (Exception cancelException) {
                log.error("Error during cancellation after unexpected error: {}", transactionContext.getGlobalTxId(), cancelException);
            }
            return TransactionStatus.FAILED;
        }
    }
}