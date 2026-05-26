package cn.aiedge.transaction.service;

import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;

/**
 * 最大努力通知模式事务管理器接口
 * 最大努力通知模式：通过不断重试直到成功或达到最大重试次数
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface BestEffortNotifyTransactionManager {

    /**
     * 发送通知
     * 
     * @param transactionContext 事务上下文
     * @return 执行结果
     */
    boolean sendNotification(TransactionContext transactionContext);

    /**
     * 执行最大努力通知
     * 
     * @param transactionContext 事务上下文
     * @return 事务状态
     */
    TransactionStatus executeBestEffortNotify(TransactionContext transactionContext);
}