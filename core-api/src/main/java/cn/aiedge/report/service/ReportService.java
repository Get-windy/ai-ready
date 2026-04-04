package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 报表服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ReportService {

    /**
     * 获取报表定义
     *
     * @param reportId 报表ID
     * @return 报表定义
     */
    ReportDefinition getReportDefinition(String reportId);

    /**
     * 获取报表列表
     *
     * @param category 分类（可选）
     * @param tenantId 租户ID
     * @return 报表定义列表
     */
    List<ReportDefinition> getReportList(String category, Long tenantId);

    /**
     * 生成报表数据
     *
     * @param reportId  报表ID
     * @param parameters 参数
     * @param tenantId  租户ID
     * @return 报表数据
     */
    ReportData generateReport(String reportId, Map<String, Object> parameters, Long tenantId);

    /**
     * 导出报表为Excel
     *
     * @param reportId  报表ID
     * @param parameters 参数
     * @param outputStream 输出流
     * @param tenantId  租户ID
     */
    void exportToExcel(String reportId, Map<String, Object> parameters, 
                       OutputStream outputStream, Long tenantId);

    /**
     * 导出报表为PDF
     *
     * @param reportId  报表ID
     * @param parameters 参数
     * @param outputStream 输出流
     * @param tenantId  租户ID
     */
    void exportToPdf(String reportId, Map<String, Object> parameters, 
                     OutputStream outputStream, Long tenantId);

    /**
     * 导出报表为CSV
     *
     * @param reportId  报表ID
     * @param parameters 参数
     * @param outputStream 输出流
     * @param tenantId  租户ID
     */
    void exportToCsv(String reportId, Map<String, Object> parameters, 
                     OutputStream outputStream, Long tenantId);

    /**
     * 保存报表定义
     *
     * @param definition 报表定义
     * @param tenantId   租户ID
     * @return 保存后的报表定义
     */
    ReportDefinition saveReportDefinition(ReportDefinition definition, Long tenantId);

    /**
     * 删除报表定义
     *
     * @param reportId 报表ID
     * @param tenantId 租户ID
     * @return 是否成功
     */
    boolean deleteReportDefinition(String reportId, Long tenantId);

    /**
     * 复制报表定义
     *
     * @param reportId 源报表ID
     * @param newName  新报表名称
     * @param tenantId 租户ID
     * @return 新报表定义
     */
    ReportDefinition copyReportDefinition(String reportId, String newName, Long tenantId);

    /**
     * 预览报表数据（限制行数）
     *
     * @param reportId  报表ID
     * @param parameters 参数
     * @param maxRows   最大行数
     * @param tenantId  租户ID
     * @return 报表数据
     */
    ReportData previewReport(String reportId, Map<String, Object> parameters, 
                             int maxRows, Long tenantId);
}
