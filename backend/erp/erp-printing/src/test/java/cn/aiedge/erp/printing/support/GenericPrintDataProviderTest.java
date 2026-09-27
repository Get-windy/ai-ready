/**
 * 通用单据打印装配器单元测试
 *
 * 纯 Mockito，不连库 —— 钉的是**产出契约**：主表按实体属性名摊平、明细进 items、
 * 行号为空按顺序补、单号能自动认出来。手写装配器要钉的东西它一样要钉。
 */
package cn.aiedge.erp.printing.support;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
@DisplayName("通用单据打印装配器")
class GenericPrintDataProviderTest {

    @Data
    static class TestDoc {
        private Long id;
        private String stockInNo;
        private LocalDate stockInDate;
        private String partnerName;
        private BigDecimal totalAmount;
        private Integer status;
    }

    @Data
    static class TestItem {
        private Long id;
        private Integer lineNo;
        private String productName;
        private BigDecimal quantity;
    }

    @Mock
    private BaseMapper<TestDoc> docMapper;

    @Mock
    private BaseMapper<TestItem> itemMapper;

    /** 与 Spring Boot 的 ObjectMapper 同口径：日期写 ISO 字符串，不写时间戳数组 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private GenericPrintDataProvider provider() {
        return new GenericPrintDataProvider("stock-in", docMapper, itemMapper, "stock_in_id", objectMapper);
    }

    private TestDoc doc() {
        TestDoc doc = new TestDoc();
        doc.setId(100L);
        doc.setStockInNo("CGRK-20260927-0001");
        doc.setStockInDate(LocalDate.of(2026, 9, 27));
        doc.setPartnerName("测试供应商");
        doc.setTotalAmount(new BigDecimal("120.00"));
        doc.setStatus(2);
        return doc;
    }

    private TestItem item(Integer lineNo, String name, String qty) {
        TestItem it = new TestItem();
        it.setLineNo(lineNo);
        it.setProductName(name);
        it.setQuantity(new BigDecimal(qty));
        return it;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> data) {
        return (List<Map<String, Object>>) data.get("items");
    }

    @Test
    @DisplayName("pageCode 就是登记表里那个")
    void pageCode() {
        assertEquals("stock-in", provider().pageCode());
    }

    @Test
    @DisplayName("单据不存在返回 null（由调用方抛业务异常）")
    void missingDocument() {
        when(docMapper.selectById(999L)).thenReturn(null);
        assertNull(provider().load(999L));
        assertNull(provider().documentNo(999L));
    }

    @Test
    @DisplayName("主表按实体属性名摊平，明细进 items，日期仍是可格式化的日期")
    void flattensDocAndItems() {
        when(docMapper.selectById(100L)).thenReturn(doc());
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<TestItem>>any()))
                .thenReturn(List.of(item(1, "商品甲", "2.0000")));

        Map<String, Object> data = provider().load(100L);

        assertEquals("CGRK-20260927-0001", data.get("stockInNo"));
        assertEquals("测试供应商", data.get("partnerName"));
        assertEquals(new BigDecimal("120.00"), data.get("totalAmount"));
        assertEquals("2026-09-27", data.get("stockInDate"));
        assertEquals(1, items(data).size());
        assertEquals("商品甲", items(data).get(0).get("productName"));
    }

    @Test
    @DisplayName("行号为 null 时按顺序补 1..n（库里真有 line_no 空的单）")
    void fillsMissingLineNo() {
        when(docMapper.selectById(100L)).thenReturn(doc());
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<TestItem>>any()))
                .thenReturn(List.of(item(null, "商品甲", "1"), item(null, "商品乙", "1")));

        List<Map<String, Object>> rows = items(provider().load(100L));
        assertEquals(1, rows.get(0).get("lineNo"));
        assertEquals(2, rows.get(1).get("lineNo"));
    }

    @Test
    @DisplayName("没有明细时 items 是空列表（不是 null），模板才能走 emptyWhenNoData")
    void emptyItems() {
        when(docMapper.selectById(100L)).thenReturn(doc());
        when(itemMapper.selectList(ArgumentMatchers.<Wrapper<TestItem>>any())).thenReturn(new ArrayList<>());

        Map<String, Object> data = provider().load(100L);
        assertNotNull(data.get("items"));
        assertTrue(items(data).isEmpty());
    }

    @Test
    @DisplayName("没有明细 mapper 的单据：不查明细，items 仍是空列表（模板可统一按 items 写）")
    void docOnly() {
        when(docMapper.selectById(100L)).thenReturn(doc());
        GenericPrintDataProvider p = new GenericPrintDataProvider("stock-in", docMapper, null, null, objectMapper);

        Map<String, Object> data = p.load(100L);
        assertNotNull(data.get("items"));
        assertTrue(items(data).isEmpty());
        assertEquals("测试供应商", data.get("partnerName"));
    }

    @Test
    @DisplayName("documentNo：认得 xxxNo 型单号（stockInNo 这种不叫 orderNo 的也要认）")
    void documentNo() {
        when(docMapper.selectById(100L)).thenReturn(doc());
        assertEquals("CGRK-20260927-0001", provider().documentNo(100L));
    }
}
