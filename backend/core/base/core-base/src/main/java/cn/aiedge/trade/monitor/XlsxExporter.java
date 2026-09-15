package cn.aiedge.trade.monitor;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 单表 xlsx 导出（API 监控页两个台账共用）
 *
 * <p>导出**真实 xlsx**（非 JSON / 非 CSV）：调用日志与库存同步记录结构相同（表头 + 行），
 * 故下沉为一个写入器，避免两处重复实现（《配送模块 README》§6 开发原则）。</p>
 */
public final class XlsxExporter {

    private XlsxExporter() {
    }

    /**
     * 写出 xlsx 响应
     *
     * @param response  响应
     * @param fileName  文件名（含 .xlsx，中文由 RFC 5987 编码）
     * @param sheetName 工作表名
     * @param headers   表头
     * @param rows      数据行（每行长度应与表头一致，不足处留空）
     */
    public static void write(HttpServletResponse response, String fileName, String sheetName,
                             String[] headers, List<String[]> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    row.createCell(i).setCellValue(i < values.length && values[i] != null ? values[i] : "");
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }
}
