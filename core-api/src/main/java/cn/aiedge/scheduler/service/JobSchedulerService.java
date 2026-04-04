package cn.aiedge.scheduler.service;

import cn.aiedge.scheduler.mapper.JobConfigMapper;
import cn.aiedge.scheduler.mapper.JobLogMapper;
import cn.aiedge.scheduler.model.JobConfig;
import cn.aiedge.scheduler.model.JobLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 定时任务调度服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService extends ServiceImpl<JobConfigMapper, JobConfig> {

    private final Scheduler scheduler;
    private final JobLogMapper jobLogMapper;

    private static final String JOB_PARAM_KEY = "JOB_PARAM_KEY";

    /**
     * 创建定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createJob(JobConfig jobConfig) throws SchedulerException {
        // 保存任务配置
        save(jobConfig);
        
        // 创建Quartz任务
        JobDetail jobDetail = JobBuilder.newJob(QuartzJobExecution.class)
            .withIdentity(jobConfig.getJobName(), jobConfig.getJobGroup())
            .usingJobData(JOB_PARAM_KEY, jobConfig.getId())
            .storeDurably()
            .build();
        
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
            .cronSchedule(jobConfig.getCronExpression())
            .withMisfireHandlingPolicyDoNothing();
        
        CronTrigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(jobConfig.getJobName(), jobConfig.getJobGroup())
            .withSchedule(scheduleBuilder)
            .build();
        
        scheduler.scheduleJob(jobDetail, trigger);
        
        // 如果任务状态为暂停，则暂停任务
        if (jobConfig.getStatus() == 0) {
            scheduler.pauseJob(JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup()));
        }
        
        return true;
    }

    /**
     * 更新定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJob(JobConfig jobConfig) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup());
        
        if (scheduler.checkExists(jobKey)) {
            // 更新Cron表达式
            TriggerKey triggerKey = TriggerKey.triggerKey(jobConfig.getJobName(), jobConfig.getJobGroup());
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule(jobConfig.getCronExpression())
                .withMisfireHandlingPolicyDoNothing();
            
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            trigger = trigger.getTriggerBuilder()
                .withIdentity(triggerKey)
                .withSchedule(scheduleBuilder)
                .build();
            
            scheduler.rescheduleJob(triggerKey, trigger);
        }
        
        return updateById(jobConfig);
    }

    /**
     * 删除定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteJob(Long jobId) throws SchedulerException {
        JobConfig jobConfig = getById(jobId);
        if (jobConfig == null) {
            return false;
        }
        
        JobKey jobKey = JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup());
        scheduler.deleteJob(jobKey);
        
        return removeById(jobId);
    }

    /**
     * 暂停任务
     */
    public void pauseJob(Long jobId) throws SchedulerException {
        JobConfig jobConfig = getById(jobId);
        if (jobConfig != null) {
            scheduler.pauseJob(JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup()));
            jobConfig.setStatus(0);
            updateById(jobConfig);
        }
    }

    /**
     * 恢复任务
     */
    public void resumeJob(Long jobId) throws SchedulerException {
        JobConfig jobConfig = getById(jobId);
        if (jobConfig != null) {
            scheduler.resumeJob(JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup()));
            jobConfig.setStatus(1);
            updateById(jobConfig);
        }
    }

    /**
     * 立即执行一次
     */
    public void runOnce(Long jobId) throws SchedulerException {
        JobConfig jobConfig = getById(jobId);
        if (jobConfig != null) {
            scheduler.triggerJob(JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup()));
        }
    }

    /**
     * 获取所有任务
     */
    public List<JobConfig> listAll() {
        return list();
    }

    /**
     * 分页查询任务
     */
    public Page<JobConfig> pageList(int page, int pageSize, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<JobConfig> wrapper = new LambdaQueryWrapper<>();
        if (jobName != null && !jobName.isEmpty()) {
            wrapper.like(JobConfig::getJobName, jobName);
        }
        if (jobGroup != null && !jobGroup.isEmpty()) {
            wrapper.eq(JobConfig::getJobGroup, jobGroup);
        }
        if (status != null) {
            wrapper.eq(JobConfig::getStatus, status);
        }
        return page(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 记录任务执行日志
     */
    public void logJobExecution(JobConfig jobConfig, boolean success, String message, String exception, 
                                LocalDateTime startTime, LocalDateTime endTime) {
        JobLog jobLog = new JobLog();
        jobLog.setJobId(jobConfig.getId());
        jobLog.setJobName(jobConfig.getJobName());
        jobLog.setJobGroup(jobConfig.getJobGroup());
        jobLog.setBeanName(jobConfig.getBeanName());
        jobLog.setMethodName(jobConfig.getMethodName());
        jobLog.setParams(jobConfig.getParams());
        jobLog.setStatus(success ? 1 : 0);
        jobLog.setMessage(message);
        jobLog.setException(exception);
        jobLog.setStartTime(startTime);
        jobLog.setEndTime(endTime);
        jobLog.setDuration(java.time.Duration.between(startTime, endTime).toMillis());
        
        try {
            jobLog.setServerIp(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            jobLog.setServerIp("unknown");
        }
        
        jobLogMapper.insert(jobLog);
    }

    /**
     * 分页查询执行日志
     */
    public Page<JobLog> pageLogList(int page, int pageSize, Long jobId, Integer status) {
        LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(JobLog::getJobId, jobId);
        }
        if (status != null) {
            wrapper.eq(JobLog::getStatus, status);
        }
        wrapper.orderByDesc(JobLog::getStartTime);
        return jobLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 清理指定天数前的日志
     */
    public int cleanLogs(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return jobLogMapper.delete(new LambdaQueryWrapper<JobLog>()
            .lt(JobLog::getCreateTime, threshold));
    }

    /**
     * Quartz任务执行类
     */
    public class QuartzJobExecution implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Long jobId = context.getJobDetail().getJobDataMap().getLong(JOB_PARAM_KEY);
            JobConfig jobConfig = getById(jobId);
            
            if (jobConfig == null) {
                return;
            }
            
            LocalDateTime startTime = LocalDateTime.now();
            boolean success = false;
            String message = "";
            String exception = "";
            
            try {
                // 反射执行任务
                Object bean = SpringContextHolder.getBean(jobConfig.getBeanName());
                Method method = bean.getClass().getDeclaredMethod(jobConfig.getMethodName(), String.class);
                method.invoke(bean, jobConfig.getParams());
                success = true;
                message = "执行成功";
            } catch (Exception e) {
                exception = e.getMessage();
                message = "执行失败";
                log.error("定时任务执行失败: {}", jobConfig.getJobName(), e);
            } finally {
                LocalDateTime endTime = LocalDateTime.now();
                logJobExecution(jobConfig, success, message, exception, startTime, endTime);
            }
        }
    }

    /**
     * Spring上下文持有者
     */
    private static class SpringContextHolder {
        private static org.springframework.context.ApplicationContext applicationContext;
        
        public static void setApplicationContext(org.springframework.context.ApplicationContext ctx) {
            applicationContext = ctx;
        }
        
        public static Object getBean(String name) {
            return applicationContext.getBean(name);
        }
    }
}
