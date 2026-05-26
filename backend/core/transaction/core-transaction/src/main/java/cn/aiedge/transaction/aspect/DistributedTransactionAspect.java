package cn.aiedge.transaction.aspect;

import cn.aiedge.transaction.annotation.DistributedTransaction;
import cn.aiedge.transaction.enums.TransactionMode;
import cn.aiedge.transaction.model.TransactionContext;
import cn.aiedge.transaction.model.TransactionStatus;
import cn.aiedge.transaction.service.*;
import cn.aiedge.transaction.util.TransactionIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 分布式事务切面
 * 拦截带有@DistributedTransaction注解的方法，自动执行分布式事务管理
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedTransactionAspect {

    private final TransactionManager transactionManager;
    private final TccTransactionManager tccTransactionManager;
    private final SagaTransactionManager sagaTransactionManager;
    private final BestEffortNotifyTransactionManager bestEffortNotifyTransactionManager;

    @Around("@annotation(distributedTransaction)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedTransaction distributedTransaction) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        log.info("Intercepting method with distributed transaction: {}", methodName);

        // 创建事务上下文
        TransactionContext context = createContext(distributedTransaction, methodName);

        try {
            // 根据事务模式执行对应的事务处理
            TransactionStatus status;
            switch (distributedTransaction.mode()) {
                case TCC:
                    status = tccTransactionManager.executeTccTransaction(context);
                    break;
                case SAGA:
                    status = sagaTransactionManager.executeSagaTransaction(context);
                    break;
                case BEST_EFFORT:
                    status = bestEffortNotifyTransactionManager.executeBestEffortNotify(context);
                    break;
                default:
                    log.error("Unsupported transaction mode: {}", distributedTransaction.mode());
                    return joinPoint.proceed(); // 如果不支持，则直接执行原方法
            }

            if (status == TransactionStatus.SUCCESS || status == TransactionStatus.CONFIRMED) {
                log.info("Distributed transaction completed successfully for method: {}", methodName);
                return joinPoint.proceed(); // 执行原方法
            } else {
                log.error("Distributed transaction failed for method: {}, status: {}", methodName, status.getDescription());
                throw new RuntimeException("Distributed transaction failed: " + status.getDescription());
            }
        } catch (Exception e) {
            log.error("Error occurred during distributed transaction for method: {}", methodName, e);
            throw e;
        }
    }

    /**
     * 创建事务上下文
     * 
     * @param annotation 分布式事务注解
     * @param methodName 方法名称
     * @return 事务上下文
     */
    private TransactionContext createContext(DistributedTransaction annotation, String methodName) {
        String globalTxId = TransactionIdGenerator.generateGlobalTxId();
        
        TransactionContext context = new TransactionContext()
                .setGlobalTxId(globalTxId)
                .setTransactionName(annotation.name().isEmpty() ? methodName : annotation.name())
                .setTransactionMode(annotation.mode().getCode())
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now())
                .setTimeoutTime(LocalDateTime.now().plusSeconds(annotation.timeout()))
                .setMaxRetries(annotation.maxRetries());

        // 设置参与者信息
        if (annotation.participants().length > 0) {
            TransactionContext.ParticipantInfo participantInfo = new TransactionContext.ParticipantInfo()
                    .setServiceName(String.join(",", annotation.participants()));
            context.setParticipantInfo(participantInfo);
        }

        return context;
    }
}