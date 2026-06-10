package cn.aiedge.erp.printing.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("格式化引擎测试")
class FormatEngineTest {

    private FormatEngine formatEngine;

    @BeforeEach
    void setUp() {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        ObjectMapper objectMapper = new ObjectMapper();
        formatEngine = new FormatEngineImpl(evaluator, objectMapper);
    }

    @Test
    @DisplayName("基础字段格式化：无配置返回原始字符串")
    void testFormatNoConfig() {
        String result = formatEngine.format(12345, null);
        assertEquals("12345", result);
    }

    @Test
    @DisplayName("表达式格式化：value + '元'")
    void testFormatExpression() {
        Map<String, Object> config = Map.of("type", "expression", "expression", "value + '元'");
        String result = formatEngine.format(100, config);
        assertEquals("100元", result);
    }

    @Test
    @DisplayName("数字格式化：千分位+小数位")
    void testFormatNumber() {
        Map<String, Object> config = Map.of("type", "number", "decimals", 2, "thousands", true);
        String result = formatEngine.format(1234567.89, config);
        assertEquals("1,234,567.89", result);
    }

    @Test
    @DisplayName("HTML 渲染：基础模板+简单数据")
    void testRenderToHtml() {
        String templateJson = """
                {
                    "paperSize": "A4",
                    "marginTop": 10,
                    "marginLeft": 10,
                    "components": [
                        {
                            "type": "label",
                            "x": 0, "y": 0, "w": 100, "h": 20,
                            "content": "测试标题"
                        },
                        {
                            "type": "field",
                            "x": 0, "y": 25, "w": 50, "h": 15,
                            "field": "customerName"
                        }
                    ]
                }
                """;
        String dataJson = """
                {"customerName": "测试客户"}
                """;

        String html = formatEngine.renderToHtml(templateJson, dataJson);
        assertNotNull(html);
        assertTrue(html.contains("测试标题"));
        assertTrue(html.contains("测试客户"));
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    @DisplayName("HTML 渲染：表格组件")
    void testRenderTable() {
        String templateJson = """
                {
                    "paperSize": "A5",
                    "components": [
                        {
                            "type": "table",
                            "x": 0, "y": 30, "w": 150, "h": 50,
                            "field": "items",
                            "columns": [
                                {"header": "商品", "field": "name"},
                                {"header": "数量", "field": "qty"}
                            ]
                        }
                    ]
                }
                """;
        String dataJson = """
                {"items": [
                    {"name": "商品A", "qty": 10},
                    {"name": "商品B", "qty": 20}
                ]}
                """;

        String html = formatEngine.renderToHtml(templateJson, dataJson);
        assertNotNull(html);
        assertTrue(html.contains("商品A"));
        assertTrue(html.contains("商品B"));
        assertTrue(html.contains("10"));
        assertTrue(html.contains("20"));
    }

    @Test
    @DisplayName("HTML 渲染：条码占位符")
    void testRenderBarcode() {
        String templateJson = """
                {
                    "paperSize": "A4",
                    "components": [
                        {
                            "type": "barcode",
                            "x": 0, "y": 0, "w": 60, "h": 20,
                            "field": "productCode"
                        }
                    ]
                }
                """;
        String dataJson = """
                {"productCode": "ABC-123"}
                """;

        String html = formatEngine.renderToHtml(templateJson, dataJson);
        assertNotNull(html);
        assertTrue(html.contains("ABC-123"));
        assertTrue(html.contains("条码"));
    }

    @Test
    @DisplayName("校验自定义函数")
    void testValidateFormula() {
        FormulaValidationResult result = formatEngine.validateExpression("value > 0 ? '有效' : '无效'", 100);
        assertTrue(result.isValid());
        assertEquals("有效", result.getPreviewResult());
    }

    @Test
    @DisplayName("自定义纸张渲染")
    void testCustomPaperSize() {
        String templateJson = """
                {
                    "paperSize": "CUSTOM",
                    "paperWidth": 100,
                    "paperHeight": 150,
                    "components": [
                        {"type": "label", "x": 0, "y": 0, "w": 50, "h": 10, "content": "自定义尺寸"}
                    ]
                }
                """;
        String dataJson = "{}";

        String html = formatEngine.renderToHtml(templateJson, dataJson);
        assertNotNull(html);
        assertTrue(html.contains("100mm"));
        assertTrue(html.contains("150mm"));
    }
}
