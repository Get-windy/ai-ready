package cn.aiedge.base.util;

import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务调度工具类
 * 提供通用的任务调度相关工具方法
 */
public class JobSchedulingUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JobSchedulingUtil.class);

    /**
     * 验证cron表达式格式是否正确
     * 
     * @param cronExpression cron表达式
     * @return 是否有效
     */
    public static boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        
        // 简单的格式验证（实际应用中可以使用cron-utils等库进行更严格的验证）
        String[] parts = cronExpression.trim().split("\\s+");
        return parts.length >= 6 && parts.length <= 7; // 支持秒、分、时、日、月、周、年（年可选）
    }

    /**
     * 格式化任务执行时间显示
     * 
     * @param milliseconds 毫秒数
     * @return 格式化的时间字符串
     */
    public static String formatExecutionTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60 * 1000) {
            return String.format("%.2f", milliseconds / 1000.0) + "s";
        } else {
            return String.format("%.2f", milliseconds / (60.0 * 1000)) + "min";
        }
    }

    /**
     * 获取任务执行结果的简要描述
     * 
     * @param success 是否成功
     * @param message 结果消息
     * @return 简要描述
     */
    public static String getResultDescription(boolean success, String message) {
        if (success) {
            return "SUCCESS: " + message;
        } else {
            return "FAILED: " + message;
        }
    }

    /**
     * 执行任务的安全包装器
     * 确保任务执行过程中发生的异常被捕获和处理
     * 
     * @param jobHandler 任务处理器
     * @param jobName 任务名称
     * @return 执行结果
     */
    public static String executeJobSafely(IJobHandler jobHandler, String jobName) {
        long startTime = System.currentTimeMillis();
        String result;
        
        try {
            logger.info("开始执行任务: {}", jobName);
            jobHandler.execute();
            long executionTime = System.currentTimeMillis() - startTime;
            
            result = getResultDescription(true, 
                String.format("任务执行成功，耗时: %s", formatExecutionTime(executionTime)));
            
            logger.info("任务执行完成: {}, {}", jobName, result);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            result = getResultDescription(false, 
                String.format("任务执行失败: %s，耗时: %s", e.getMessage(), formatExecutionTime(executionTime)));
            
            logger.error("任务执行失败: " + jobName, e);
        }
        
        return result;
    }

    /**
     * 验证任务参数
     * 
     * @param params 任务参数
     * @return 是否有效
     */
    public static boolean validateJobParameters(String params) {
        // 可以根据需要实现参数验证逻辑
        // 这里简单检查参数长度
        if (params == null) {
            return true; // 参数可以为空
        }
        
        return params.length() <= 512; // 限制参数长度
    }
}