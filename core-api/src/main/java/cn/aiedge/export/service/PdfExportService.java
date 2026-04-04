package cn.aiedge.export.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * PDF导出服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class PdfExportService {

    // 中文字体（需要添加itext-asian依赖）
    private static final String FONT_PATH = "/fonts/simsun.ttc";
    private BaseFont chineseFont;

    public PdfExportService() {
        try {
            // 尝试加载中文字体
            chineseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("无法加载中文字体，PDF中文可能显示异常");
        }
    }

    /**
     * 导出为PDF
     *
     * @param data    数据列表
     * @param headers 表头定义
     * @param title   文档标题
     * @param out     输出流
     */
    public <T> void exportPdf(List<T> data, Map<String, String> headers, String title, OutputStream out) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // 添加标题
            if (title != null && !title.isEmpty()) {
                Font titleFont = new Font(chineseFont, 18, Font.BOLD);
                Paragraph titleParagraph = new Paragraph(title, titleFont);
                titleParagraph.setAlignment(Element.ALIGN_CENTER);
                titleParagraph.setSpacingAfter(20);
                document.add(titleParagraph);
            }

            // 创建表格
            List<String> fieldNames = new java.util.ArrayList<>(headers.keySet());
            int columnCount = fieldNames.size();

            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);

            // 设置表头
            Font headerFont = new Font(chineseFont, 10, Font.BOLD);
            headerFont.setColor(BaseColor.WHITE);
            for (String field : fieldNames) {
                PdfPCell cell = new PdfPCell(new Phrase(headers.get(field), headerFont));
                cell.setBackgroundColor(new BaseColor(66, 139, 202));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // 填充数据
            Font dataFont = new Font(chineseFont, 9, Font.NORMAL);
            for (T item : data) {
                Map<String, Object> map = cn.hutool.core.bean.BeanUtil.beanToMap(item);
                for (String field : fieldNames) {
                    Object value = map.get(field);
                    String text = value != null ? String.valueOf(value) : "";
                    PdfPCell cell = new PdfPCell(new Phrase(text, dataFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(5);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();

            log.info("PDF导出完成，共{}条数据", data.size());

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new RuntimeException("PDF导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出简单PDF报告
     *
     * @param content 报告内容
     * @param title   标题
     * @param out     输出流
     */
    public void exportReport(String content, String title, OutputStream out) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(chineseFont, 16, Font.BOLD);
            Font contentFont = new Font(chineseFont, 10, Font.NORMAL);

            // 标题
            if (title != null && !title.isEmpty()) {
                Paragraph titleParagraph = new Paragraph(title, titleFont);
                titleParagraph.setAlignment(Element.ALIGN_CENTER);
                titleParagraph.setSpacingAfter(20);
                document.add(titleParagraph);
            }

            // 内容
            Paragraph contentParagraph = new Paragraph(content, contentFont);
            contentParagraph.setLeading(20);
            document.add(contentParagraph);

            document.close();

        } catch (Exception e) {
            log.error("PDF报告导出失败", e);
            throw new RuntimeException("PDF报告导出失败: " + e.getMessage(), e);
        }
    }
}
