package cn.aiedge.base.log;

import cn.aiedge.base.entity.SysOperLog;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志导出服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出操作日志为Excel
     *
     * @param logs     日志列表
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    public void exportToExcel(List<SysOperLog> logs, HttpServletResponse response) throws IOException {
        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");

            // 创建标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "用户名", "模块", "操作", "请求方法", "请求URL", 
                    "状态", "耗时(ms)", "操作时间", "IP地址", "操作地点", "错误信息"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // 填充数据
            int rowNum = 1;
            for (SysOperLog log : logs) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(log.getId() != null ? log.getId().toString() : "");
                row.createCell(1).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(2).setCellValue(log.getModule() != null ? log.getModule() : "");
                row.createCell(3).setCellValue(log.getAction() != null ? log.getAction() : "");
                row.createCell(4).setCellValue(log.getRequestMethod() != null ? log.getRequestMethod() : "");
                row.createCell(5).setCellValue(log.getRequestUrl() != null ? log.getRequestUrl() : "");
                row.createCell(6).setCellValue(log.getStatus() != null ? (log.getStatus() == 0 ? "成功" : "失败") : "");
                row.createCell(7).setCellValue(log.getCostTime() != null ? log.getCostTime() : 0);

                Cell dateCell = row.createCell(8);
                if (log.getOperTime() != null) {
                    dateCell.setCellValue(log.getOperTime().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);
                }

                row.createCell(9).setCellValue(log.getOperIp() != null ? log.getOperIp() : "");
                row.createCell(10).setCellValue(log.getOperLocation() != null ? log.getOperLocation() : "");
                row.createCell(11).setCellValue(log.getErrorMsg() != null ? log.getErrorMsg() : "");
            }

            // 设置响应头
            String filename = "oper_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodeFilename(filename));

            // 写入响应
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        }
    }

    /**
     * 导出操作日志为CSV
     *
     * @param logs     日志列表
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    public void exportToCsv(List<SysOperLog> logs, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        String filename = "oper_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + encodeFilename(filename));

        OutputStream out = response.getOutputStream();
        StringBuilder sb = new StringBuilder();

        // BOM头（解决Excel乱码）
        out.write('\ufeff');

        // 表头
        sb.append("ID,用户名,模块,操作,请求方法,请求URL,状态,耗时(ms),操作时间,IP地址,操作地点,错误信息\n");

        // 数据
        for (SysOperLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(escapeCsv(log.getModule())).append(",");
            sb.append(escapeCsv(log.getAction())).append(",");
            sb.append(escapeCsv(log.getRequestMethod())).append(",");
            sb.append(escapeCsv(log.getRequestUrl())).append(",");
            sb.append(log.getStatus() == 0 ? "成功" : "失败").append(",");
            sb.append(log.getCostTime() != null ? log.getCostTime() : 0).append(",");
            sb.append(log.getOperTime() != null ? log.getOperTime().format(DATE_FORMATTER) : "").append(",");
            sb.append(escapeCsv(log.getOperIp())).append(",");
            sb.append(escapeCsv(log.getOperLocation())).append(",");
            sb.append(escapeCsv(log.getErrorMsg())).append("\n");
        }

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * 导出操作日志为JSON
     *
     * @param logs     日志列表
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    public void exportToJson(List<SysOperLog> logs, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String filename = "oper_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        response.setHeader("Content-Disposition", "attachment; filename=" + encodeFilename(filename));

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < logs.size(); i++) {
            SysOperLog log = logs.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(log.getId()).append("\",\n");
            sb.append("    \"username\": \"").append(escapeJson(log.getUsername())).append("\",\n");
            sb.append("    \"module\": \"").append(escapeJson(log.getModule())).append("\",\n");
            sb.append("    \"action\": \"").append(escapeJson(log.getAction())).append("\",\n");
            sb.append("    \"requestMethod\": \"").append(escapeJson(log.getRequestMethod())).append("\",\n");
            sb.append("    \"requestUrl\": \"").append(escapeJson(log.getRequestUrl())).append("\",\n");
            sb.append("    \"status\": ").append(log.getStatus()).append(",\n");
            sb.append("    \"costTime\": ").append(log.getCostTime() != null ? log.getCostTime() : 0).append(",\n");
            sb.append("    \"operTime\": \"").append(log.getOperTime() != null ? log.getOperTime().format(DATE_FORMATTER) : "").append("\",\n");
            sb.append("    \"operIp\": \"").append(escapeJson(log.getOperIp())).append("\",\n");
            sb.append("    \"operLocation\": \"").append(escapeJson(log.getOperLocation())).append("\",\n");
            sb.append("    \"errorMsg\": \"").append(escapeJson(log.getErrorMsg())).append("\"\n");
            sb.append("  }").append(i < logs.size() - 1 ? "," : "").append("\n");
        }

        sb.append("]");

        OutputStream out = response.getOutputStream();
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // ==================== 辅助方法 ====================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    private String encodeFilename(String filename) throws Exception {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
