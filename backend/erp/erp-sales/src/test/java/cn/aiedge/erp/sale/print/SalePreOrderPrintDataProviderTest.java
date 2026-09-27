/**
 * 预订货单打印数据装配器单元测试
 *
 * 纯 Mockito，不加载 Spring、不连库 —— 钉的是**契约**而不是 SQL：
 * 模板里的 field 名一旦跟装配器给的键对不上，打出来就是一片空白，
 * 而这种错在编译期完全看不出来，只能靠断言把键名钉住。
 */
package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.sale.preorder.entity.SalePreOrder;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderItemMapper;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("预订货单打印数据装配器")
class SalePreOrderPrintDataProviderTest {

    @Mock
    private SalePreOrderMapper orderMapper;

    @Mock
    private SalePreOrderItemMapper itemMapper;

    @InjectMocks
    private SalePreOrderPrintDataProvider provider;

    /** 模板 items 里引用的列 —— 缺一个那一列就是空白 */
    private static final List<String> ITEM_KEYS = List.of(
            "lineNo", "productCode", "productName", "specification", "model",
            "brand", "unit", "quantity", "unitPrice", "amount", "remark");

    /** 模板 docHeader / summary 里引用的键 */
    private static final List<String> HEADER_KEYS = List.of(
            "orderNo", "orderDate", "status", "customerName", "customerCode",
            "warehouseName", "handlerName", "deptName", "receiverName", "receiverPhone",
            "shippingAddress", "depositDeadline", "summary", "remark",
            "totalQuantity", "totalLineAmount", "orderAmount",
            "depositAmount", "receivedDeposit", "unreceivedDeposit", "items");

    private SalePreOrder order() {
        SalePreOrder doc = new SalePreOrder();
        doc.setId(200L);
        doc.setOrderNo("YDH-20260927-0001");
        doc.setOrderDate(LocalDate.of(2026, 9, 27));
        doc.setStatus(2);
        doc.setCustomerCode("KH001");
        doc.setCustomerName("测试客户");
        doc.setWarehouseName("主仓库");
        doc.setHandlerName("张三");
        doc.setDeptName("销售一部");
        doc.setReceiverName("李四");
        doc.setReceiverPhone("13800000000");
        doc.setShippingAddress("测试市测试路 1 号");
        doc.setDepositDeadline(LocalDate.of(2026, 10, 10));
        doc.setSummary("预订摘要");
        doc.setRemark("单据备注");
        doc.setTotalAmount(new BigDecimal("300.00"));
        doc.setOrderAmount(new BigDecimal("300.00"));
        doc.setDepositAmount(new BigDecimal("100.00"));
        doc.setReceivedDeposit(new BigDecimal("50.00"));
        doc.setUnreceivedDeposit(new BigDecimal("50.00"));
        doc.setPreOrderQuantity(new BigDecimal("12.0000"));
        return doc;
    }

    private SalePreOrderItem item(Integer lineNo, String name, String qty, String price, String amount) {
        SalePreOrderItem it = new SalePreOrderItem();
        it.setLineNo(lineNo);
        it.setProductName(name);
        it.setProductCode("SP-" + (lineNo == null ? "?" : lineNo));
        it.setUnit("件");
        if (qty != null) it.setQuantity(new BigDecimal(qty));
        it.setUnitPrice(new BigDecimal(price));
        it.setAmount(new BigDecimal(amount));
        return it;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> data) {
        return (List<Map<String, Object>>) data.get("items");
    }

    @Test
    @DisplayName("pageCode 与模板/前端三处一致，且不含 `/`（它是路径段）")
    void pageCode() {
        assertEquals("sale-pre-order", provider.pageCode());
        assertFalse(provider.pageCode().contains("/"), "page-code 不能含斜杠，否则业务级接口会 400");
    }

    @Test
    @DisplayName("单据不存在返回 null（由调用方抛业务异常）")
    void missingDocument() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        assertNull(provider.load(999L));
        assertNull(provider.documentNo(999L));
    }

    @Test
    @DisplayName("明细行键齐全 + 金额/数量取实体原名")
    void itemKeysAndValues() {
        when(orderMapper.selectById(200L)).thenReturn(order());
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<SalePreOrderItem>>any()))
                .thenReturn(List.of(
                        item(1, "商品甲", "10.0000", "20.00", "200.00"),
                        item(2, "商品乙", "2.0000", "50.00", "100.00")));

        List<Map<String, Object>> rows = items(provider.load(200L));

        assertEquals(2, rows.size());
        assertEquals(new BigDecimal("10.0000"), rows.get(0).get("quantity"));
        assertEquals(new BigDecimal("200.00"), rows.get(0).get("amount"));
        // 模板引用的就是这些键名，缺一个就是一片空白
        for (String key : ITEM_KEYS) {
            assertTrue(rows.get(0).containsKey(key), "明细行缺少模板引用的键: " + key);
        }
    }

    @Test
    @DisplayName("行号为空时按顺序补")
    void fillsMissingLineNo() {
        when(orderMapper.selectById(200L)).thenReturn(order());
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<SalePreOrderItem>>any()))
                .thenReturn(List.of(
                        item(null, "商品甲", "1", "10.00", "10.00"),
                        item(null, "商品乙", "1", "10.00", "10.00")));

        List<Map<String, Object>> rows = items(provider.load(200L));
        assertEquals(1, rows.get(0).get("lineNo"));
        assertEquals(2, rows.get(1).get("lineNo"));
    }

    @Test
    @DisplayName("单据头键齐全；明细合计按明细求和")
    void headerKeysAndAggregates() {
        when(orderMapper.selectById(200L)).thenReturn(order());
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<SalePreOrderItem>>any()))
                .thenReturn(List.of(
                        item(1, "商品甲", "10.0000", "20.00", "200.00"),
                        item(2, "商品乙", "2.0000", "50.00", "100.00")));

        Map<String, Object> data = provider.load(200L);

        assertEquals(new BigDecimal("300.00"), data.get("totalLineAmount"));
        // 表头存了预订数量就用它（权威值），不按明细重算
        assertEquals(new BigDecimal("12.0000"), data.get("totalQuantity"));
        for (String key : HEADER_KEYS) {
            assertTrue(data.containsKey(key), "单据头缺少模板引用的键: " + key);
        }
    }

    @Test
    @DisplayName("没有明细时 items 是空列表（不是 null），模板才能走 emptyWhenNoData")
    void emptyItems() {
        SalePreOrder doc = order();
        doc.setPreOrderQuantity(null);
        when(orderMapper.selectById(200L)).thenReturn(doc);
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<SalePreOrderItem>>any()))
                .thenReturn(new ArrayList<>());

        Map<String, Object> data = provider.load(200L);
        assertNotNull(data.get("items"));
        assertTrue(items(data).isEmpty());
        assertEquals(BigDecimal.ZERO, data.get("totalLineAmount"));
        assertEquals(BigDecimal.ZERO, data.get("totalQuantity"));
    }

    @Test
    @DisplayName("documentNo 取单号")
    void documentNo() {
        when(orderMapper.selectById(200L)).thenReturn(order());
        assertEquals("YDH-20260927-0001", provider.documentNo(200L));
    }
}
