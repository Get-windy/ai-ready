package cn.aiedge.erp.price.engine.execution.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 策略执行请求DTO
 */
public record ExecutionRequest(
        String executionId,
        String strategyId,
        ExecutionType executionType,
        Map<String, Object> context,
        List<ExecutionTarget> targets,
        ExecutionSchedule schedule,
        ExecutionPriority priority,
        Map<String, String> parameters,
        String requesterId,
        LocalDateTime requestTime
) {
    public enum ExecutionType {
        IMMEDIATE,          // 立即执行
        SCHEDULED,          // 定时执行
        CONDITIONAL,        // 条件触发
        RECURRING,          // 循环执行
        BATCH               // 批量执行
    }
    
    public enum ExecutionPriority {
        CRITICAL,           // 关键
        HIGH,               // 高
        MEDIUM,             // 中
        LOW                 // 低
    }
}

/**
 * 执行目标
 */
record ExecutionTarget(
        String targetId,
        TargetType targetType,
        String referenceId,
        Map<String, Object> targetData,
        List<ExecutionAction> actions
) {
    public enum TargetType {
        PRODUCT,            // 产品
        CUSTOMER,           // 客户
        ORDER,              // 订单
        CATEGORY,           // 品类
        REGION,             // 区域
        CHANNEL             // 渠道
    }
}

/**
 * 执行动作
 */
record ExecutionAction(
        String actionId,
        ActionType actionType,
        Map<String, Object> parameters,
        BigDecimal expectedImpact,
        ActionPriority priority,
        List<Dependency> dependencies
) {
    public enum ActionType {
        PRICE_UPDATE,           // 价格更新
        DISCOUNT_APPLY,         // 应用折扣
        PROMOTION_START,        // 开始促销
        PROMOTION_END,          // 结束促销
        PRICE_LOCK,             // 价格锁定
        PRICE_UNLOCK,           // 价格解锁
        NOTIFICATION_SEND       // 发送通知
    }
    
    public enum ActionPriority {
        MUST_EXECUTE,           // 必须执行
        SHOULD_EXECUTE,         // 应该执行
        COULD_EXECUTE,          // 可以执行
        WONT_EXECUTE            // 不会执行
    }
}

/**
 * 依赖关系
 */
record Dependency(
        String dependencyId,
        DependencyType dependencyType,
        String targetActionId,
        DependencyCondition condition
) {
    public enum DependencyType {
        PRECEDES,               // 前置
        SUCCEEDS,               // 后置
        PARALLEL,               // 并行
        EXCLUSIVE               // 互斥
    }
}

/**
 * 依赖条件
 */
record DependencyCondition(
        String conditionExpression,
        ConditionOperator operator,
        Object expectedValue,
        boolean mustSatisfy
) {
    public enum ConditionOperator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        EXISTS,
        NOT_EXISTS
    }
}

/**
 * 执行计划
 */
record ExecutionSchedule(
        LocalDateTime startTime,
        LocalDateTime endTime,
        ScheduleType scheduleType,
        String cronExpression,
        Integer repeatCount,
        Long repeatIntervalSeconds,
        List<DayOfWeek> daysOfWeek,
        List<Integer> hoursOfDay
) {
    public enum ScheduleType {
        ONCE,                   // 一次
        DAILY,                  // 每日
        WEEKLY,                 // 每周
        MONTHLY,                // 每月
        YEARLY,                 // 每年
        CUSTOM                  // 自定义
    }
    
    public enum DayOfWeek {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }
}