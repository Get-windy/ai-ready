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

    // ══════════════════════════════════════════════════════════════
    // v2 分区流式模板
    // ══════════════════════════════════════════════════════════════

    private static int count(String s, String sub) {
        int c = 0;
        for (int i = 0; (i = s.indexOf(sub, i)) >= 0; i += sub.length()) {
            c++;
        }
        return c;
    }

    private static String sectionsTemplate(int rowsPerPage, boolean fillBlankRows) {
        return """
                {
                  "version": 2,
                  "paperSize": "A4",
                  "freeLayout": false,
                  "margin": {"top": 10, "right": 10, "bottom": 10, "left": 10},
                  "sections": {
                    "pageHeader": {"repeat": true, "height": 20, "items": [
                      {"type": "label", "content": "甘肃干饭郎科贸有限公司",
                       "style": {"textAlign": "center", "fontSize": "16px", "fontWeight": "bold"}}]},
                    "docHeader": {"columns": 12, "items": [
                      {"type": "field", "label": "单号", "field": "orderNo", "span": 4},
                      {"type": "field", "label": "日期", "field": "orderDate", "span": 4},
                      {"type": "field", "label": "客户", "field": "customerName", "span": 4}]},
                    "items": {"repeatHeader": true, "rowsPerPage": %d, "fillBlankRows": %b, "showTotal": true,
                      "columns": [
                        {"header": "行号", "field": "lineNo", "width": "50px", "align": "center"},
                        {"header": "商品名称", "field": "productName"},
                        {"header": "数量", "field": "quantity", "width": "80px", "align": "right",
                         "agg": "sum", "digits": 0},
                        {"header": "金额", "field": "lineAmount", "width": "100px", "align": "right",
                         "agg": "sum", "digits": 2}]},
                    "summary": {"items": [
                      {"type": "field", "label": "金额合计", "field": "billAmount"},
                      {"type": "field", "label": "金额大写", "field": "billAmount",
                       "formatConfig": {"type": "rmbUpper"}}]},
                    "pageFooter": {"repeat": true, "items": [
                      {"type": "label", "content": "制单：______ 审核：______ 签收：______"},
                      {"type": "pageNumber", "format": "第{0}页 / 共{1}页"}]}
                  }
                }
                """.formatted(rowsPerPage, fillBlankRows);
    }

    private static String saleOrderData(int rowCount) {
        StringBuilder items = new StringBuilder();
        for (int i = 1; i <= rowCount; i++) {
            if (i > 1) items.append(',');
            items.append("{\"lineNo\":").append(i)
                    .append(",\"productName\":\"商品").append(i).append("\"")
                    .append(",\"quantity\":1,\"lineAmount\":8.40}");
        }
        return "{\"orderNo\":\"XSDD-20260926-001\",\"orderDate\":\"2026-09-26\","
                + "\"customerName\":\"兰州牛肉面馆\",\"billAmount\":84.00,\"items\":[" + items + "]}";
    }

    @Test
    @DisplayName("v2 分区模板：合计求和 / 表头每页重印 / 大写金额 / 页码")
    void testSectionsRender() {
        String html = formatEngine.renderToHtml(sectionsTemplate(8, false), saleOrderData(10));

        // 10 条数据、每页 8 行 → 2 页，每页各带一份表头
        assertEquals(2, count(html, "<thead>"), "应分成 2 页且每页重印表头");
        assertEquals(2, count(html, "class=\"print-page\""), "应输出 2 个页块");

        // 合计：84.00 是明细求和（不是单据头字段），必须落在 tfoot 里
        assertTrue(html.contains("<tfoot"), "应有合计区");
        assertTrue(html.contains("合计"), "应有合计字样");
        assertTrue(html.contains("84.00"), "金额合计应为 84.00，实际 HTML 未命中");
        assertTrue(html.contains(">10</td>"), "数量合计应为 10");

        // 单据头与页眉页脚
        assertTrue(html.contains("XSDD-20260926-001"));
        assertTrue(html.contains("甘肃干饭郎科贸有限公司"), "页眉每页重印");
        assertTrue(html.contains("捌拾肆元整"), "金额大写");
        assertTrue(html.contains("第1页 / 共2页"));
        assertTrue(html.contains("第2页 / 共2页"));
    }

    @Test
    @DisplayName("v2 套打：固定行数切页 + 末页补空行")
    void testFixedRowsPerPageWithBlankFill() {
        String html = formatEngine.renderToHtml(sectionsTemplate(3, true), saleOrderData(5));

        String[] parts = html.split("<tbody>");
        assertEquals(3, parts.length, "5 条 / 每页 3 行 → 2 页");
        String page2 = parts[2].split("</tbody>")[0];
        assertEquals(3, count(page2, "<tr>"), "第 2 页应补足 3 行");
        // 第 2 页实有 2 行数据，补 1 空行 → 该行 4 列全是 &nbsp;
        assertEquals(4, count(page2, "&nbsp;"), "第 2 页应补 1 个空行");
    }

    @Test
    @DisplayName("v2 自动分页：rowsPerPage=0 时按纸高切页")
    void testAutoPaging() {
        String html = formatEngine.renderToHtml(sectionsTemplate(0, false), saleOrderData(300));

        int pages = count(html, "class=\"print-page\"");
        assertTrue(pages > 1, "300 条数据 A4 自动分页应多于 1 页，实际 " + pages);
        assertEquals(pages, count(html, "<thead>"), "每页都应有一份表头");
        assertTrue(html.contains("第1页 / 共" + pages + "页"));
    }

    @Test
    @DisplayName("v2 无明细数据时整表不打印")
    void testEmptyWhenNoData() {
        String html = formatEngine.renderToHtml(sectionsTemplate(8, false), saleOrderData(0));
        assertEquals(0, count(html, "<thead>"), "emptyWhenNoData 默认 true，不应出现明细表");
        assertTrue(html.contains("甘肃干饭郎科贸有限公司"), "页眉仍应打印");
        assertEquals(1, count(html, "class=\"print-page\""));
    }

    @Test
    @DisplayName("金额大写：零 / 整 / 角分 / 超大数")
    void testRmbUpper() {
        Map<String, Object> cfg = Map.of("type", "rmbUpper");
        assertEquals("壹仟贰佰叁拾肆元伍角陆分", formatEngine.format(1234.56, cfg));
        assertEquals("壹仟贰佰叁拾肆元整", formatEngine.format(1234.00, cfg));
        assertEquals("壹仟贰佰叁拾肆元伍角整", formatEngine.format(1234.50, cfg));
        assertEquals("壹仟零伍元整", formatEngine.format(1005, cfg), "中间连续零只出一个「零」");
        assertEquals("壹拾元整", formatEngine.format(10, cfg));
        assertEquals("壹仟万元整", formatEngine.format(10000000, cfg), "节权不能丢");
        assertEquals("壹亿零壹元整", formatEngine.format(100000001, cfg));
        assertEquals("零元整", formatEngine.format(0, cfg));
        assertEquals("伍分", formatEngine.format(0.05, cfg));
        assertEquals("", formatEngine.format(null, cfg));
    }

    @Test
    @DisplayName("日期格式化：LocalDate 与 LocalDateTime 都要能按 pattern 输出")
    void testFormatDate() {
        Map<String, Object> cfg = Map.of("type", "date", "pattern", "yyyy-MM-dd");
        // 实体里有的字段是 LocalDateTime（序列化带 T），有的就是 LocalDate —— 两种都得认，
        // 否则单据上会打出 `2026-09-03T00:00:00`
        assertEquals("2026-09-03", formatEngine.format("2026-09-03T00:00:00", cfg));
        assertEquals("2026-09-02", formatEngine.format("2026-09-02", cfg));
        // 不是日期就原样返回，不制造空值
        assertEquals("待定", formatEngine.format("待定", cfg));
        assertEquals("", formatEngine.format(null, cfg));
    }

    /** 结果集打印模板：列不写死，由数据给（items.columnsFrom） */
    private static String resultSetTemplate() {
        return """
                {
                  "version": 2,
                  "paperSize": "A4",
                  "sections": {
                    "pageHeader": {"items": [
                      {"type": "field", "field": "title",
                       "style": {"textAlign": "center", "fontSize": "16px", "fontWeight": "bold"}}]},
                    "items": {"field": "rows", "columnsFrom": "columns", "repeatHeader": true,
                      "columns": [{"header": "占位列", "field": "placeholder"}]},
                    "pageFooter": {"items": [{"type": "pageNumber", "format": "第{0}页 / 共{1}页"}]}
                  }
                }
                """;
    }

    @Test
    @DisplayName("v2 结果集：列由数据给（{key,title}），模板里的占位列不被使用")
    void testColumnsFromDataKeyTitle() {
        String data = """
                {"title": "商城商品列表",
                 "columns": [{"key": "productId", "title": "商品编码"},
                             {"key": "salePrice", "title": "销售价", "align": "right", "digits": 2,
                              "width": 110}],
                 "rows": [{"productId": "P001", "salePrice": 12.5},
                          {"productId": "P002", "salePrice": 30}]}
                """;
        String html = formatEngine.renderToHtml(resultSetTemplate(), data);

        assertTrue(html.contains("商品编码"), "表头应来自数据的 key/title");
        assertTrue(html.contains("销售价"));
        assertTrue(html.contains("P001") && html.contains("P002"));
        assertTrue(html.contains("12.50"), "digits=2 应生效，实际未命中");
        // 前端列宽是纯数字，必须补成 px，否则 col style="width:110" 是无效 CSS
        assertTrue(html.contains("width:110px"), "数字宽度应归一化为 px，实际未命中");
        assertFalse(html.contains("占位列"), "数据给了列就不该再用模板里的占位列");
        // 无 label 的 field 不该打出那个「：」
        assertTrue(html.contains("商城商品列表"));
        assertFalse(html.contains("：商城商品列表"));
        assertTrue(html.contains("第1页 / 共1页"));
    }

    @Test
    @DisplayName("v2 结果集：列定义也认 {field,header} 写法")
    void testColumnsFromDataFieldHeader() {
        String data = """
                {"title": "货位设置",
                 "columns": [{"field": "productName", "header": "商品名称"},
                             {"field": "unit", "header": "单位", "align": "center"}],
                 "rows": [{"productName": "商品甲", "unit": "件"}]}
                """;
        String html = formatEngine.renderToHtml(resultSetTemplate(), data);

        assertTrue(html.contains("商品名称") && html.contains("单位"));
        assertTrue(html.contains("商品甲"));
        assertFalse(html.contains("占位列"));
    }

    @Test
    @DisplayName("v2 结果集：数据里没给列时回落到模板写死的列（不让整张表消失）")
    void testColumnsFromFallsBackToTemplate() {
        String data = """
                {"title": "商品列表", "rows": [{"placeholder": "兜底值"}]}
                """;
        String html = formatEngine.renderToHtml(resultSetTemplate(), data);

        assertTrue(html.contains("占位列"), "数据没给列时应回落模板列");
        assertTrue(html.contains("兜底值"));
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
