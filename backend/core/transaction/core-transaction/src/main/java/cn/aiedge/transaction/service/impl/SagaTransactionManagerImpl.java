package cn.aiedge.transaction.service.impl;

import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;
import cn.aiedge.transaction.service.SagaTransactionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Saga模式事务管理器实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaTransactionManagerImpl implements SagaTransactionManager {

    @Override
    public boolean forwardPhase(TransactionContext transactionContext) {
        log.info("Starting Saga Forward phase for transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行Saga正向事务逻辑
            // 按顺序执行各个本地事务
            // 这里应该调用实际的业务方法
            
            log.info("Saga Forward phase completed successfully for transaction: {}", transactionContext.getGlobalTxId());
            return true;
        } catch (Exception e) {
            log.error("Saga Forward phase failed for transaction: {}", transactionContext.getGlobalTxId(), e);
            return false;
        }
    }

    @Override
    public boolean compensatePhase(TransactionContext transactionContext) {
        log.info("Starting Saga Compensate phase for transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行Saga补偿事务逻辑
            // 按相反顺序执行补偿操作
            // 这里应该调用实际的补偿方法
            
            log.info("Saga Compensate phase completed successfully for transaction: {}", transactionContext.getGlobalTxId());
            return true;
        } catch (Exception e) {
            log.error("Saga Compensate phase failed for transaction: {}", transactionContext.getGlobalTxId(), e);
            return false;
        }
    }

    @Override
    public TransactionStatus executeSagaTransaction(TransactionContext transactionContext) {
        log.info("Executing Saga transaction: {}", transactionContext.getGlobalTxId());
        
        try {
            // 执行正向事务
            if (!forwardPhase(transactionContext)) {
                log.warn("Saga Forward phase failed, compensating transaction: {}", transactionContext.getGlobalTxId());
                
                // 正向事务失败，执行补偿事务
                if (compensatePhase(transactionContext)) {
                    log.info("Saga transaction compensated successfully: {}", transactionContext.getGlobalTxId());
                    return TransactionStatus.COMPENSATED;
                } else {
                    log.error("Saga compensation failed for transaction: {}", transactionContext.getGlobalTxId());
                    return TransactionStatus.FAILED;
                }
            }

            // 正向事务成功
            log.info("Saga transaction completed successfully: {}", transactionContext.getGlobalTxId());
            return TransactionStatus.SUCCESS;
        } catch (Exception e) {
            log.error("Unexpected error during Saga transaction: {}", transactionContext.getGlobalTxId(), e);
            try {
                compensatePhase(transactionContext);
            } catch (Exception compensateException) {
                log.error("Error during compensation after unexpected error: {}", transactionContext.getGlobalTxId(), compensateException);
            }
            return TransactionStatus.FAILED;
        }
    }
}