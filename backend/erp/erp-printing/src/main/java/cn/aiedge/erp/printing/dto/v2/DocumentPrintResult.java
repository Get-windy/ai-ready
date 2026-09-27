package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按单据渲染打印内容的结果。
 *
 * 前端拿 {@code html} 直接送打印；其余字段用于预览标题、纸张选择与「记住上次用的模板」。
 */
@Data
public class DocumentPrintResult {

    /** 渲染好的整页 HTML */
    private String html;

    private Long templateId;

    private String templateName;

    private String paperSize;

    private BigDecimal paperWidth;

    private BigDecimal paperHeight;

    /** 该页面当前的默认模板 id，供前端下拉预选 */
    private Long defaultTemplateId;

    /** 单据编号，用于打印留痕 */
    private String documentNo;
}
