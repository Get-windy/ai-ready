package cn.aiedge.scheduler.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 示例定时任务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class SampleJob implements ScheduledJob {

    @Override
    public String execute(Map<String, Object> params) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String message = "示例任务执行成功，当前时间: " + time;
        
        if (params != null && !params.isEmpty()) {
            message += "，参数: " + params;
        }
        
        log.info(message);
        return message;
    }

    @Override
    public String getTaskName() {
        return "示例任务";
    }

    @Override
    public String getDescription() {
        return "这是一个示例定时任务，用于演示任务执行流程";
    }
}
