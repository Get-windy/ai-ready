package cn.aiedge.transaction.service;

import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;

/**
 * TCC模式事务管理器接口
 * TCC模式：Try-Confirm-Cancel
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface TccTransactionManager {

    /**
     * TCC Try阶段
     * 预留资源，检查业务活动的可行性
     * 
     * @param transactionContext 事务上下文
     * @return 执行结果
     */
    boolean tryPhase(TransactionContext transactionContext);

    /**
     * TCC Confirm阶段
     * 确认执行，真正提交业务活动
     * 
     * @param transactionContext 事务上下文
     * @return 执行结果
     */
    boolean confirmPhase(TransactionContext transactionContext);

    /**
     * TCC Cancel阶段
     * 取消执行，释放预留资源
     * 
     * @param transactionContext 事务上下文
     * @return 执行结果
     */
    boolean cancelPhase(TransactionContext transactionContext);

    /**
     * 执行TCC完整流程
     * 
     * @param transactionContext 事务上下文
     * @return 事务状态
     */
    TransactionStatus executeTccTransaction(TransactionContext transactionContext);
}