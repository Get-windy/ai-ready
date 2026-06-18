package cn.aiedge.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁实现
 * 
 * 基于Redis的SET NX EX命令实现分布式锁
 * 支持可重入、锁续期等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class RedisDistributedLock implements DistributedLock {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final String lockKey;
    private final String requestId;
    private final long leaseTime;
    
    // 锁续期相关的属性
    private volatile boolean locked = false;
    private volatile Thread lockHolder = null;
    private volatile String lockValue = null;
    private volatile boolean renewActive = false;
    
    // 默认租约时间（毫秒）
    public static final long DEFAULT_LEASE_TIME = 30000;
    
    // 释放锁的Lua脚本
    private static final String RELEASE_LOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) " +
        "else return 0 end";
    
    // 续期锁的Lua脚本
    private static final String RENEW_LOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('expire', KEYS[1], ARGV[2]) " +
        "else return 0 end";

    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate, String lockKey) {
        this(redisTemplate, lockKey, DEFAULT_LEASE_TIME);
    }
    
    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate, String lockKey, long leaseTime) {
        this.redisTemplate = redisTemplate;
        this.lockKey = lockKey;
        this.leaseTime = leaseTime;
        this.requestId = UUID.randomUUID().toString();
    }
    
    @Override
    public void lock() {
        lock(leaseTime, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean lock(long time, TimeUnit unit) {
        return tryLock(time, leaseTime, unit);
    }
    
    @Override
    public boolean tryLock() {
        return tryLock(0, leaseTime, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) {
        return tryLock(waitTime, leaseTime, unit);
    }
    
    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
        long waitMillis = unit.toMillis(waitTime);
        long leaseMillis = unit.toMillis(leaseTime);
        long startTime = System.currentTimeMillis();
        
        // 如果waitTime为0，则只尝试一次
        if (waitTime == 0) {
            return acquireLock(leaseMillis);
        }
        
        // 循环尝试获取锁
        while (true) {
            if (acquireLock(leaseMillis)) {
                startRenewalTask(); // 开始续期任务
                return true;
            }
            
            // 检查是否超过等待时间
            if (System.currentTimeMillis() - startTime >= waitMillis) {
                return false;
            }
            
            // 等待一段时间再尝试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
    
    @Override
    public void unlock() {
        if (!locked || !Thread.currentThread().equals(lockHolder)) {
            throw new IllegalMonitorStateException("Current thread does not hold the lock");
        }
        
        // 停止续期任务
        stopRenewalTask();
        
        // 释放锁
        RedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
        Long result = (Long) redisTemplate.execute(script, Arrays.asList(lockKey), lockValue);
        
        if (result != null && result == 1) {
            log.debug("Successfully released lock: {}", lockKey);
            locked = false;
            lockHolder = null;
            lockValue = null;
        } else {
            log.warn("Failed to release lock: {} (lock may have expired)", lockKey);
        }
    }
    
    /**
     * 获取锁（可重入，带租约时间）
     * 使用 acquireLock 实现阻塞获取
     * 
     * @param leaseTime 锁的租约时间
     * @param unit 时间单位
     */
    public void lockWithLease(long leaseTime, TimeUnit unit) {
        long leaseMillis = unit.toMillis(leaseTime);
        while (!acquireLock(leaseMillis)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        startRenewalTask(); // 开始续期任务
    }
    
    /**
     * 尝试获取锁
     * 
     * @param leaseTime 锁的租约时间（毫秒）
     * @return 是否成功获取锁
     */
    private boolean acquireLock(long leaseTime) {
        String value = requestId + ":" + Thread.currentThread().getId();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, value, leaseTime, TimeUnit.MILLISECONDS);
        
        if (Boolean.TRUE.equals(result)) {
            locked = true;
            lockHolder = Thread.currentThread();
            lockValue = value;
            log.debug("Successfully acquired lock: {}", lockKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * 开始锁续期任务
     */
    private void startRenewalTask() {
        if (renewActive) {
            return; // 续期任务已经在运行
        }
        
        renewActive = true;
        Thread renewalThread = new Thread(() -> {
            while (renewActive && locked) {
                try {
                    // 在锁过期前的一半时间内续期
                    Thread.sleep(leaseTime / 2);
                    
                    if (locked && Thread.currentThread().equals(lockHolder)) {
                        // 续期锁
                        RedisScript<Long> script = new DefaultRedisScript<>(RENEW_LOCK_SCRIPT, Long.class);
                        Long result = (Long) redisTemplate.execute(script, 
                            Arrays.asList(lockKey), lockValue, String.valueOf(leaseTime / 1000));
                        
                        if (result != null && result == 1) {
                            log.debug("Successfully renewed lock: {}", lockKey);
                        } else {
                            log.warn("Failed to renew lock: {} (lock may have been released by another process)", lockKey);
                            // 如果续期失败，停止续期任务
                            renewActive = false;
                        }
                    } else {
                        // 当前线程不再持有锁，停止续期
                        renewActive = false;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error occurred during lock renewal: ", e);
                }
            }
        }, "Lock-Renewal-" + lockKey);
        
        renewalThread.setDaemon(true);
        renewalThread.start();
    }
    
    /**
     * 停止锁续期任务
     */
    private void stopRenewalTask() {
        renewActive = false;
    }
    
    /**
     * 检查当前线程是否持有锁
     * 
     * @return 当前线程是否持有锁
     */
    public boolean isHeldByCurrentThread() {
        return locked && Thread.currentThread().equals(lockHolder);
    }
    
    /**
     * 获取锁的持有者线程
     * 
     * @return 锁的持有者线程，如果没有线程持有则返回null
     */
    public Thread getLockHolder() {
        return lockHolder;
    }
    
    /**
     * 检查锁是否已被持有
     * 
     * @return 锁是否已被持有
     */
    public boolean isLocked() {
        return locked;
    }
}

class IllegalMonitorStateException extends RuntimeException {
    public IllegalMonitorStateException(String message) {
        super(message);
    }
}