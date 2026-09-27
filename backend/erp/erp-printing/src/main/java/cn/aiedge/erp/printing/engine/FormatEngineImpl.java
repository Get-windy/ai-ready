package cn.aiedge.erp.printing.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 格式化引擎实现
 *
 * 负责将模板 JSON + 数据 JSON 渲染为 HTML。
 * 支持：
 * 1. 字段值格式化（含自定义函数）
 * 2. 条码/二维码占位符（客户端渲染时动态生成）
 * 3. 多组件布局输出
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormatEngineImpl implements FormatEngine {

    private final ExpressionEvaluator expressionEvaluator;
    private final ObjectMapper objectMapper;

    @Override
    public String format(Object value, Map<String, Object> formatConfig) {
        if (formatConfig == null || formatConfig.isEmpty()) {
            return String.valueOf(value);
        }

        String type = (String) formatConfig.getOrDefault("type", "none");

        switch (type) {
            case "expression":
                return formatByExpression(value, formatConfig);
            case "date":
                return formatDate(value, formatConfig);
            case "number":
                return formatNumber(value, formatConfig);
            case "enum":
                return formatEnum(value, formatConfig);
            case "rmbUpper":
                return rmbUpper(value);
            default:
                return String.valueOf(value);
        }
    }

    private String formatByExpression(Object value, Map<String, Object> config) {
        String expression = (String) config.get("expression");
        if (expression == null || expression.trim().isEmpty()) {
            return String.valueOf(value);
        }
        Object result = expressionEvaluator.evaluate(expression, value);
        return result != null ? result.toString() : "";
    }

    /**
     * 日期格式化。
     *
     * 实体里的日期字段有 {@code LocalDate} 也有 {@code LocalDateTime}，序列化后分别是
     * {@code 2026-09-02} 与 {@code 2026-09-03T00:00:00} —— 后者直接打在单据上会带个 {@code T}，
     * 所以两种都要能解析；都不是就原样返回（不制造空值）。
     */
    private String formatDate(Object value, Map<String, Object> config) {
        if (value == null) return "";
        // 简单的日期格式化，更复杂的由 expression 模式处理
        String pattern = (String) config.getOrDefault("pattern", "yyyy-MM-dd");
        String raw = value.toString();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern(pattern);
        try {
            return java.time.LocalDateTime.parse(raw).format(fmt);
        } catch (Exception ignore) {
            // 落到 LocalDate
        }
        try {
            return java.time.LocalDate.parse(raw).format(fmt);
        } catch (Exception ignore) {
            return raw;
        }
    }

    private String formatNumber(Object value, Map<String, Object> config) {
        if (value == null) return "0";
        try {
            double num = Double.parseDouble(value.toString());
            int decimals = (int) config.getOrDefault("decimals", 2);
            boolean thousands = (boolean) config.getOrDefault("thousands", true);
            String fmt = "%." + decimals + "f";
            String result = String.format(fmt, num);
            if (thousands) {
                String[] parts = result.split("\\.");
                StringBuilder intPart = new StringBuilder(parts[0]);
                int len = intPart.length();
                for (int i = len - 3; i > 0; i -= 3) {
                    intPart.insert(i, ",");
                }
                result = intPart.toString() + (parts.length > 1 ? "." + parts[1] : "");
            }
            return result;
        } catch (Exception e) {
            return value.toString();
        }
    }

    private String formatEnum(Object value, Map<String, Object> config) {
        if (value == null) return "";
        @SuppressWarnings("unchecked")
        Map<String, String> mapping = (Map<String, String>) config.get("mapping");
        if (mapping != null && mapping.containsKey(value.toString())) {
            return mapping.get(value.toString());
        }
        return value.toString();
    }

    @Override
    public FormulaValidationResult validateExpression(String expression, Object sampleValue) {
        return expressionEvaluator.validate(expression, sampleValue);
    }

    @Override
    public String renderToHtml(String templateJson, String dataJson) {
        try {
            Map<String, Object> template = objectMapper.readValue(templateJson,
                    new TypeReference<Map<String, Object>>() {});
            Map<String, Object> data = objectMapper.readValue(dataJson,
                    new TypeReference<Map<String, Object>>() {});

            // v1：绝对坐标（freeLayout=true，或模板里只有 components）→ 旧渲染路径
            // v2：分区流式（模板里有 sections）→ 新渲染路径
            if (Boolean.TRUE.equals(template.get("freeLayout")) || !template.containsKey("sections")) {
                return doRender(template, data);
            }
            return doRenderSections(template, data);
        } catch (Exception e) {
            log.error("渲染模板 HTML 失败", e);
            return "<html><body><p>渲染错误: " + e.getMessage() + "</p></body></html>";
        }
    }

    @SuppressWarnings("unchecked")
    private String doRender(Map<String, Object> template, Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"utf-8\">");
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("body { font-family: 'SimSun', 'Microsoft YaHei', sans-serif; ");
        html.append("  font-size: 12px; color: #333; }");
        html.append("table { border-collapse: collapse; width: 100%; }");
        html.append("td, th { border: 1px solid #ccc; padding: 4px 6px; text-align: left; }");
        html.append(".barcode-placeholder { display: inline-block; border: 1px dashed #999; ");
        html.append("  padding: 10px 20px; background: #f5f5f5; text-align: center; ");
        html.append("  font-size: 11px; color: #666; }");
        html.append(".qrcode-placeholder { display: inline-block; border: 1px dashed #999; ");
        html.append("  padding: 15px; background: #f5f5f5; text-align: center; }");
        html.append("</style></head><body>");

        // 纸张尺寸
        String paperSize = (String) template.getOrDefault("paperSize", "A4");
        Number paperWidth = (Number) template.get("paperWidth");
        Number paperHeight = (Number) template.get("paperHeight");

        Number marginTop = (Number) template.getOrDefault("marginTop", 10);
        Number marginBottom = (Number) template.getOrDefault("marginBottom", 10);
        Number marginLeft = (Number) template.getOrDefault("marginLeft", 10);
        Number marginRight = (Number) template.getOrDefault("marginRight", 10);

        if ("CUSTOM".equals(paperSize) && paperWidth != null && paperHeight != null) {
            html.append("<div style=\"width:").append(paperWidth).append("mm;");
            html.append("height:").append(paperHeight).append("mm;");
        } else {
            html.append("<div style=\"width:190mm;min-height:277mm;");
        }
        html.append("padding:").append(marginTop).append("mm ")
                .append(marginRight).append("mm ")
                .append(marginBottom).append("mm ")
                .append(marginLeft).append("mm;");
        html.append("position:relative;\">");

        // 渲染组件
        List<Map<String, Object>> components = (List<Map<String, Object>>) template.getOrDefault("components", new ArrayList<>());
        for (Map<String, Object> comp : components) {
            html.append(renderComponent(comp, data));
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderComponent(Map<String, Object> comp, Map<String, Object> data) {
        String type = (String) comp.getOrDefault("type", "label");
        Number x = (Number) comp.getOrDefault("x", 0);
        Number y = (Number) comp.getOrDefault("y", 0);
        Number w = (Number) comp.getOrDefault("w", 100);
        Number h = (Number) comp.getOrDefault("h", 20);
        String field = (String) comp.get("field");
        Map<String, Object> style = (Map<String, Object>) comp.getOrDefault("style", new HashMap<>());
        Map<String, Object> formatConfig = (Map<String, Object>) comp.get("formatConfig");

        String inlineStyle = buildStyle(style, x, y, w, h);

        switch (type) {
            case "label":
                return renderLabel(comp, inlineStyle);
            case "field":
                return renderField(comp, field, data, formatConfig, inlineStyle);
            case "table":
                return renderTable(comp, data, inlineStyle);
            case "barcode":
                return renderBarcode(field, data, inlineStyle);
            case "qrcode":
                return renderQrcode(field, data, inlineStyle);
            case "image":
                return renderImage(comp, inlineStyle);
            case "line":
                return "<hr style=\"" + inlineStyle + "border:0;border-top:1px solid #333;\">";
            default:
                return "<div style=\"" + inlineStyle + "\">未知组件</div>";
        }
    }

    private String renderLabel(Map<String, Object> comp, String style) {
        String text = (String) comp.getOrDefault("content", "");
        return "<div style=\"" + style + "\">" + escapeHtml(text) + "</div>";
    }

    private String renderField(Map<String, Object> comp, String field,
                                Map<String, Object> data, Map<String, Object> formatConfig,
                                String style) {
        Object rawValue = null;
        if (field != null && data.containsKey(field)) {
            rawValue = data.get(field);
        }

        String displayValue;
        if (formatConfig != null && !formatConfig.isEmpty()) {
            displayValue = format(rawValue, formatConfig);
        } else {
            displayValue = rawValue != null ? rawValue.toString() : "";
        }

        return "<div style=\"" + style + "\">" + escapeHtml(displayValue) + "</div>";
    }

    @SuppressWarnings("unchecked")
    private String renderTable(Map<String, Object> comp, Map<String, Object> data, String style) {
        String field = (String) comp.get("field");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) comp.getOrDefault("columns", new ArrayList<>());

        if (columns.isEmpty()) {
            return "<div style=\"" + style + "\">表格（未配置列）</div>";
        }

        StringBuilder table = new StringBuilder();
        table.append("<table style=\"").append(style).append("\">");
        table.append("<thead><tr>");
        for (Map<String, Object> col : columns) {
            String header = (String) col.getOrDefault("header", "");
            String colStyle = (String) col.getOrDefault("width", "");
            table.append("<th");
            if (!colStyle.isEmpty()) {
                table.append(" style=\"width:").append(colStyle).append("\"");
            }
            table.append(">").append(escapeHtml(header)).append("</th>");
        }
        table.append("</tr></thead><tbody>");

        // 从数据中获取表格行
        List<Map<String, Object>> rows = new ArrayList<>();
        if (field != null && data.get(field) instanceof List) {
            rows = (List<Map<String, Object>>) data.get(field);
        }

        for (Map<String, Object> row : rows) {
            table.append("<tr>");
            for (Map<String, Object> col : columns) {
                String colField = (String) col.get("field");
                Object cellValue = colField != null ? row.get(colField) : "";
                String display = cellValue != null ? cellValue.toString() : "";
                table.append("<td>").append(escapeHtml(display)).append("</td>");
            }
            table.append("</tr>");
        }

        table.append("</tbody></table>");
        return table.toString();
    }

    private String renderBarcode(String field, Map<String, Object> data, String style) {
        String value = field != null && data.containsKey(field) ? data.get(field).toString() : "";
        return "<div class=\"barcode-placeholder\" style=\"" + style + "\">"
                + "<div>[条码]</div>"
                + "<div style=\"font-size:16px;letter-spacing:2px;\">" + escapeHtml(value) + "</div>"
                + "</div>";
    }

    private String renderQrcode(String field, Map<String, Object> data, String style) {
        String value = field != null && data.containsKey(field) ? data.get(field).toString() : "";
        return "<div class=\"qrcode-placeholder\" style=\"" + style + "\">"
                + "<div>[二维码]</div>"
                + "<div style=\"font-size:10px;word-break:break-all;\">" + escapeHtml(value) + "</div>"
                + "</div>";
    }

    private String renderImage(Map<String, Object> comp, String style) {
        String src = (String) comp.getOrDefault("src", "");
        if (src.isEmpty()) {
            return "<div style=\"" + style + "background:#eee;text-align:center;line-height:40px;\">图片</div>";
        }
        return "<img src=\"" + escapeHtml(src) + "\" style=\"" + style + "\">";
    }

    private String buildStyle(Map<String, Object> style, Number x, Number y, Number w, Number h) {
        StringBuilder sb = new StringBuilder();
        sb.append("position:absolute;");
        sb.append("left:").append(x).append("mm;");
        sb.append("top:").append(y).append("mm;");
        sb.append("width:").append(w).append("mm;");
        sb.append("min-height:").append(h).append("mm;");

        if (style != null) {
            appendStyleProp(sb, style, "fontSize", "font-size", "px");
            appendStyleProp(sb, style, "fontWeight", "font-weight");
            appendStyleProp(sb, style, "fontFamily", "font-family");
            appendStyleProp(sb, style, "color", "color");
            appendStyleProp(sb, style, "backgroundColor", "background-color");
            appendStyleProp(sb, style, "textAlign", "text-align");
            appendStyleProp(sb, style, "border", "border");
            appendStyleProp(sb, style, "padding", "padding");
            appendStyleProp(sb, style, "fontStyle", "font-style");
            appendStyleProp(sb, style, "textDecoration", "text-decoration");
        }

        return sb.toString();
    }

    private void appendStyleProp(StringBuilder sb, Map<String, Object> style, String key, String cssKey) {
        appendStyleProp(sb, style, key, cssKey, null);
    }

    private void appendStyleProp(StringBuilder sb, Map<String, Object> style, String key, String cssKey, String unit) {
        if (style.containsKey(key)) {
            Object val = style.get(key);
            if (val != null) {
                sb.append(cssKey).append(":").append(val);
                if (unit != null && !(val instanceof String && ((String) val).endsWith(unit))) {
                    sb.append(unit);
                }
                sb.append(";");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // v2 分区流式渲染
    //
    // 与 v1（绝对坐标）的分工：
    //   v1 freeLayout=true  → 套打/固定版面，x/y/w/h 全部由模板给死
    //   v2 sections         → 分区流式，坐标由引擎按 12 栅格 + 分页算出来
    //
    // 为什么坚持 table + inline style：
    //   最终走浏览器打印 / wkhtmltopdf 这类老 WebKit，Flexbox、Grid、
    //   CSS 变量一律不可靠；table 的 <thead> 每页重印是原生行为，最稳。
    // ══════════════════════════════════════════════════════════════════════

    /** 中文大写数字 / 节权 / 子权（金额大写与序号无关，纯 4 位一组） */
    private static final String[] RMB_DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] RMB_SUB = {"", "拾", "佰", "仟"};
    private static final String[] RMB_SEC = {"", "万", "亿", "万亿"};

    /**
     * 分区渲染入口。
     *
     * 页结构（每页一个 .print-page 块，固定纸高 + overflow:hidden，页间 page-break-after）：
     *   pageHeader(每页) → docHeader(仅首页) → items(本页明细 + 末页合计) → summary(仅末页) → pageFooter(每页)
     */
    @SuppressWarnings("unchecked")
    private String doRenderSections(Map<String, Object> template, Map<String, Object> data) {
        Map<String, Object> sections = (Map<String, Object>) template.getOrDefault("sections", new HashMap<>());

        // ── 纸张与页边距 ──
        String paperSize = str(template.get("paperSize"), "A4");
        double[] paper = paperMm(paperSize);
        double pageW = numOf(template.get("pageWidth"), paper[0]);
        double pageH = numOf(template.get("pageHeight"), paper[1]);
        Map<String, Object> margin = (Map<String, Object>) template.get("margin");
        double mt = margin != null ? numOf(margin.get("top"), 10) : numOf(template.get("marginTop"), 10);
        double mr = margin != null ? numOf(margin.get("right"), 10) : numOf(template.get("marginRight"), 10);
        double mb = margin != null ? numOf(margin.get("bottom"), 10) : numOf(template.get("marginBottom"), 10);
        double ml = margin != null ? numOf(margin.get("left"), 10) : numOf(template.get("marginLeft"), 10);

        Map<String, Object> headerCfg = (Map<String, Object>) sections.get("pageHeader");
        Map<String, Object> docCfg = (Map<String, Object>) sections.get("docHeader");
        Map<String, Object> itemsCfg = (Map<String, Object>) sections.get("items");
        Map<String, Object> summaryCfg = (Map<String, Object>) sections.get("summary");
        Map<String, Object> footerCfg = (Map<String, Object>) sections.get("pageFooter");

        // ── 明细数据 ──
        List<Map<String, Object>> allRows = new ArrayList<>();
        if (itemsCfg != null) {
            String itemsField = str(itemsCfg.get("field"), "items");
            Object raw = data.get(itemsField);
            if (raw instanceof List) {
                for (Object o : (List<Object>) raw) {
                    if (o instanceof Map) {
                        allRows.add((Map<String, Object>) o);
                    }
                }
            }
        }
        boolean emptyWhenNoData = itemsCfg == null || !Boolean.FALSE.equals(itemsCfg.get("emptyWhenNoData"));
        boolean renderTable = itemsCfg != null && !itemsCfg.isEmpty()
                && (!allRows.isEmpty() || !emptyWhenNoData);

        // ── 每页行数：>0 用模板给的（套打），=0 按纸高自动估算 ──
        int rowsPerPage = itemsCfg != null ? intOf(itemsCfg.get("rowsPerPage"), 0) : 0;
        if (rowsPerPage <= 0) {
            double usable = pageH - mt - mb;
            double rowH = itemsCfg != null ? numOf(itemsCfg.get("rowHeight"), 8) : 8;
            double occupied = flowHeight(headerCfg) + gridHeight(docCfg) + flowHeight(footerCfg)
                    + gridHeight(summaryCfg) + 8 /* 表头 */;
            int auto = (int) Math.floor((usable - occupied) / Math.max(4, rowH));
            rowsPerPage = Math.max(1, Math.min(auto, 200));
        }
        boolean fillBlank = itemsCfg != null && Boolean.TRUE.equals(itemsCfg.get("fillBlankRows"));

        List<List<Map<String, Object>>> pages = applyRowPaging(allRows, rowsPerPage, fillBlank);
        int totalPages = pages.size();

        boolean headerRepeat = headerCfg == null || !Boolean.FALSE.equals(headerCfg.get("repeat"));
        boolean footerRepeat = footerCfg == null || !Boolean.FALSE.equals(footerCfg.get("repeat"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>");
        html.append("@page{size:").append(fmt(pageW)).append("mm ").append(fmt(pageH)).append("mm;margin:0;}");
        html.append("*{margin:0;padding:0;box-sizing:border-box;}");
        html.append("body{font-family:'SimSun','宋体','Microsoft YaHei',sans-serif;font-size:12px;color:#000;background:#fff;}");
        html.append(".print-page{background:#fff;font-size:12px;}");
        html.append("table{border-collapse:collapse;}");
        html.append("td,th{font-size:12px;vertical-align:middle;word-break:break-all;}");
        html.append("thead{display:table-header-group;}");
        html.append("</style></head><body>");

        for (int p = 0; p < totalPages; p++) {
            boolean first = p == 0;
            boolean last = p == totalPages - 1;
            html.append("<div class=\"print-page\" style=\"width:").append(fmt(pageW)).append("mm;");
            html.append("height:").append(fmt(pageH)).append("mm;");
            html.append("padding:").append(fmt(mt)).append("mm ").append(fmt(mr)).append("mm ")
                    .append(fmt(mb)).append("mm ").append(fmt(ml)).append("mm;");
            html.append("position:relative;overflow:hidden;");
            if (!last) {
                html.append("page-break-after:always;");
            }
            html.append("\">");

            if (headerCfg != null && !headerCfg.isEmpty() && (first || headerRepeat)) {
                html.append("<div style=\"position:relative;")
                        .append(headerCfg.get("height") != null
                                ? "height:" + fmt(numOf(headerCfg.get("height"), 0)) + "mm;overflow:hidden;" : "")
                        .append("\">")
                        .append(renderFlowSection(headerCfg, data, p + 1, totalPages))
                        .append("</div>");
            }
            if (docCfg != null && !docCfg.isEmpty() && first) {
                html.append(renderFieldGrid(docCfg, data));
            }
            if (renderTable) {
                html.append(renderItemsTable(itemsCfg, resolveColumns(itemsCfg, data),
                        pages.get(p), allRows, last));
            }
            if (summaryCfg != null && !summaryCfg.isEmpty() && last) {
                html.append(renderFieldGrid(summaryCfg, data));
            }
            if (footerCfg != null && !footerCfg.isEmpty() && (first || footerRepeat)) {
                html.append(renderPageFooter(footerCfg, data, p + 1, totalPages));
            }
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /** 单据头 / 合计区：字段按栅格自动排布，不用算坐标 */
    @SuppressWarnings("unchecked")
    private String renderFieldGrid(Map<String, Object> cfg, Map<String, Object> data) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) cfg.getOrDefault("items", new ArrayList<>());
        if (items.isEmpty()) {
            return "";
        }
        int columns = Math.max(1, intOf(cfg.get("columns"), 12));
        int labelSpan = Math.max(1, intOf(cfg.get("labelSpan"), 1));
        boolean border = !Boolean.FALSE.equals(cfg.get("border"));
        String bd = border ? "border:1px solid #333;" : "";
        String base = bd + "padding:3px 5px;";

        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"width:100%;table-layout:fixed;border-collapse:collapse;")
                .append(bd).append("\">");

        for (List<Map<String, Object>> rowItems : layoutGrid(items, columns)) {
            int used = rowItems.stream().mapToInt(it -> Math.max(1, intOf(it.get("span"), 1))).sum();
            sb.append("<tr>");
            for (Map<String, Object> it : rowItems) {
                int span = Math.max(1, intOf(it.get("span"), 1));
                String type = str(it.get("type"), "field");
                String extra = styleCss(styleOf(it));
                if ("label".equals(type)) {
                    sb.append("<td colspan=\"").append(span).append("\" style=\"").append(base)
                            .append(extra).append("\">")
                            .append(escapeHtml(str(it.get("content"), ""))).append("</td>");
                    continue;
                }
                int ls = Math.min(labelSpan, Math.max(1, span - 1));
                int vs = Math.max(1, span - ls);
                String label = str(it.get("label"), "");
                if (!label.isEmpty()) {
                    sb.append("<td colspan=\"").append(ls).append("\" style=\"").append(base)
                            .append("text-align:right;white-space:nowrap;color:#333;\">")
                            .append(escapeHtml(label)).append("</td>");
                } else {
                    vs += ls;
                }
                String value = cellValue(it, data);
                sb.append("<td colspan=\"").append(vs).append("\" style=\"").append(base)
                        .append(extra).append("\">")
                        .append(value.isEmpty() ? "&nbsp;" : escapeHtml(value)).append("</td>");
            }
            int filler = columns - used;
            if (filler > 0) {
                sb.append("<td colspan=\"").append(filler).append("\" style=\"").append(bd)
                        .append("\">&nbsp;</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * 明细表的列从哪来。
     *
     * <p>两种模式：</p>
     * <ul>
     *   <li><b>模板定列</b>（默认）—— 用 {@code items.columns}，单据类模板都走这条；</li>
     *   <li><b>数据定列</b>（{@code items.columnsFrom: "columns"}）—— 列定义从**数据**里取，
     *       给"结果集打印"用：列表页打的是"我当前看到的这批数据 + 当前列配置"，
     *       列是运行时才知道的，模板里写不死。</li>
     * </ul>
     *
     * <p>数据里的列定义兼容两种写法（用哪个都行，混着也行）：</p>
     * <pre>
     *   { "key": "productName", "title": "商品名称" }
     *   { "field": "productName", "header": "商品名称", "width": "120px", "align": "right", "digits": 2 }
     * </pre>
     * <p>数据里没给列（或给了空数组）时**回落到模板写死的 columns**，
     * 不让整张表凭空消失 —— 宁可少列也不要一片空白。</p>
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveColumns(Map<String, Object> cfg, Map<String, Object> data) {
        String fromKey = cfg == null ? "" : str(cfg.get("columnsFrom"), "");
        if (!fromKey.isEmpty() && data != null) {
            Object raw = data.get(fromKey);
            if (raw instanceof List) {
                List<Map<String, Object>> resolved = new ArrayList<>();
                for (Object element : (List<Object>) raw) {
                    if (!(element instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> source = (Map<String, Object>) element;
                    Object field = source.containsKey("field") ? source.get("field") : source.get("key");
                    if (field == null || String.valueOf(field).isEmpty()) {
                        continue;
                    }
                    Object header = source.containsKey("header") ? source.get("header") : source.get("title");
                    Map<String, Object> column = new LinkedHashMap<>();
                    column.put("field", String.valueOf(field));
                    column.put("header", header == null ? String.valueOf(field) : String.valueOf(header));
                    // 只在数据真的给了的时候才带上，免得覆盖 renderItemsTable 里的默认行为
                    for (String key : new String[]{"width", "align", "digits", "agg"}) {
                        Object value = source.get(key);
                        if (value == null) {
                            continue;
                        }
                        // 前端的列配置里 width 是**数字**（110），直接当 CSS 用会得到无效的 `width:110`
                        column.put(key, "width".equals(key) ? withPx(value) : value);
                    }
                    resolved.add(column);
                }
                if (!resolved.isEmpty()) {
                    return resolved;
                }
            }
            log.warn("items.columnsFrom={} 的数据里没有可用列，回落到模板里的 columns", fromKey);
        }
        if (cfg == null) {
            return new ArrayList<>();
        }
        return (List<Map<String, Object>>) cfg.getOrDefault("columns", new ArrayList<>());
    }

    /**
     * 明细表：&lt;thead&gt; 每页重印 + &lt;tbody&gt; 本页行 + &lt;tfoot&gt; 末页合计。
     *
     * @param columns  本页要画的列（可能来自模板 items.columns，也可能来自数据 items.columnsFrom）
     * @param rows     本页要画的行（可能含 __blank 补空行）
     * @param allRows  全部行 —— 合计必须按全量算，不能只算本页
     * @param showTotal 是否输出合计行（只在末页 true）
     */
    /** 宽度没带单位时补 px（前端列宽是纯数字，模板里则习惯写 "120px"） */
    private String withPx(Object width) {
        if (width instanceof Number) {
            return fmt(((Number) width).doubleValue()) + "px";
        }
        String text = String.valueOf(width).trim();
        if (text.isEmpty()) {
            return text;
        }
        // 已经是 px/em/%/auto 之类就原样用
        return text.matches("^-?\\d+(\\.\\d+)?$") ? text + "px" : text;
    }

    @SuppressWarnings("unchecked")
    private String renderItemsTable(Map<String, Object> cfg, List<Map<String, Object>> columns,
                                    List<Map<String, Object>> rows,
                                    List<Map<String, Object>> allRows, boolean showTotal) {
        if (columns.isEmpty()) {
            return "";
        }
        boolean border = !Boolean.FALSE.equals(cfg.get("border"));
        String bd = border ? "border:1px solid #333;" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"width:100%;table-layout:fixed;border-collapse:collapse;")
                .append(bd).append("\">");

        sb.append("<colgroup>");
        for (Map<String, Object> col : columns) {
            String w = str(col.get("width"), "");
            sb.append("<col");
            if (!w.isEmpty() && !"auto".equalsIgnoreCase(w)) {
                sb.append(" style=\"width:").append(w).append("\"");
            }
            sb.append(">");
        }
        sb.append("</colgroup>");

        boolean repeatHeader = !Boolean.FALSE.equals(cfg.get("repeatHeader"));
        if (repeatHeader) {
            sb.append("<thead><tr>");
            for (Map<String, Object> col : columns) {
                sb.append("<th style=\"").append(bd).append("padding:4px 5px;font-weight:bold;")
                        .append("background:#f2f2f2;text-align:").append(alignOf(col))
                        .append(";white-space:nowrap;\">")
                        .append(escapeHtml(str(col.get("header"), ""))).append("</th>");
            }
            sb.append("</tr></thead>");
        }

        sb.append("<tbody>");
        for (Map<String, Object> row : rows) {
            boolean blank = Boolean.TRUE.equals(row.get("__blank"));
            sb.append("<tr>");
            for (Map<String, Object> col : columns) {
                String field = str(col.get("field"), "");
                String v = blank ? "" : cellValue(col, row);
                sb.append("<td style=\"").append(bd).append("padding:3px 5px;text-align:")
                        .append(alignOf(col)).append("\">")
                        .append(v.isEmpty() ? "&nbsp;" : escapeHtml(v)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody>");

        boolean hasAgg = columns.stream().anyMatch(c -> "sum".equals(c.get("agg")));
        if (showTotal && (hasAgg || Boolean.TRUE.equals(cfg.get("showTotal")))) {
            String totalLabel = str(cfg.get("totalLabel"), "合计");
            boolean labelPlaced = false;
            sb.append("<tfoot style=\"page-break-inside:avoid;\"><tr>");
            for (Map<String, Object> col : columns) {
                String text;
                if ("sum".equals(col.get("agg"))) {
                    String field = str(col.get("field"), "");
                    text = formatNumber(sumOf(allRows, field), intOf(col.get("digits"), 2));
                } else if (!labelPlaced) {
                    text = totalLabel;
                    labelPlaced = true;
                } else {
                    text = "";
                }
                sb.append("<td style=\"").append(bd).append("padding:4px 5px;font-weight:bold;text-align:")
                        .append(alignOf(col)).append("\">")
                        .append(escapeHtml(text)).append("</td>");
            }
            sb.append("</tr></tfoot>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    /** 页眉 / 页脚：纵向流式排布（页码、签字栏都在这） */
    @SuppressWarnings("unchecked")
    private String renderFlowSection(Map<String, Object> cfg, Map<String, Object> data,
                                     int pageNo, int totalPages) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) cfg.getOrDefault("items", new ArrayList<>());
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> it : items) {
            String type = str(it.get("type"), "label");
            String css = styleCss(styleOf(it));
            switch (type) {
                case "pageNumber": {
                    String pat = str(it.get("format"), "第{0}页 / 共{1}页");
                    String text = pat.replace("{0}", String.valueOf(pageNo))
                            .replace("{1}", String.valueOf(totalPages));
                    sb.append("<div style=\"text-align:center;").append(css).append("\">")
                            .append(escapeHtml(text)).append("</div>");
                    break;
                }
                case "field": {
                    // 没给 label 时不打那个「：」—— 结果集打印的标题就是「只用值、不要标签」的用法
                    String label = str(it.get("label"), "");
                    sb.append("<div style=\"").append(css).append("\">");
                    if (!label.isEmpty()) {
                        sb.append(escapeHtml(label)).append("：");
                    }
                    sb.append(escapeHtml(cellValue(it, data))).append("</div>");
                    break;
                }
                default: {
                    String content = str(it.get("content"), "");
                    if (content.isEmpty()) {
                        sb.append("<div style=\"height:6mm;\"></div>");
                        break;
                    }
                    sb.append("<div style=\"").append(css).append("\">")
                            .append(escapeHtml(content)).append("</div>");
                }
            }
        }
        return sb.toString();
    }

    /** 页脚默认吸在纸底（签字栏必须在固定位置），position=flow 时跟着内容走 */
    private String renderPageFooter(Map<String, Object> cfg, Map<String, Object> data,
                                    int pageNo, int totalPages) {
        boolean bottom = !"flow".equalsIgnoreCase(str(cfg.get("position"), "bottom"));
        String css = bottom ? "position:absolute;left:0;right:0;bottom:0;" : "margin-top:6mm;";
        return "<div style=\"" + css + "\">" + renderFlowSection(cfg, data, pageNo, totalPages) + "</div>";
    }

    /** 固定行数切页；fillBlankRows 时末页补空行（套打用，纸上格子是印好的） */
    private List<List<Map<String, Object>>> applyRowPaging(List<Map<String, Object>> rows,
                                                            int rowsPerPage, boolean fillBlankRows) {
        List<List<Map<String, Object>>> pages = new ArrayList<>();
        if (rowsPerPage <= 0) {
            pages.add(new ArrayList<>(rows));
            return pages;
        }
        if (rows.isEmpty()) {
            pages.add(fillBlankRows ? blankRows(rowsPerPage) : new ArrayList<>());
            return pages;
        }
        for (int i = 0; i < rows.size(); i += rowsPerPage) {
            List<Map<String, Object>> chunk = new ArrayList<>(
                    rows.subList(i, Math.min(rows.size(), i + rowsPerPage)));
            if (fillBlankRows) {
                while (chunk.size() < rowsPerPage) {
                    chunk.add(blankRow());
                }
            }
            pages.add(chunk);
        }
        return pages;
    }

    /** 按 span 把栅格元素折行 —— 只算行数/行内容，不产出 HTML */
    private List<List<Map<String, Object>>> layoutGrid(List<Map<String, Object>> items, int columns) {
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        List<Map<String, Object>> cur = new ArrayList<>();
        int used = 0;
        for (Map<String, Object> it : items) {
            int span = Math.max(1, intOf(it.get("span"), 1));
            if (used + span > columns && !cur.isEmpty()) {
                rows.add(cur);
                cur = new ArrayList<>();
                used = 0;
            }
            cur.add(it);
            used += span;
        }
        if (!cur.isEmpty()) {
            rows.add(cur);
        }
        return rows;
    }

    /** 栅格区估算高度（mm）：行数 × 7mm + 边框 */
    @SuppressWarnings("unchecked")
    private double gridHeight(Map<String, Object> cfg) {
        if (cfg == null || cfg.isEmpty()) {
            return 0;
        }
        List<Map<String, Object>> items = (List<Map<String, Object>>) cfg.getOrDefault("items", new ArrayList<>());
        if (items.isEmpty()) {
            return 0;
        }
        int columns = Math.max(1, intOf(cfg.get("columns"), 12));
        return layoutGrid(items, columns).size() * 7.0 + 2;
    }

    /** 页眉页脚估算高度（mm）：给了 height 就照用，否则按行数估 */
    @SuppressWarnings("unchecked")
    private double flowHeight(Map<String, Object> cfg) {
        if (cfg == null || cfg.isEmpty()) {
            return 0;
        }
        if (cfg.get("height") != null) {
            return numOf(cfg.get("height"), 0);
        }
        List<Map<String, Object>> items = (List<Map<String, Object>>) cfg.getOrDefault("items", new ArrayList<>());
        return items.isEmpty() ? 0 : items.size() * 7.0 + 2;
    }

    private List<Map<String, Object>> blankRows(int n) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(blankRow());
        }
        return rows;
    }

    private Map<String, Object> blankRow() {
        Map<String, Object> m = new HashMap<>();
        m.put("__blank", Boolean.TRUE);
        return m;
    }

    /** 取字段值并按 formatConfig / digits 格式化 */
    @SuppressWarnings("unchecked")
    private String cellValue(Map<String, Object> item, Map<String, Object> data) {
        String field = str(item.get("field"), "");
        Object raw = field.isEmpty() ? null : data.get(field);
        Map<String, Object> fc = (Map<String, Object>) item.get("formatConfig");
        if (fc != null && !fc.isEmpty()) {
            return format(raw, fc);
        }
        if (item.get("digits") != null) {
            return formatNumber(raw, intOf(item.get("digits"), 0));
        }
        return raw != null ? raw.toString() : "";
    }

    /** 全量求和（合计行用，不能只算本页） */
    private BigDecimal sumOf(List<Map<String, Object>> rows, String field) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object v = row.get(field);
            if (v == null) {
                continue;
            }
            try {
                sum = sum.add(new BigDecimal(v.toString()));
            } catch (NumberFormatException ignore) {
                // 非数值列不参与合计
            }
        }
        return sum;
    }

    /**
     * 人民币金额大写。
     * 1005.00 → 壹仟零伍元整 · 1234.56 → 壹仟贰佰叁拾肆元伍角陆分
     * 0.05 → 伍分 · 1234.50 → 壹仟贰佰叁拾肆元伍角整
     *
     * 超出 long 能表示的范围（≥ 1e15 元）直接退回数字串，不硬算。
     */
    private String rmbUpper(Object value) {
        if (value == null) {
            return "";
        }
        BigDecimal bd;
        try {
            bd = new BigDecimal(value.toString().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return value.toString();
        }
        boolean negative = bd.signum() < 0;
        bd = bd.abs().setScale(2, java.math.RoundingMode.HALF_UP);
        if (bd.compareTo(new BigDecimal("999999999999999.99")) > 0) {
            return (negative ? "-" : "") + bd.toPlainString() + "元";
        }
        BigInteger cents = bd.unscaledValue();
        int fen = cents.mod(BigInteger.TEN).intValue();
        int jiao = cents.divide(BigInteger.TEN).mod(BigInteger.TEN).intValue();
        long yuan = bd.toBigInteger().longValueExact();

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append("负");
        }
        if (yuan == 0 && jiao == 0 && fen == 0) {
            return sb.append("零元整").toString();
        }
        if (yuan > 0) {
            sb.append(intToUpper(yuan)).append("元");
        }
        if (jiao == 0 && fen == 0) {
            sb.append("整");
        } else if (jiao > 0 && fen == 0) {
            sb.append(RMB_DIGITS[jiao]).append("角整");
        } else if (jiao == 0) {
            if (yuan > 0) {
                sb.append("零");
            }
            sb.append(RMB_DIGITS[fen]).append("分");
        } else {
            sb.append(RMB_DIGITS[jiao]).append("角").append(RMB_DIGITS[fen]).append("分");
        }
        return sb.toString();
    }

    /**
     * 整数转中文大写。单位按「4 位一节」拼：
     *   pos%4 → ""/拾/佰/仟 ；pos/4 → ""/万/亿/万亿
     * 连续零只出一个「零」，且不在节权前重复出零。
     */
    private String intToUpper(long n) {
        if (n == 0) {
            return RMB_DIGITS[0];
        }
        String digits = Long.toString(n);
        int len = digits.length();
        StringBuilder sb = new StringBuilder();
        boolean zeroPending = false;
        for (int i = 0; i < len; i++) {
            int d = digits.charAt(i) - '0';
            int pos = len - 1 - i;
            if (d == 0) {
                zeroPending = sb.length() > 0;
                continue;
            }
            if (zeroPending) {
                sb.append(RMB_DIGITS[0]);
                zeroPending = false;
            }
            sb.append(RMB_DIGITS[d]).append(RMB_SUB[pos % 4]).append(RMB_SEC[pos / 4]);
        }
        return sb.toString();
    }

    /** 千分位 + 固定小数位（合计数值走这里，保证 84.00 不会退化成 84） */
    private String formatNumber(Object value, int digits) {
        if (value == null) {
            return "";
        }
        try {
            BigDecimal bd = new BigDecimal(value.toString()).setScale(digits, java.math.RoundingMode.HALF_UP);
            java.text.DecimalFormat df = new java.text.DecimalFormat();
            df.setDecimalFormatSymbols(java.text.DecimalFormatSymbols.getInstance(Locale.CHINA));
            df.setGroupingUsed(true);
            df.setMinimumFractionDigits(digits);
            df.setMaximumFractionDigits(digits);
            return df.format(bd);
        } catch (Exception e) {
            return value.toString();
        }
    }

    /** 纸张名义尺寸（mm），模板不给 pageWidth/pageHeight 时兜底 */
    private double[] paperMm(String paperSize) {
        switch (paperSize == null ? "" : paperSize.toUpperCase()) {
            case "A3": return new double[]{297, 420};
            case "A5": return new double[]{148, 210};
            case "A6": return new double[]{105, 148};
            case "B5": return new double[]{176, 250};
            case "LETTER": return new double[]{216, 279};
            case "A4":
            default: return new double[]{210, 297};
        }
    }

    private String alignOf(Map<String, Object> col) {
        String a = str(col.get("align"), "left");
        return "center".equals(a) || "right".equals(a) ? a : "left";
    }

    /** 行内区块只认这几个 style key，其余一律忽略（避免模板塞任意 CSS） */
    private String styleCss(Map<String, Object> st) {
        if (st == null || st.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendStyleProp(sb, st, "fontSize", "font-size", "px");
        appendStyleProp(sb, st, "fontWeight", "font-weight");
        appendStyleProp(sb, st, "fontFamily", "font-family");
        appendStyleProp(sb, st, "color", "color");
        appendStyleProp(sb, st, "backgroundColor", "background-color");
        appendStyleProp(sb, st, "textAlign", "text-align");
        appendStyleProp(sb, st, "border", "border");
        appendStyleProp(sb, st, "padding", "padding");
        appendStyleProp(sb, st, "fontStyle", "font-style");
        appendStyleProp(sb, st, "lineHeight", "line-height");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> styleOf(Map<String, Object> item) {
        Object st = item.get("style");
        return st instanceof Map ? (Map<String, Object>) st : new HashMap<>();
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.toString();
        return s.isEmpty() ? fallback : s;
    }

    private int intOf(Object v, int fallback) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v == null) {
            return fallback;
        }
        try {
            return (int) Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double numOf(Object v, double fallback) {
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** mm 数值转字符串：去掉多余的 .0，避免 210.0mm 这种脏输出 */
    private String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(Math.round(v * 100) / 100.0);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
