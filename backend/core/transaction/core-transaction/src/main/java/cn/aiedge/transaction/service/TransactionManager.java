package cn.aiedge.transaction.service;

import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;

/**
 * 分布式事务管理器接口
 * 定义分布式事务的核心操作方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface TransactionManager {

    /**
     * 开启全局事务
     * 
     * @param transactionContext 事务上下文
     * @return 全局事务ID
     */
    String beginGlobalTransaction(TransactionContext transactionContext);

    /**
     * 提交全局事务
     * 
     * @param globalTxId 全局事务ID
     * @return 事务状态
     */
    TransactionStatus commitGlobalTransaction(String globalTxId);

    /**
     * 回滚全局事务
     * 
     * @param globalTxId 全局事务ID
     * @return 事务状态
     */
    TransactionStatus rollbackGlobalTransaction(String globalTxId);

    /**
     * 注册分支事务
     * 
     * @param globalTxId 全局事务ID
     * @param branchTxContext 分支事务上下文
     * @return 分支事务ID
     */
    String registerBranchTransaction(String globalTxId, TransactionContext branchTxContext);

    /**
     * 尝试执行分支事务(TCC模式)
     * 
     * @param branchTxId 分支事务ID
     * @return 执行结果
     */
    boolean tryBranchTransaction(String branchTxId);

    /**
     * 确认分支事务(TCC模式)
     * 
     * @param branchTxId 分支事务ID
     * @return 执行结果
     */
    boolean confirmBranchTransaction(String branchTxId);

    /**
     * 取消分支事务(TCC模式)
     * 
     * @param branchTxId 分支事务ID
     * @return 执行结果
     */
    boolean cancelBranchTransaction(String branchTxId);

    /**
     * 执行前向恢复(Saga模式)
     * 
     * @param branchTxId 分支事务ID
     * @return 执行结果
     */
    boolean forwardSagaTransaction(String branchTxId);

    /**
     * 执行补偿操作(Saga模式)
     * 
     * @param branchTxId 分支事务ID
     * @return 执行结果
     */
    boolean compensateSagaTransaction(String branchTxId);

    /**
     * 检查事务状态
     * 
     * @param txId 事务ID
     * @return 事务状态
     */
    TransactionStatus checkTransactionStatus(String txId);

    /**
     * 处理超时事务
     * 
     * @param globalTxId 全局事务ID
     * @return 处理结果
     */
    boolean handleTimeoutTransaction(String globalTxId);

    /**
     * 重试失败的事务
     * 
     * @param txId 事务ID
     * @return 重试结果
     */
    boolean retryFailedTransaction(String txId);
}