package cn.aiedge.dms.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 乐观锁重试工具
 * <p>
 * 当 MyBatis-Plus 的 @Version 乐观锁更新返回受影响行数为 0 时，
 * 自动重试更新操作，避免并发冲突导致数据不一致。
 * </p>
 *
 * <pre>{@code
 * RetryUtils.retryWithOptimisticLock(3, 50, () -> {
 *     DmsOrderPool pool = orderPoolMapper.selectById(id);
 *     pool.setPoolStatus(PoolStatusEnum.CLAIMED.getValue());
 *     return orderPoolMapper.updateById(pool) > 0;
 * });
 * }</pre>
 */
@Slf4j
public final class RetryUtils {

    private RetryUtils() {
    }

    /**
     * 带乐观锁重试的执行函数
     *
     * @param maxRetries  最大重试次数
     * @param baseDelayMs 基础延迟（毫秒），每次重试递增
     * @param action      更新操作，返回 true 表示更新成功
     * @return 是否最终更新成功
     * @throws InterruptedException 线程中断时抛出
     */
    public static boolean retryWithOptimisticLock(int maxRetries, long baseDelayMs,
                                                   RetryableAction action) throws InterruptedException {
        int attempt = 0;
        while (attempt <= maxRetries) {
            boolean success = action.execute();
            if (success) {
                if (attempt > 0) {
                    log.debug("乐观锁重试成功, attempts={}", attempt);
                }
                return true;
            }
            attempt++;
            if (attempt <= maxRetries) {
                long delay = baseDelayMs * (1L << (attempt - 1)); // 指数退避
                log.debug("乐观锁冲突, 第{}次重试, delay={}ms", attempt, delay);
                Thread.sleep(delay);
            }
        }
        log.warn("乐观锁重试{}次后仍然失败", maxRetries);
        return false;
    }

    /**
     * 带乐观锁重试的执行函数（无检查异常版本）
     *
     * @param maxRetries  最大重试次数
     * @param baseDelayMs 基础延迟（毫秒）
     * @param action      更新操作
     * @return 是否最终更新成功
     */
    public static boolean retryWithOptimisticLockSafe(int maxRetries, long baseDelayMs,
                                                       RetryableAction action) {
        try {
            return retryWithOptimisticLock(maxRetries, baseDelayMs, action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("乐观锁重试被中断", e);
            return false;
        }
    }

    /**
     * 简单 3 次重试，50ms 初始延迟，指数退避
     */
    public static boolean retry3(RetryableAction action) {
        return retryWithOptimisticLockSafe(3, 50, action);
    }

    /**
     * 函数式接口：更新操作
     */
    @FunctionalInterface
    public interface RetryableAction {
        /**
         * 执行更新操作
         *
         * @return true 表示更新成功（受影响行数 > 0）
         */
        boolean execute();
    }
}
