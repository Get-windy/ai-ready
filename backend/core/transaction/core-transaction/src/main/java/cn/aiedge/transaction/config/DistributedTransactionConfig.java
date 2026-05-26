package cn.aiedge.transaction.config;

import cn.aiedge.transaction.aspect.DistributedTransactionAspect;
import cn.aiedge.transaction.service.*;
import cn.aiedge.transaction.service.impl.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 分布式事务配置
 * 配置分布式事务相关的组件和切面
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DistributedTransactionConfig {

    /**
     * 配置分布式事务切面
     * 
     * @param transactionManager 事务管理器
     * @param tccTransactionManager TCC事务管理器
     * @param sagaTransactionManager Saga事务管理器
     * @param bestEffortNotifyTransactionManager 最大努力通知事务管理器
     * @return 分布式事务切面
     */
    @Bean
    @ConditionalOnMissingBean(DistributedTransactionAspect.class)
    public DistributedTransactionAspect distributedTransactionAspect(
            TransactionManager transactionManager,
            TccTransactionManager tccTransactionManager,
            SagaTransactionManager sagaTransactionManager,
            BestEffortNotifyTransactionManager bestEffortNotifyTransactionManager) {
        return new DistributedTransactionAspect(
                transactionManager,
                tccTransactionManager,
                sagaTransactionManager,
                bestEffortNotifyTransactionManager
        );
    }

    /**
     * 配置事务管理器
     * 
     * @param transactionManagerImpl 事务管理器实现
     * @return 事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(TransactionManager.class)
    public TransactionManager transactionManager(TransactionManagerImpl transactionManagerImpl) {
        return transactionManagerImpl;
    }

    /**
     * 配置TCC事务管理器
     * 
     * @param tccTransactionManagerImpl TCC事务管理器实现
     * @return TCC事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(TccTransactionManager.class)
    public TccTransactionManager tccTransactionManager(TccTransactionManagerImpl tccTransactionManagerImpl) {
        return tccTransactionManagerImpl;
    }

    /**
     * 配置Saga事务管理器
     * 
     * @param sagaTransactionManagerImpl Saga事务管理器实现
     * @return Saga事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(SagaTransactionManager.class)
    public SagaTransactionManager sagaTransactionManager(SagaTransactionManagerImpl sagaTransactionManagerImpl) {
        return sagaTransactionManagerImpl;
    }

    /**
     * 配置最大努力通知事务管理器
     * 
     * @param bestEffortNotifyTransactionManagerImpl 最大努力通知事务管理器实现
     * @return 最大努力通知事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(BestEffortNotifyTransactionManager.class)
    public BestEffortNotifyTransactionManager bestEffortNotifyTransactionManager(
            BestEffortNotifyTransactionManagerImpl bestEffortNotifyTransactionManagerImpl) {
        return bestEffortNotifyTransactionManagerImpl;
    }
}