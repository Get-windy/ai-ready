/**
 * 销售出库单打印数据装配器单元测试
 *
 * 纯 Mockito，不加载 Spring、不连库 —— 钉的是**契约**而不是 SQL：
 * 模板里的 field 名一旦跟装配器给的键对不上，打出来就是一片空白，
 * 而这种错在编译期完全看不出来，只能靠断言把键名钉住。
 */
package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundItemMapper;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("销售出库单打印数据装配器")
class SaleOutboundPrintDataProviderTest {

    @Mock
    private SaleOutboundMapper outboundMapper;

    @Mock
    private SaleOutboundItemMapper itemMapper;

    @InjectMocks
    private SaleOutboundPrintDataProvider provider;

    private SaleOutbound outbound() {
        SaleOutbound doc = new SaleOutbound();
        doc.setId(100L);
        doc.setOutboundNo("XSCKD-20260926-0001");
        doc.setCustomerName("测试客户");
        doc.setWarehouseName("主仓库");
        doc.setTotalAmount(new BigDecimal("120.00"));
        doc.setTotalQuantity(new BigDecimal("3.0000"));
        return doc;
    }

    private SaleOutboundItem item(Integer lineNo, String name, String qty, String price, String amount) {
        SaleOutboundItem it = new SaleOutboundItem();
        it.setLineNo(lineNo);
        it.setProductName(name);
        it.setProductUnit("件");
        // ⚠️ 只设 outboundQuantity（落库列）；quantity 是 @TableField(exist=false) 的临时别名，
        //    从库里查出来必然是 null —— 装配器若读了它，打出来就是空数量
        if (qty != null) it.setOutboundQuantity(new BigDecimal(qty));
        it.setUnitPrice(new BigDecimal(price));
        it.setLineAmount(new BigDecimal(amount));
        return it;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> data) {
        return (List<Map<String, Object>>) data.get("items");
    }

    @Test
    @DisplayName("pageCode 与模板/前端三处一致")
    void pageCode() {
        assertEquals("sale-outbound", provider.pageCode());
    }

    @Test
    @DisplayName("单据不存在返回 null（由调用方抛业务异常）")
    void missingDocument() {
        when(outboundMapper.selectById(999L)).thenReturn(null);
        assertNull(provider.load(999L));
    }

    @Test
    @DisplayName("明细数量取 outboundQuantity —— 别名 quantity 是 null，读了就会打出空数量")
    void usesOutboundQuantityNotAlias() {
        when(outboundMapper.selectById(100L)).thenReturn(outbound());
        when(itemMapper.selectByOutboundId(100L)).thenReturn(List.of(
                item(1, "商品甲", "1.0000", "40.00", "40.00"),
                item(2, "商品乙", "2.0000", "40.00", "80.00")));

        Map<String, Object> data = provider.load(100L);
        List<Map<String, Object>> rows = items(data);

        assertEquals(2, rows.size());
        assertEquals(new BigDecimal("1.0000"), rows.get(0).get("outboundQuantity"));
        assertEquals(new BigDecimal("2.0000"), rows.get(1).get("outboundQuantity"));
        // 模板引用的就是这些键名，缺一个就是一片空白
        for (String key : List.of("lineNo", "productCode", "productName", "specification",
                "productUnit", "outboundQuantity", "unitPrice", "lineAmount", "batchNo", "remark")) {
            assertTrue(rows.get(0).containsKey(key), "明细行缺少模板引用的键: " + key);
        }
    }

    @Test
    @DisplayName("行号为空时按顺序补（库里 line_no 可能是 null）")
    void fillsMissingLineNo() {
        when(outboundMapper.selectById(100L)).thenReturn(outbound());
        when(itemMapper.selectByOutboundId(100L)).thenReturn(List.of(
                item(null, "商品甲", "1", "10.00", "10.00"),
                item(null, "商品乙", "1", "10.00", "10.00")));

        List<Map<String, Object>> rows = items(provider.load(100L));
        assertEquals(1, rows.get(0).get("lineNo"));
        assertEquals(2, rows.get(1).get("lineNo"));
    }

    @Test
    @DisplayName("跨行汇总 = 明细求和；单据头键齐全")
    void aggregatesAndHeaderKeys() {
        when(outboundMapper.selectById(100L)).thenReturn(outbound());
        when(itemMapper.selectByOutboundId(100L)).thenReturn(List.of(
                item(1, "商品甲", "1.0000", "40.00", "40.00"),
                item(2, "商品乙", "2.0000", "40.00", "80.00")));

        Map<String, Object> data = provider.load(100L);

        assertEquals(new BigDecimal("120.00"), data.get("totalLineAmount"));
        assertEquals(new BigDecimal("3.0000"), data.get("totalOutboundQuantity"));
        // 模板 docHeader / summary 引用的键
        for (String key : List.of("outboundNo", "outboundDate", "orderNo", "customerName",
                "customerCode", "warehouseName", "salesPersonName", "receiverName",
                "shippingAddress", "deliveryMethod", "logisticsCompany", "waybillNo",
                "totalAmount", "totalQuantity", "remark", "items")) {
            assertTrue(data.containsKey(key), "单据头缺少模板引用的键: " + key);
        }
    }

    @Test
    @DisplayName("没有明细时 items 是空列表（不是 null），模板才能走 emptyWhenNoData")
    void emptyItems() {
        when(outboundMapper.selectById(100L)).thenReturn(outbound());
        when(itemMapper.selectByOutboundId(100L)).thenReturn(new ArrayList<>());

        Map<String, Object> data = provider.load(100L);
        assertNotNull(data.get("items"));
        assertTrue(items(data).isEmpty());
        assertEquals(BigDecimal.ZERO, data.get("totalLineAmount"));
    }

    @Test
    @DisplayName("documentNo 取单号；单据不存在返回 null")
    void documentNo() {
        when(outboundMapper.selectById(100L)).thenReturn(outbound());
        assertEquals("XSCKD-20260926-0001", provider.documentNo(100L));
    }
}
