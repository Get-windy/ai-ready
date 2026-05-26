package cn.aiedge.report.service;

import cn.aiedge.notification.service.NotificationService;
import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 定时报表任务Job
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledReportJob implements Job {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ReportExportService reportExportService;
    
    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        
        String reportId = dataMap.getString("reportId");
        String emailRecipientsStr = dataMap.getString("emailRecipients");
        String exportFormat = dataMap.getString("exportFormat");
        String parametersStr = dataMap.getString("parameters");
        Long tenantId = Long.valueOf(dataMap.getString("tenantId"));
        
        log.info("开始执行定时报表任务: reportId={}, format={}", reportId, exportFormat);
        
        try {
            // 解析参数
            Map<String, Object> parameters = parseParameters(parametersStr);
            
            // 生成报表数据
            ReportDefinition definition = reportService.getReportDefinition(reportId);
            if (definition == null) {
                log.error("报表定义不存在: reportId={}", reportId);
                return;
            }
            
            ReportData reportData = reportService.generateReport(reportId, parameters, tenantId);
            
            // 根据格式导出报表
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            switch (exportFormat.toLowerCase()) {
                case "excel":
                    reportExportService.exportToExcel(reportData, definition, outputStream);
                    break;
                case "pdf":
                    reportExportService.exportToPdf(reportData, definition, outputStream);
                    break;
                case "word":
                    reportExportService.exportToWord(reportData, definition, outputStream);
                    break;
                case "csv":
                    reportExportService.exportToCsv(reportData, definition, outputStream);
                    break;
                default:
                    log.warn("不支持的导出格式: {}", exportFormat);
                    return;
            }
            
            // 发送邮件
            List<String> emailRecipients = Arrays.asList(emailRecipientsStr.split(","));
            String reportName = reportData.getReportName();
            String subject = "定时报表 - " + reportName;
            String content = "您好，附件是您订阅的定时报表：" + reportName + 
                           "\n生成时间：" + reportData.getGeneratedAt();
            
            // 发送通知（邮件）
            for (String email : emailRecipients) {
                // 在实际实现中，这里会调用通知服务发送邮件
                notificationService.sendEmail(email, subject, content);
                
                // 如果支持附件，在实际实现中还会发送报表文件
                log.info("报表已发送至: {}", email);
            }
            
            log.info("定时报表任务执行成功: reportId={}", reportId);
        } catch (Exception e) {
            log.error("执行定时报表任务失败: reportId=" + reportId, e);
            throw new JobExecutionException("执行定时报表任务失败", e);
        }
    }

    /**
     * 解析参数字符串
     */
    private Map<String, Object> parseParameters(String parametersStr) {
        // 在实际实现中，会解析参数字符串为Map
        // 这里返回空Map作为示例
        return Map.of();
    }
}
