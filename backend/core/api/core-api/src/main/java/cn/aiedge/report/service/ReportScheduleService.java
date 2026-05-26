package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import java.util.List;
import java.util.Map;

/**
 * 报表定时任务服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ReportScheduleService {

    /**
     * 创建定时报表任务
     *
     * @param reportId 报表ID
     * @param scheduleCron CRON表达式
     * @param emailRecipients 邮件接收人列表
     * @param exportFormat 导出格式 (excel/pdf/word)
     * @param parameters 参数
     * @param tenantId 租户ID
     * @return 定时任务ID
     */
    Long createScheduleReport(String reportId, String scheduleCron, 
                              List<String> emailRecipients, String exportFormat,
                              Map<String, Object> parameters, Long tenantId);

    /**
     * 更新定时报表任务
     *
     * @param scheduleId 定时任务ID
     * @param scheduleCron CRON表达式
     * @param emailRecipients 邮件接收人列表
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean updateScheduleReport(Long scheduleId, String scheduleCron,
                                  List<String> emailRecipients, Boolean enabled);

    /**
     * 删除定时报表任务
     *
     * @param scheduleId 定时任务ID
     * @return 是否成功
     */
    boolean deleteScheduleReport(Long scheduleId);

    /**
     * 获取定时报表任务列表
     *
     * @param reportId 报表ID（可选）
     * @param tenantId 租户ID
     * @return 定时任务列表
     */
    List<Map<String, Object>> getScheduleReports(String reportId, Long tenantId);

    /**
     * 立即执行一次定时报表
     *
     * @param scheduleId 定时任务ID
     * @return 是否成功
     */
    boolean triggerScheduleReport(Long scheduleId);

    /**
     * 暂停定时报表任务
     *
     * @param scheduleId 定时任务ID
     * @return 是否成功
     */
    boolean pauseScheduleReport(Long scheduleId);

    /**
     * 恢复定时报表任务
     *
     * @param scheduleId 定时任务ID
     * @return 是否成功
     */
    boolean resumeScheduleReport(Long scheduleId);

    /**
     * 获取定时报表执行历史
     *
     * @param scheduleId 定时任务ID
     * @param limit 限制数量
     * @return 执行历史列表
     */
    List<Map<String, Object>> getExecutionHistory(Long scheduleId, int limit);
}
