package cn.aiedge.common.lock;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁使用示例
 * 
 * 展示如何在实际业务场景中使用分布式锁
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class DistributedLockExample {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public DistributedLockExample(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 示例1: 基本的分布式锁使用
     */
    public void basicUsageExample() {
        // 创建分布式锁
        DistributedLock lock = DistributedLockFactory.createRedisLock(
            redisTemplate, "business-operation-key");
        
        try {
            // 尝试获取锁，最多等待10秒
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                System.out.println("成功获取分布式锁，开始执行业务逻辑...");
                
                // 执行需要同步的业务逻辑
                performBusinessOperation();
                
                System.out.println("业务逻辑执行完成");
            } else {
                System.out.println("获取分布式锁失败，可能存在其他实例正在处理相同业务");
            }
        } catch (Exception e) {
            System.err.println("执行业务操作时发生异常: " + e.getMessage());
        } finally {
            // 无论如何都要释放锁
            try {
                lock.unlock();
            } catch (Exception e) {
                System.err.println("释放锁时发生异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 示例2: 使用带租约时间的锁
     */
    public void leaseTimeExample() {
        // 创建带有5秒租约时间的锁
        DistributedLock lock = DistributedLockFactory.createRedisLock(
            redisTemplate, "lease-time-key", 5000); // 5秒租约
        
        try {
            // 使用带租约时间的锁获取方式
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) { // 等待10秒，租约5秒
                System.out.println("获取带租约时间的锁成功");
                
                // 执行业务操作
                performBusinessOperation();
                
                System.out.println("业务操作完成");
            } else {
                System.out.println("未能获取带租约时间的锁");
            }
        } catch (Exception e) {
            System.err.println("执行业务操作时发生异常: " + e.getMessage());
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                System.err.println("释放锁时发生异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 示例3: 在订单处理中的应用
     */
    public boolean processOrder(String orderId) {
        // 使用订单ID作为锁的键，确保同一订单不会被重复处理
        DistributedLock lock = DistributedLockFactory.createRedisLock(
            redisTemplate, "order-processing:" + orderId);
        
        try {
            // 尝试获取锁，防止重复处理
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                System.out.println("开始处理订单: " + orderId);
                
                // 检查订单状态（防止重复处理已处理的订单）
                if (isOrderProcessed(orderId)) {
                    System.out.println("订单 " + orderId + " 已被处理，跳过");
                    return false;
                }
                
                // 执行订单处理逻辑
                boolean success = executeOrderProcessing(orderId);
                
                if (success) {
                    markOrderAsProcessed(orderId);
                    System.out.println("订单 " + orderId + " 处理成功");
                } else {
                    System.out.println("订单 " + orderId + " 处理失败");
                }
                
                return success;
            } else {
                System.out.println("无法获取订单处理锁，订单 " + orderId + " 可能正在被其他实例处理");
                return false;
            }
        } catch (Exception e) {
            System.err.println("处理订单时发生异常: " + e.getMessage());
            return false;
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                System.err.println("释放订单锁时发生异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 示例4: 库存扣减中的应用
     */
    public boolean deductStock(String productId, int quantity) {
        // 使用产品ID作为锁的键，确保库存扣减的原子性
        DistributedLock lock = DistributedLockFactory.createRedisLock(
            redisTemplate, "stock-deduct:" + productId);
        
        try {
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                System.out.println("开始扣减产品 " + productId + " 的库存，数量: " + quantity);
                
                // 检查库存是否足够
                int currentStock = getCurrentStock(productId);
                if (currentStock < quantity) {
                    System.out.println("库存不足，当前库存: " + currentStock + "，需要: " + quantity);
                    return false;
                }
                
                // 扣减库存
                boolean success = updateStock(productId, currentStock - quantity);
                
                if (success) {
                    System.out.println("库存扣减成功，产品: " + productId + 
                                     "，扣减数量: " + quantity + 
                                     "，剩余库存: " + (currentStock - quantity));
                } else {
                    System.out.println("库存扣减失败");
                }
                
                return success;
            } else {
                System.out.println("无法获取库存扣减锁，产品 " + productId + " 的库存可能正在被其他操作修改");
                return false;
            }
        } catch (Exception e) {
            System.err.println("扣减库存时发生异常: " + e.getMessage());
            return false;
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                System.err.println("释放库存锁时发生异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 模拟执行业务操作
     */
    private void performBusinessOperation() {
        try {
            // 模拟一些业务处理时间
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 检查订单是否已处理
     */
    private boolean isOrderProcessed(String orderId) {
        // 实际实现中会查询数据库或缓存
        System.out.println("检查订单 " + orderId + " 的处理状态");
        return false; // 假设订单未处理
    }
    
    /**
     * 执行订单处理逻辑
     */
    private boolean executeOrderProcessing(String orderId) {
        // 实际实现中会执行具体的订单处理逻辑
        System.out.println("执行订单 " + orderId + " 的处理逻辑");
        try {
            Thread.sleep(2000); // 模拟处理时间
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 标记订单为已处理
     */
    private void markOrderAsProcessed(String orderId) {
        // 实际实现中会更新数据库或缓存
        System.out.println("标记订单 " + orderId + " 为已处理");
    }
    
    /**
     * 获取当前库存
     */
    private int getCurrentStock(String productId) {
        // 实际实现中会查询数据库或缓存
        System.out.println("查询产品 " + productId + " 的当前库存");
        return 100; // 假设当前库存为100
    }
    
    /**
     * 更新库存
     */
    private boolean updateStock(String productId, int newStock) {
        // 实际实现中会更新数据库
        System.out.println("更新产品 " + productId + " 的库存为: " + newStock);
        return true;
    }
}