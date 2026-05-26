package cn.aiedge.common.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DistributedLock {
    
    /**
     * 获取锁（阻塞方式）
     */
    void lock();
    
    /**
     * 获取锁（阻塞方式，带超时）
     * 
     * @param time 等待时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     */
    boolean lock(long time, TimeUnit unit);
    
    /**
     * 尝试获取锁（非阻塞）
     * 
     * @return 是否成功获取锁
     */
    boolean tryLock();
    
    /**
     * 尝试获取锁（带超时）
     * 
     * @param time 等待时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(long time, TimeUnit unit);
    
    /**
     * 释放锁
     */
    void unlock();
    
    /**
     * 尝试获取锁（可重入，带超时和租约时间）
     * 
     * @param waitTime 等待时间
     * @param leaseTime 锁的租约时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit);
}