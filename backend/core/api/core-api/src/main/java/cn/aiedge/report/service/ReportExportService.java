package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 报表导出服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ReportExportService {

    /**
     * 导出报表为Excel
     *
     * @param reportData 报表数据
     * @param definition 报表定义
     * @param outputStream 输出流
     */
    void exportToExcel(ReportData reportData, ReportDefinition definition, OutputStream outputStream);

    /**
     * 导出报表为PDF
     *
     * @param reportData 报表数据
     * @param definition 报表定义
     * @param outputStream 输出流
     */
    void exportToPdf(ReportData reportData, ReportDefinition definition, OutputStream outputStream);

    /**
     * 导出报表为Word
     *
     * @param reportData 报表数据
     * @param definition 报表定义
     * @param outputStream 输出流
     */
    void exportToWord(ReportData reportData, ReportDefinition definition, OutputStream outputStream);

    /**
     * 导出报表为CSV
     *
     * @param reportData 报表数据
     * @param definition 报表定义
     * @param outputStream 输出流
     */
    void exportToCsv(ReportData reportData, ReportDefinition definition, OutputStream outputStream);

    /**
     * 导出报表为HTML
     *
     * @param reportData 报表数据
     * @param definition 报表定义
     * @return HTML字符串
     */
    String exportToHtml(ReportData reportData, ReportDefinition definition);
}
