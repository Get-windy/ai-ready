package cn.aiedge.common.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 熔断器
 */
public class CircuitBreaker {
    
    // 熔断器状态
    public enum State {
        CLOSED,      // 关闭状态（正常）
        OPEN,        // 打开状态（熔断）
        HALF_OPEN    // 半开状态（试探）
    }
    
    private final int failureThreshold;          // 失败阈值
    private final long timeout;                  // 熔断超时时间（毫秒）
    private final AtomicInteger failureCount;    // 失败计数
    private final AtomicReference<State> state;  // 当前状态
    private volatile long lastFailureTime;       // 最后一次失败时间
    
    public CircuitBreaker(int failureThreshold, long timeout) {
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.failureCount = new AtomicInteger(0);
        this.state = new AtomicReference<>(State.CLOSED);
        this.lastFailureTime = 0;
    }
    
    /**
     * 尝试执行
     */
    public boolean allowRequest() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                // 检查是否超过超时时间
                if (System.currentTimeMillis() - lastFailureTime >= timeout) {
                    // 转换为半开状态
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * 记录成功
     */
    public void recordSuccess() {
        State currentState = state.get();
        if (currentState == State.HALF_OPEN) {
            // 半开状态成功，转换为关闭状态
            state.compareAndSet(State.HALF_OPEN, State.CLOSED);
            failureCount.set(0);
        }
    }
    
    /**
     * 记录失败
     */
    public void recordFailure() {
        lastFailureTime = System.currentTimeMillis();
        int count = failureCount.incrementAndGet();
        
        State currentState = state.get();
        if (currentState == State.HALF_OPEN) {
            // 半开状态失败，立即打开熔断器
            state.compareAndSet(State.HALF_OPEN, State.OPEN);
        } else if (currentState == State.CLOSED && count >= failureThreshold) {
            // 关闭状态失败次数达到阈值，打开熔断器
            state.compareAndSet(State.CLOSED, State.OPEN);
        }
    }
    
    /**
     * 获取当前状态
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * 重置熔断器
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        lastFailureTime = 0;
    }
}
