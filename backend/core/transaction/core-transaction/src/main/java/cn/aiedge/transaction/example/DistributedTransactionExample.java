package cn.aiedge.transaction.example;

import cn.aiedge.transaction.annotation.DistributedTransaction;
import cn.aiedge.transaction.enums.TransactionMode;
import cn.aiedge.transaction.service.TransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分布式事务使用示例
 * 展示如何在业务代码中使用分布式事务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class DistributedTransactionExample {

    @Autowired
    private TransactionManager transactionManager;

    /**
     * 使用TCC模式的分布式事务示例
     * 适用于需要两阶段提交的业务场景
     */
    @DistributedTransaction(
        name = "order-payment-inventory-transaction",
        mode = TransactionMode.TCC,
        timeout = 60,
        maxRetries = 3,
        participants = {"order-service", "payment-service", "inventory-service"}
    )
    public boolean placeOrderWithTcc(Long orderId, Long userId, Double amount) {
        // 执行订单创建逻辑
        boolean orderCreated = createOrder(orderId, userId, amount);
        if (!orderCreated) {
            return false;
        }

        // 执行支付逻辑
        boolean paymentProcessed = processPayment(userId, amount);
        if (!paymentProcessed) {
            return false;
        }

        // 执行库存扣减逻辑
        boolean inventoryDeducted = deductInventory(orderId);
        if (!inventoryDeducted) {
            return false;
        }

        return true;
    }

    /**
     * 使用Saga模式的分布式事务示例
     * 适用于长事务、补偿型业务场景
     */
    @DistributedTransaction(
        name = "customer-registration-saga",
        mode = TransactionMode.SAGA,
        timeout = 120,
        maxRetries = 2,
        participants = {"user-service", "profile-service", "notification-service"}
    )
    public boolean registerCustomerWithSaga(Long customerId, String email, String name) {
        // 执行用户创建逻辑
        boolean userCreated = createUser(customerId, email, name);
        if (!userCreated) {
            return false;
        }

        // 执行用户资料创建逻辑
        boolean profileCreated = createProfile(customerId, name);
        if (!profileCreated) {
            return false;
        }

        // 执行欢迎通知发送逻辑
        boolean notificationSent = sendWelcomeNotification(customerId, email);
        if (!notificationSent) {
            return false;
        }

        return true;
    }

    /**
     * 使用最大努力通知模式的分布式事务示例
     * 适用于最终一致性要求的业务场景
     */
    @DistributedTransaction(
        name = "async-data-sync-transaction",
        mode = TransactionMode.BEST_EFFORT,
        timeout = 300,
        maxRetries = 5,
        participants = {"master-db", "slave-db", "cache-cluster"}
    )
    public boolean syncDataWithBestEffort(Long dataId, Object data) {
        // 执行主数据更新
        boolean masterUpdated = updateMasterData(dataId, data);
        if (!masterUpdated) {
            return false;
        }

        // 触发异步同步到从库和缓存
        boolean syncTriggered = triggerAsyncSync(dataId, data);
        return syncTriggered;
    }

    // 模拟业务方法
    private boolean createOrder(Long orderId, Long userId, Double amount) {
        System.out.println("Creating order: " + orderId + " for user: " + userId + ", amount: " + amount);
        // 实际业务逻辑
        return true;
    }

    private boolean processPayment(Long userId, Double amount) {
        System.out.println("Processing payment for user: " + userId + ", amount: " + amount);
        // 实际业务逻辑
        return true;
    }

    private boolean deductInventory(Long orderId) {
        System.out.println("Deducting inventory for order: " + orderId);
        // 实际业务逻辑
        return true;
    }

    private boolean createUser(Long customerId, String email, String name) {
        System.out.println("Creating user: " + name + " with email: " + email);
        // 实际业务逻辑
        return true;
    }

    private boolean createProfile(Long customerId, String name) {
        System.out.println("Creating profile for customer: " + customerId + " with name: " + name);
        // 实际业务逻辑
        return true;
    }

    private boolean sendWelcomeNotification(Long customerId, String email) {
        System.out.println("Sending welcome notification to: " + email);
        // 实际业务逻辑
        return true;
    }

    private boolean updateMasterData(Long dataId, Object data) {
        System.out.println("Updating master data: " + dataId);
        // 实际业务逻辑
        return true;
    }

    private boolean triggerAsyncSync(Long dataId, Object data) {
        System.out.println("Triggering async sync for data: " + dataId);
        // 实际业务逻辑
        return true;
    }
}