package cn.aiedge.scheduler.controller;

import cn.aiedge.scheduler.model.JobConfig;
import cn.aiedge.scheduler.model.JobLog;
import cn.aiedge.scheduler.service.JobSchedulerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "定时任务管理", description = "定时任务的增删改查、执行控制、日志查询")
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class JobSchedulerController {

    private final JobSchedulerService jobSchedulerService;

    @Operation(summary = "创建定时任务")
    @PostMapping
    public JobConfig create(@RequestBody JobConfig jobConfig) throws SchedulerException {
        jobSchedulerService.createJob(jobConfig);
        return jobConfig;
    }

    @Operation(summary = "更新定时任务")
    @PutMapping
    public JobConfig update(@RequestBody JobConfig jobConfig) throws SchedulerException {
        jobSchedulerService.updateJob(jobConfig);
        return jobConfig;
    }

    @Operation(summary = "删除定时任务")
    @DeleteMapping("/{jobId}")
    public Boolean delete(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        try {
            return jobSchedulerService.deleteJob(jobId);
        } catch (SchedulerException e) {
            return false;
        }
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{jobId}")
    public JobConfig getInfo(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        return jobSchedulerService.getById(jobId);
    }

    @Operation(summary = "获取所有任务")
    @GetMapping("/list")
    public List<JobConfig> listAll() {
        return jobSchedulerService.listAll();
    }

    @Operation(summary = "分页查询任务")
    @GetMapping("/page")
    public Page<JobConfig> pageList(
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "任务名称") @RequestParam(required = false) String jobName,
        @Parameter(description = "任务分组") @RequestParam(required = false) String jobGroup,
        @Parameter(description = "状态") @RequestParam(required = false) Integer status
    ) {
        return jobSchedulerService.pageList(page, pageSize, jobName, jobGroup, status);
    }

    @Operation(summary = "暂停任务")
    @PutMapping("/pause/{jobId}")
    public Boolean pause(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        try {
            jobSchedulerService.pauseJob(jobId);
            return true;
        } catch (SchedulerException e) {
            return false;
        }
    }

    @Operation(summary = "恢复任务")
    @PutMapping("/resume/{jobId}")
    public Boolean resume(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        try {
            jobSchedulerService.resumeJob(jobId);
            return true;
        } catch (SchedulerException e) {
            return false;
        }
    }

    @Operation(summary = "立即执行一次")
    @PostMapping("/run/{jobId}")
    public Boolean runOnce(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        try {
            jobSchedulerService.runOnce(jobId);
            return true;
        } catch (SchedulerException e) {
            return false;
        }
    }

    @Operation(summary = "分页查询执行日志")
    @GetMapping("/log/page")
    public Page<JobLog> pageLogList(
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "任务ID") @RequestParam(required = false) Long jobId,
        @Parameter(description = "状态") @RequestParam(required = false) Integer status
    ) {
        return jobSchedulerService.pageLogList(page, pageSize, jobId, status);
    }

    @Operation(summary = "清理日志")
    @DeleteMapping("/log/clean")
    public Integer cleanLogs(@Parameter(description = "保留天数") @RequestParam(defaultValue = "30") int days) {
        return jobSchedulerService.cleanLogs(days);
    }

    @Operation(summary = "获取任务统计")
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        List<JobConfig> jobs = jobSchedulerService.listAll();
        
        stats.put("total", jobs.size());
        stats.put("running", jobs.stream().filter(j -> j.getStatus() == 1).count());
        stats.put("paused", jobs.stream().filter(j -> j.getStatus() == 0).count());
        
        return stats;
    }
}
