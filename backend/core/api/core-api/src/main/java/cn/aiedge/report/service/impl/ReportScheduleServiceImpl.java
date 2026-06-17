package cn.aiedge.report.service.impl;

import cn.aiedge.report.mapper.ReportScheduleLogMapper;
import cn.aiedge.report.mapper.ReportScheduleMapper;
import cn.aiedge.report.model.ReportSchedule;
import cn.aiedge.report.model.ReportScheduleLog;
import cn.aiedge.report.service.ReportScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportScheduleServiceImpl implements ReportScheduleService {

    private final ReportScheduleMapper reportScheduleMapper;
    private final ReportScheduleLogMapper reportScheduleLogMapper;

    @Override
    public Long createScheduleReport(String reportId, String scheduleCron,
                                     List<String> emailRecipients, String exportFormat,
                                     Map<String, Object> parameters, Long tenantId) {
        LocalDateTime now = LocalDateTime.now();

        ReportSchedule schedule = new ReportSchedule();
        schedule.setReportId(reportId);
        schedule.setScheduleCron(scheduleCron);
        schedule.setEmailRecipients(emailRecipients != null ? String.join(",", emailRecipients) : null);
        schedule.setExportFormat(exportFormat != null ? exportFormat : "excel");
        schedule.setParameters(parameters != null ? parameters.toString() : null);
        schedule.setStatus("STOPPED");
        schedule.setEnabled(1);
        schedule.setExecuteCount(0);
        schedule.setTenantId(tenantId != null ? tenantId : 1L);
        schedule.setCreateTime(now);
        schedule.setUpdateTime(now);

        reportScheduleMapper.insert(schedule);
        log.info("创建定时报表任务: id={}, reportId={}, cron={}", schedule.getId(), reportId, scheduleCron);

        return schedule.getId();
    }

    @Override
    public boolean updateScheduleReport(Long scheduleId, String scheduleCron,
                                        List<String> emailRecipients, Boolean enabled) {
        ReportSchedule existing = reportScheduleMapper.selectById(scheduleId);
        if (existing == null) {
            log.warn("更新定时报表任务失败，任务不存在: {}", scheduleId);
            return false;
        }

        if (scheduleCron != null) existing.setScheduleCron(scheduleCron);
        if (emailRecipients != null) existing.setEmailRecipients(String.join(",", emailRecipients));
        if (enabled != null) {
            existing.setEnabled(enabled ? 1 : 0);
            existing.setStatus(enabled ? "RUNNING" : "STOPPED");
        }
        existing.setUpdateTime(LocalDateTime.now());

        reportScheduleMapper.updateById(existing);
        log.info("更新定时报表任务: id={}", scheduleId);
        return true;
    }

    @Override
    public boolean deleteScheduleReport(Long scheduleId) {
        boolean deleted = reportScheduleMapper.deleteById(scheduleId) > 0;
        if (deleted) {
            log.info("删除定时报表任务: id={}", scheduleId);
        }
        return deleted;
    }

    @Override
    public List<Map<String, Object>> getScheduleReports(String reportId, Long tenantId) {
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<ReportSchedule>()
                .eq(reportId != null && !reportId.isEmpty(), ReportSchedule::getReportId, reportId)
                .eq(tenantId != null, ReportSchedule::getTenantId, tenantId)
                .orderByDesc(ReportSchedule::getCreateTime);

        List<ReportSchedule> schedules = reportScheduleMapper.selectList(wrapper);

        return schedules.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("reportId", s.getReportId());
            map.put("reportName", s.getReportName());
            map.put("scheduleCron", s.getScheduleCron());
            map.put("emailRecipients", s.getEmailRecipients() != null
                    ? Arrays.asList(s.getEmailRecipients().split(","))
                    : Collections.emptyList());
            map.put("exportFormat", s.getExportFormat());
            map.put("status", s.getStatus());
            map.put("enabled", s.getEnabled() == 1);
            map.put("lastExecuteTime", s.getLastExecuteTime());
            map.put("nextExecuteTime", s.getNextExecuteTime());
            map.put("executeCount", s.getExecuteCount());
            map.put("createTime", s.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean triggerScheduleReport(Long scheduleId) {
        ReportSchedule schedule = reportScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("触发定时报表执行失败，任务不存在: {}", scheduleId);
            return false;
        }

        // 记录执行日志
        ReportScheduleLog logEntry = new ReportScheduleLog();
        logEntry.setScheduleId(scheduleId);
        logEntry.setReportId(schedule.getReportId());
        logEntry.setExecuteStatus("RUNNING");
        logEntry.setStartTime(LocalDateTime.now());
        reportScheduleLogMapper.insert(logEntry);

        // 更新任务执行信息
        schedule.setLastExecuteTime(LocalDateTime.now());
        schedule.setExecuteCount(schedule.getExecuteCount() != null ? schedule.getExecuteCount() + 1 : 1);
        schedule.setUpdateTime(LocalDateTime.now());
        reportScheduleMapper.updateById(schedule);

        // 更新日志为成功
        logEntry.setExecuteStatus("SUCCESS");
        logEntry.setEndTime(LocalDateTime.now());
        logEntry.setExecuteTime(java.time.Duration.between(logEntry.getStartTime(), logEntry.getEndTime()).toMillis());
        reportScheduleLogMapper.updateById(logEntry);

        log.info("触发定时报表执行: id={}, reportId={}", scheduleId, schedule.getReportId());
        return true;
    }

    @Override
    public boolean pauseScheduleReport(Long scheduleId) {
        ReportSchedule schedule = reportScheduleMapper.selectById(scheduleId);
        if (schedule == null) return false;

        schedule.setStatus("PAUSED");
        schedule.setUpdateTime(LocalDateTime.now());
        reportScheduleMapper.updateById(schedule);
        log.info("暂停定时报表任务: id={}", scheduleId);
        return true;
    }

    @Override
    public boolean resumeScheduleReport(Long scheduleId) {
        ReportSchedule schedule = reportScheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getEnabled() != 1) return false;

        schedule.setStatus("RUNNING");
        schedule.setUpdateTime(LocalDateTime.now());
        reportScheduleMapper.updateById(schedule);
        log.info("恢复定时报表任务: id={}", scheduleId);
        return true;
    }

    @Override
    public List<Map<String, Object>> getExecutionHistory(Long scheduleId, int limit) {
        LambdaQueryWrapper<ReportScheduleLog> wrapper = new LambdaQueryWrapper<ReportScheduleLog>()
                .eq(ReportScheduleLog::getScheduleId, scheduleId)
                .orderByDesc(ReportScheduleLog::getStartTime)
                .last("LIMIT " + Math.min(limit, 100));

        List<ReportScheduleLog> logs = reportScheduleLogMapper.selectList(wrapper);

        return logs.stream().map(l -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId());
            map.put("scheduleId", l.getScheduleId());
            map.put("executeStatus", l.getExecuteStatus());
            map.put("startTime", l.getStartTime());
            map.put("endTime", l.getEndTime());
            map.put("executeTime", l.getExecuteTime());
            map.put("errorMessage", l.getErrorMessage());
            return map;
        }).collect(Collectors.toList());
    }
}
