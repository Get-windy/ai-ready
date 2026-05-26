package cn.aiedge.transaction.service;

import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;

/**
 * Saga模式事务管理器接口
 * Saga模式：一系列本地事务组成，每个本地事务更新数据并发布消息/事件
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SagaTransactionManager {

    /**
     * 执行Saga正向事务
     * 
     * @param transactionContext 事务上下文
     * @return 执行结果
     */
    boolean forwardPhase(TransactionContext transactionContext);

    /**
     * 执行Saga补偿事务
     * 
     * @param transactionContext 事务上下文
     * @return 执行结果
     */
    boolean compensatePhase(TransactionContext transactionContext);

    /**
     * 执行Saga完整流程
     * 
     * @param transactionContext 事务上下文
     * @return 事务状态
     */
    TransactionStatus executeSagaTransaction(TransactionContext transactionContext);
}