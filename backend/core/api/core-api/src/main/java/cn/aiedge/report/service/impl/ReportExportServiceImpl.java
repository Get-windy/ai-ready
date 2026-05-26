package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.model.ReportDefinition.ReportColumn;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 报表导出服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {

    @Override
    public void exportToExcel(ReportData reportData, ReportDefinition definition, OutputStream outputStream) {
        log.info("导出Excel: reportId={}, reportName={}", reportData.getReportId(), reportData.getReportName());
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(reportData.getReportName());
            
            // 创建样式
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            XSSFCellStyle summaryStyle = workbook.createCellStyle();
            XSSFFont summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryStyle.setFont(summaryFont);
            summaryStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex());
            summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 创建表头
            XSSFRow headerRow = sheet.createRow(0);
            List<ReportColumn> columns = reportData.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                ReportColumn column = columns.get(i);
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(column.getTitle());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, column.getWidth() * 256); // 宽度转换
            }
            
            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> rowData : reportData.getRows()) {
                XSSFRow row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    XSSFCell cell = row.createCell(i);
                    Object value = rowData.get(column.getField());
                    
                    if (value != null) {
                        setCellValue(cell, value, column.getDataType());
                    }
                }
            }
            
            // 添加汇总行
            if (reportData.getSummary() != null && !reportData.getSummary().isEmpty()) {
                XSSFRow summaryRow = sheet.createRow(rowNum);
                XSSFCell cell = summaryRow.createCell(0);
                cell.setCellValue("汇总");
                cell.setCellStyle(summaryStyle);
                
                for (int i = 1; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    if (column.isAggregate()) {
                        XSSFCell summaryCell = summaryRow.createCell(i);
                        Object summaryValue = reportData.getSummary().get(column.getField());
                        if (summaryValue != null) {
                            setCellValue(summaryCell, summaryValue, column.getDataType());
                        }
                        summaryCell.setCellStyle(summaryStyle);
                    }
                }
            }
            
            workbook.write(outputStream);
            log.info("Excel导出完成: rows={}, time={}ms", reportData.getRows().size(), System.currentTimeMillis());
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public void exportToPdf(ReportData reportData, ReportDefinition definition, OutputStream outputStream) {
        log.info("导出PDF: reportId={}, reportName={}", reportData.getReportId(), reportData.getReportName());
        
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // 添加标题
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph(reportData.getReportName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));
            
            // 创建表格
            List<ReportColumn> columns = reportData.getColumns();
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            
            // 添加表头
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            for (ReportColumn column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column.getTitle(), headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }
            
            // 添加数据行
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            for (Map<String, Object> rowData : reportData.getRows()) {
                for (ReportColumn column : columns) {
                    Object value = rowData.get(column.getField());
                    String displayValue = value != null ? value.toString() : "";
                    PdfPCell cell = new PdfPCell(new Phrase(displayValue, dataFont));
                    table.addCell(cell);
                }
            }
            
            // 添加汇总行
            if (reportData.getSummary() != null && !reportData.getSummary().isEmpty()) {
                Font summaryFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
                for (int i = 0; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    if (i == 0) {
                        PdfPCell cell = new PdfPCell(new Phrase("汇总", summaryFont));
                        cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                        table.addCell(cell);
                    } else {
                        Object summaryValue = reportData.getSummary().get(column.getField());
                        String displayValue = summaryValue != null ? summaryValue.toString() : "";
                        PdfPCell cell = new PdfPCell(new Phrase(displayValue, summaryFont));
                        cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                        table.addCell(cell);
                    }
                }
            }
            
            document.add(table);
            
            log.info("PDF导出完成: rows={}", reportData.getRows().size());
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            throw new RuntimeException("导出PDF失败: " + e.getMessage());
        } finally {
            if (document != null && document.isOpen()) {
                try {
                    document.close();
                } catch (Exception e) {
                    log.error("关闭PDF文档失败", e);
                }
            }
        }
    }

    @Override
    public void exportToWord(ReportData reportData, ReportDefinition definition, OutputStream outputStream) {
        log.info("导出Word: reportId={}, reportName={}", reportData.getReportId(), reportData.getReportName());
        
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(reportData.getReportName());
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            
            // 创建表格
            XWPFTable table = document.createTable();
            
            // 添加表头
            XWPFTableRow headerRow = table.getRow(0);
            List<ReportColumn> columns = reportData.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                ReportColumn column = columns.get(i);
                if (i == 0) {
                    headerRow.getCell(0).setText(column.getTitle());
                } else {
                    headerRow.addNewTableCell().setText(column.getTitle());
                }
            }
            
            // 设置表头样式
            XWPFTableRow modifiedHeaderRow = table.getRow(0);
            for (int i = 0; i < columns.size(); i++) {
                XWPFTableCell cell = modifiedHeaderRow.getCell(i);
                XWPFParagraph para = cell.getParagraphs().get(0);
                XWPFRun run = para.getRuns().get(0);
                run.setBold(true);
                run.setFontSize(11);
            }
            
            // 添加数据行
            for (Map<String, Object> rowData : reportData.getRows()) {
                XWPFTableRow dataRow = table.createRow();
                for (int i = 0; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    Object value = rowData.get(column.getField());
                    String displayValue = value != null ? value.toString() : "";
                    dataRow.getCell(i).setText(displayValue);
                }
            }
            
            // 添加汇总行
            if (reportData.getSummary() != null && !reportData.getSummary().isEmpty()) {
                XWPFTableRow summaryRow = table.createRow();
                summaryRow.getCell(0).setText("汇总");
                
                for (int i = 1; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    if (column.isAggregate()) {
                        Object summaryValue = reportData.getSummary().get(column.getField());
                        String displayValue = summaryValue != null ? summaryValue.toString() : "";
                        summaryRow.getCell(i).setText(displayValue);
                    }
                }
            }
            
            document.write(outputStream);
            log.info("Word导出完成: rows={}", reportData.getRows().size());
        } catch (IOException e) {
            log.error("导出Word失败", e);
            throw new RuntimeException("导出Word失败: " + e.getMessage());
        }
    }

    @Override
    public void exportToCsv(ReportData reportData, ReportDefinition definition, OutputStream outputStream) {
        log.info("导出CSV: reportId={}, reportName={}", reportData.getReportId(), reportData.getReportName());
        
        try {
            StringBuilder csv = new StringBuilder();
            
            // 表头
            List<ReportColumn> columns = reportData.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) csv.append(",");
                csv.append("\"").append(escapeCsvValue(columns.get(i).getTitle())).append("\"");
            }
            csv.append("\n");
            
            // 数据行
            for (Map<String, Object> rowData : reportData.getRows()) {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) csv.append(",");
                    Object value = rowData.get(columns.get(i).getField());
                    csv.append("\"").append(escapeCsvValue(value != null ? value.toString() : "")).append("\"");
                }
                csv.append("\n");
            }
            
            // 添加汇总行
            if (reportData.getSummary() != null && !reportData.getSummary().isEmpty()) {
                csv.append("\"汇总\"");
                for (int i = 1; i < columns.size(); i++) {
                    csv.append(",");
                    ReportColumn column = columns.get(i);
                    if (column.isAggregate()) {
                        Object summaryValue = reportData.getSummary().get(column.getField());
                        csv.append("\"").append(escapeCsvValue(summaryValue != null ? summaryValue.toString() : "")).append("\"");
                    } else {
                        csv.append("\"\"");
                    }
                }
                csv.append("\n");
            }
            
            outputStream.write(csv.toString().getBytes("UTF-8"));
            log.info("CSV导出完成: rows={}", reportData.getRows().size());
        } catch (IOException e) {
            log.error("导出CSV失败", e);
            throw new RuntimeException("导出CSV失败: " + e.getMessage());
        }
    }

    @Override
    public String exportToHtml(ReportData reportData, ReportDefinition definition) {
        log.info("导出HTML: reportId={}, reportName={}", reportData.getReportId(), reportData.getReportName());
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(escapeHtml(reportData.getReportName())).append("</title>\n");
        html.append("<style>\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }\n");
        html.append(".summary-row { background-color: #ffffcc; font-weight: bold; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        // 标题
        html.append("<h2>").append(escapeHtml(reportData.getReportName())).append("</h2>\n");
        
        // 表格
        html.append("<table>\n");
        
        // 表头
        html.append("<thead>\n<tr>\n");
        for (ReportColumn column : reportData.getColumns()) {
            html.append("<th>").append(escapeHtml(column.getTitle())).append("</th>\n");
        }
        html.append("</tr>\n</thead>\n");
        
        // 数据行
        html.append("<tbody>\n");
        for (Map<String, Object> rowData : reportData.getRows()) {
            html.append("<tr>\n");
            for (ReportColumn column : reportData.getColumns()) {
                Object value = rowData.get(column.getField());
                html.append("<td>").append(escapeHtml(value != null ? value.toString() : "")).append("</td>\n");
            }
            html.append("</tr>\n");
        }
        
        // 汇总行
        if (reportData.getSummary() != null && !reportData.getSummary().isEmpty()) {
            html.append("<tr class=\"summary-row\">\n<td>汇总</td>\n");
            for (int i = 1; i < reportData.getColumns().size(); i++) {
                ReportColumn column = reportData.getColumns().get(i);
                if (column.isAggregate()) {
                    Object summaryValue = reportData.getSummary().get(column.getField());
                    html.append("<td>").append(escapeHtml(summaryValue != null ? summaryValue.toString() : "")).append("</td>\n");
                } else {
                    html.append("<td></td>\n");
                }
            }
            html.append("</tr>\n");
        }
        
        html.append("</tbody>\n");
        html.append("</table>\n");
        html.append("</body>\n</html>");
        
        log.info("HTML导出完成: rows={}", reportData.getRows().size());
        return html.toString();
    }

    /**
     * 设置Excel单元格值
     */
    private void setCellValue(XSSFCell cell, Object value, String dataType) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        
        switch (dataType.toLowerCase()) {
            case "number":
            case "int":
            case "integer":
            case "long":
            case "float":
            case "double":
                try {
                    cell.setCellValue(Double.parseDouble(value.toString()));
                } catch (NumberFormatException e) {
                    cell.setCellValue(value.toString());
                }
                break;
            case "date":
            case "datetime":
                cell.setCellValue(value.toString());
                break;
            case "boolean":
                cell.setCellValue(Boolean.parseBoolean(value.toString()));
                break;
            default:
                cell.setCellValue(value.toString());
        }
    }

    /**
     * 转义CSV值
     */
    private String escapeCsvValue(String value) {
        if (value == null) return "";
        // 替换双引号
        value = value.replace("\"", "\"\"");
        return value;
    }

    /**
     * 转义HTML特殊字符
     */
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
