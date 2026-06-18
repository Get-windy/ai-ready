package cn.aiedge.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * RedLock算法实现 - Redis分布式锁的高可用方案
 * 
 * RedLock是一种基于多个独立Redis实例的分布式锁算法，
 * 通过多数派机制提高锁的安全性和可靠性。
 * 
 * 算法原理：
 * 1. 获取当前时间戳
 * 2. 依次向N个Redis节点请求加锁
 * 3. 如果在大多数节点上成功获取锁，并且总耗时小于锁有效期，则认为获取锁成功
 * 4. 锁的实际有效期 = 原有效期 - 总耗时
 * 5. 如果获取锁失败，向所有节点发送解锁请求
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class RedLock implements DistributedLock {

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_RETRY_DELAY = 200; // ms
    private static final int CLOCK_DRIFT_FACTOR = 2; // 时钟漂移因子
    
    private final List<RedisTemplate<String, Object>> redisTemplates;
    private final String lockKey;
    private final String requestId;
    private final long leaseTime;
    private final int quorum; // 成功所需的最小节点数
    
    private volatile boolean locked = false;
    private volatile Thread lockHolder = null;
    private volatile List<String> lockValues = new ArrayList<>(); // 每个节点上的锁值
    private volatile boolean renewActive = false;
    
    public RedLock(List<RedisTemplate<String, Object>> redisTemplates, String lockKey, long leaseTime) {
        this.redisTemplates = redisTemplates;
        this.lockKey = lockKey;
        this.leaseTime = leaseTime;
        this.requestId = UUID.randomUUID().toString();
        // 计算多数派数量 (N/2 + 1)
        this.quorum = redisTemplates.size() / 2 + 1;
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
        long start = System.currentTimeMillis();
        
        int attempt = 0;
        do {
            long currentTime = System.currentTimeMillis();
            int lockedCount = 0;
            List<String> currentLockValues = new ArrayList<>();
            
            // 尝试在所有Redis节点上获取锁
            for (int i = 0; i < redisTemplates.size(); i++) {
                RedisTemplate<String, Object> template = redisTemplates.get(i);
                
                String value = requestId + ":" + Thread.currentThread().getId() + ":" + attempt;
                Boolean result = template.opsForValue().setIfAbsent(lockKey, value, leaseMillis, TimeUnit.MILLISECONDS);
                
                if (Boolean.TRUE.equals(result)) {
                    lockedCount++;
                    currentLockValues.add(value);
                    log.debug("Successfully acquired lock on Redis instance {}: {}", i, lockKey);
                } else {
                    log.debug("Failed to acquire lock on Redis instance {}: {}", i, lockKey);
                    currentLockValues.add(null);
                }
            }
            
            // 检查是否在大多数节点上成功获取锁
            if (lockedCount >= quorum) {
                // 检查总的耗时是否超过了锁的有效期
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed < leaseMillis) {
                    // 成功获取分布式锁
                    this.locked = true;
                    this.lockHolder = Thread.currentThread();
                    this.lockValues = currentLockValues;
                    startRenewalTask();
                    log.info("Successfully acquired distributed lock: {} on {} nodes", lockKey, lockedCount);
                    return true;
                } else {
                    // 耗时过长，锁已经过期，需要释放已获得的锁
                    log.warn("Lock acquisition took too long: {}ms, releasing acquired locks", elapsed);
                    unlock();
                    return false;
                }
            } else {
                // 在大多数节点上获取锁失败，释放已获得的锁
                log.debug("Failed to acquire lock on majority of nodes, releasing partial locks");
                releasePartialLocks(currentLockValues);
            }
            
            // 等待一段随机时间后重试（避免活锁）
            try {
                long drift = Math.max(DEFAULT_RETRY_DELAY, leaseMillis / CLOCK_DRIFT_FACTOR);
                long randomDelay = ThreadLocalRandom.current().nextLong(drift);
                Thread.sleep(randomDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            attempt++;
        } while ((System.currentTimeMillis() - start) < waitMillis);
        
        return false;
    }
    
    @Override
    public void unlock() {
        if (!locked || !Thread.currentThread().equals(lockHolder)) {
            log.warn("Current thread does not hold the lock: {}", lockKey);
            return;
        }
        
        // 停止续期任务
        stopRenewalTask();
        
        // 向所有Redis节点发送解锁请求
        int unlockedCount = 0;
        for (int i = 0; i < redisTemplates.size(); i++) {
            RedisTemplate<String, Object> template = redisTemplates.get(i);
            String lockValue = lockValues.get(i);
            
            if (lockValue != null) {
                // 使用Lua脚本安全地释放锁
                String luaScript = 
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";
                
                try {
                    org.springframework.data.redis.core.script.RedisScript<Long> script = 
                        new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class);
                    Long result = template.execute(script, Collections.singletonList(lockKey), lockValue);
                    
                    if (result != null && Long.parseLong(result.toString()) == 1) {
                        unlockedCount++;
                        log.debug("Successfully released lock on Redis instance {}: {}", i, lockKey);
                    } else {
                        log.warn("Failed to release lock on Redis instance {}: {}", i, lockKey);
                    }
                } catch (Exception e) {
                    log.error("Error releasing lock on Redis instance " + i + ": " + lockKey, e);
                }
            }
        }
        
        log.info("Released distributed lock: {} on {} nodes", lockKey, unlockedCount);
        locked = false;
        lockHolder = null;
        lockValues.clear();
    }
    
    /**
     * 获取锁（可重入，带租约时间）
     * 使用 tryLock 实现阻塞获取
     * 
     * @param leaseTime 锁的租约时间
     * @param unit 时间单位
     */
    public void lockWithLease(long leaseTime, TimeUnit unit) {
        long leaseMillis = unit.toMillis(leaseTime);
        long start = System.currentTimeMillis();
        
        // 重试直到成功获取锁
        while (true) {
            if (tryLock(leaseMillis, leaseMillis, TimeUnit.MILLISECONDS)) {
                return;
            }
            
            // 检查是否应该继续等待
            if (System.currentTimeMillis() - start >= leaseMillis) {
                // 超时仍未获取到锁，抛出异常
                throw new RuntimeException("Failed to acquire lock within lease time: " + lockKey);
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for lock: " + lockKey);
            }
        }
    }
    
    /**
     * 释放部分已获取的锁
     * 
     * @param lockValues 锁值列表
     */
    private void releasePartialLocks(List<String> lockValues) {
        for (int i = 0; i < redisTemplates.size(); i++) {
            RedisTemplate<String, Object> template = redisTemplates.get(i);
            String lockValue = lockValues.get(i);
            
            if (lockValue != null) {
                // 使用Lua脚本安全地释放锁
                String luaScript = 
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";
                
                try {
                    org.springframework.data.redis.core.script.RedisScript<Long> script = 
                        new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class);
                    template.execute(script, Collections.singletonList(lockKey), lockValue);
                } catch (Exception e) {
                    log.error("Error releasing partial lock on Redis instance " + i + ": " + lockKey, e);
                }
            }
        }
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
                        // 续期所有节点上的锁
                        int renewedCount = 0;
                        for (int i = 0; i < redisTemplates.size(); i++) {
                            RedisTemplate<String, Object> template = redisTemplates.get(i);
                            String lockValue = lockValues.get(i);
                            
                            if (lockValue != null) {
                                // 使用Lua脚本安全地续期锁
                                String luaScript = 
                                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                                    "return redis.call('expire', KEYS[1], ARGV[2]) " +
                                    "else return 0 end";
                                
                                try {
                                    org.springframework.data.redis.core.script.RedisScript<Long> script = 
                                        new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class);
                                    Long result = template.execute(script, 
                                        Collections.singletonList(lockKey), 
                                        lockValue, String.valueOf(leaseTime / 1000));
                                    
                                    if (result != null && result == 1) {
                                        renewedCount++;
                                    }
                                } catch (Exception e) {
                                    log.error("Error renewing lock on Redis instance " + i + ": " + lockKey, e);
                                }
                            }
                        }
                        
                        if (renewedCount >= quorum) {
                            log.debug("Successfully renewed distributed lock: {} on {} nodes", lockKey, renewedCount);
                        } else {
                            log.warn("Failed to renew distributed lock: {} (not enough nodes renewed)", lockKey);
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
        }, "RedLock-Renewal-" + lockKey);
        
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
     * 检查锁是否已被持有
     * 
     * @return 锁是否已被持有
     */
    public boolean isLocked() {
        return locked;
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
     * 获取锁的多数派数量
     * 
     * @return 锁的多数派数量
     */
    public int getQuorum() {
        return quorum;
    }
}