package cn.aiedge.scheduler.job;

import java.util.Map;

/**
 * 定时任务接口
 * 所有定时任务需要实现此接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ScheduledJob {

    /**
     * 执行任务
     *
     * @param params 任务参数
     * @return 执行结果消息
     * @throws Exception 执行异常
     */
    String execute(Map<String, Object> params) throws Exception;

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    default String getTaskName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取任务描述
     *
     * @return 任务描述
     */
    default String getDescription() {
        return "";
    }

    /**
     * 任务执行前的钩子方法
     *
     * @param params 任务参数
     */
    default void beforeExecute(Map<String, Object> params) {
        // 默认空实现
    }

    /**
     * 任务执行后的钩子方法
     *
     * @param params 任务参数
     * @param result 执行结果
     * @param e      异常信息（如果有）
     */
    default void afterExecute(Map<String, Object> params, String result, Exception e) {
        // 默认空实现
    }
}
