package cn.aiedge.erp.printing.support;

import cn.aiedge.erp.printing.entity.SetPrintConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 「打印设置作用到打印数据」的服务端口径测试。
 *
 * <p>这套规则是从前端 {@code printBehavior.ts} 移植过来的，移植最容易在
 * 「金额不参与格式化」「零只出一个」「只注入明细行不注入单据头」这几个边界上走样，
 * 所以逐条钉住。</p>
 */
@DisplayName("打印设置加工：小数位 + 打印内容")
class PrintBehaviorApplierTest {

    private static SetPrintConfig config(Integer decimalEnabled, Integer qtyDecimal, Integer priceDecimal,
                                         String printContent) {
        SetPrintConfig cfg = new SetPrintConfig();
        cfg.setDecimalEnabled(decimalEnabled);
        cfg.setQtyDecimal(qtyDecimal);
        cfg.setPriceDecimal(priceDecimal);
        cfg.setPrintContent(printContent);
        return cfg;
    }

    /** 造一份「单据头 + 两行明细」的最小打印数据 */
    private static Map<String, Object> sampleData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", "XSDD-20260926-001");
        data.put("totalQuantity", 3);
        data.put("billAmount", 99.9);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("lineNo", 1);
        row1.put("quantity", 1);
        row1.put("unitPrice", 20);
        row1.put("amount", 20.0);
        row1.put("discountAmount", 1.5);
        row1.put("batchCode", "P20240101");
        row1.put("productionDate", "2024-01-01");
        row1.put("expiryDate", "2025-01-01");
        items.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lineNo", 2);
        row2.put("quantity", 2);
        row2.put("unitPrice", 39.95);
        row2.put("amount", 79.9);
        row2.put("remark", "-");
        items.add(row2);

        data.put("items", items);
        return data;
    }

    @Test
    @DisplayName("小数位开启：数量/单价定长，金额类字段一律不碰")
    void formatsQuantityAndPriceOnly() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(1, 2, 1, null));

        List<Map<String, Object>> items = items(data);
        assertEquals("1.00", items.get(0).get("quantity"));
        assertEquals("20.0", items.get(0).get("unitPrice"));
        // 金额类字段即使名字里带 price/amount 也不格式化
        assertEquals(20.0, items.get(0).get("amount"));
        assertEquals(1.5, items.get(0).get("discountAmount"));
        // 单据头的 totalQuantity 也属数量口径
        assertEquals("3.00", data.get("totalQuantity"));
        assertEquals(99.9, data.get("billAmount"));
    }

    @Test
    @DisplayName("小数位关闭：一个字段都不动（含非数值原样保留）")
    void ignoresDecimalsWhenDisabled() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(0, 2, 2, null));

        List<Map<String, Object>> items = items(data);
        assertEquals(1, items.get(0).get("quantity"));
        assertEquals(20, items.get(0).get("unitPrice"));
        assertEquals("-", items.get(1).get("remark"));
    }

    @Test
    @DisplayName("打印内容「批号 *数量」：只注入明细行，单据头不注入")
    void injectsBatchTextIntoRowsOnly() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(0, null, null, "批号 *数量"));

        List<Map<String, Object>> items = items(data);
        assertEquals("P20240101 *1", items.get(0).get("batchEffectiveText"));
        // 第二行没有批号 → 不注入，避免污染渲染数据
        assertFalse(items.get(1).containsKey("batchEffectiveText"));
        // 单据头不注入批次字段，但要带上选项文案供表头引用
        assertFalse(data.containsKey("batchEffectiveText"));
        assertEquals("批号 *数量", data.get("printContentText"));
    }

    @Test
    @DisplayName("打印内容「批号 生产日期~到期日期 *数量」：起止成段，缺一端不打出半截波浪号")
    void buildsBatchTextWithDateRange() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(0, null, null, "批号 生产日期~到期日期 *数量"));

        List<Map<String, Object>> items = items(data);
        assertEquals("P20240101 2024-01-01~2025-01-01 *1", items.get(0).get("batchEffectiveText"));
        assertFalse(items.get(1).containsKey("batchEffectiveText"));
    }

    @Test
    @DisplayName("小数位与打印内容叠加：数量先格式化，再拼进批次文本")
    void appliesDecimalsBeforeBatchText() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(1, 2, null, "批号 *数量"));
        assertEquals("P20240101 *1.00", items(data).get(0).get("batchEffectiveText"));
    }

    @Test
    @DisplayName("两项都关闭：数据原样返回，不新增任何字段")
    void noopWhenEverythingOff() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(0, 2, 2, ""));

        assertEquals(4, data.size());
        assertNull(data.get("printContentText"));
        assertFalse(items(data).get(0).containsKey("batchEffectiveText"));
    }

    @Test
    @DisplayName("配置为 null 时不炸也不改数据")
    void toleratesNullConfig() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, null);
        assertEquals(4, data.size());
    }

    @Test
    @DisplayName("未知的打印内容取值不注入（库里可能有历史脏值）")
    void ignoresUnknownPrintContent() {
        Map<String, Object> data = sampleData();
        PrintBehaviorApplier.apply(data, config(0, null, null, "不存在的选项"));
        assertFalse(items(data).get(0).containsKey("batchEffectiveText"));
        assertFalse(data.containsKey("printContentText"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> data) {
        return (List<Map<String, Object>>) data.get("items");
    }
}
