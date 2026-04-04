package cn.aiedge.scheduler.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Quartz任务包装类
 * 将ScheduledJob适配到Quartz Job
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class QuartzJobWrapper implements Job {

    public static final String TASK_ID_KEY = "taskId";
    public static final String TASK_NAME_KEY = "taskName";
    public static final String TASK_CLASS_KEY = "taskClass";
    public static final String TASK_PARAMS_KEY = "taskParams";

    @Override
    @SuppressWarnings("unchecked")
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        Long taskId = dataMap.getLong(TASK_ID_KEY);
        String taskName = dataMap.getString(TASK_NAME_KEY);
        String taskClass = dataMap.getString(TASK_CLASS_KEY);
        String paramsJson = dataMap.getString(TASK_PARAMS_KEY);

        log.info("开始执行定时任务: id={}, name={}, class={}", taskId, taskName, taskClass);

        try {
            // 加载并实例化任务类
            Class<?> clazz = Class.forName(taskClass);
            if (!ScheduledJob.class.isAssignableFrom(clazz)) {
                throw new JobExecutionException("任务类未实现ScheduledJob接口: " + taskClass);
            }

            ScheduledJob job = (ScheduledJob) clazz.getDeclaredConstructor().newInstance();

            // 解析参数
            Map<String, Object> params = parseParams(paramsJson);

            // 执行前钩子
            job.beforeExecute(params);

            // 执行任务
            String result;
            Exception exception = null;
            try {
                result = job.execute(params);
                log.info("定时任务执行完成: id={}, name={}, result={}", taskId, taskName, result);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                // 执行后钩子
                job.afterExecute(params, result, exception);
            }

        } catch (ClassNotFoundException e) {
            log.error("任务类不存在: {}", taskClass, e);
            throw new JobExecutionException("任务类不存在: " + taskClass, e);
        } catch (Exception e) {
            log.error("定时任务执行失败: id={}, name={}", taskId, taskName, e);
            throw new JobExecutionException("任务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析任务参数
     */
    private Map<String, Object> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isEmpty()) {
            return Map.of();
        }
        try {
            // 简单JSON解析，实际应使用Jackson
            return cn.hutool.json.JSONUtil.toBean(paramsJson, Map.class);
        } catch (Exception e) {
            log.warn("解析任务参数失败: {}", paramsJson, e);
            return Map.of();
        }
    }
}
