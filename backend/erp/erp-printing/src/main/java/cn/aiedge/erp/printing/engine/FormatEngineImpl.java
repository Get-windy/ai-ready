package cn.aiedge.erp.printing.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private String formatDate(Object value, Map<String, Object> config) {
        if (value == null) return "";
        // 简单的日期格式化，更复杂的由 expression 模式处理
        String pattern = (String) config.getOrDefault("pattern", "yyyy-MM-dd");
        try {
            java.time.LocalDateTime date = java.time.LocalDateTime.parse(value.toString());
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern(pattern);
            return date.format(fmt);
        } catch (Exception e) {
            return value.toString();
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

            return doRender(template, data);
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
