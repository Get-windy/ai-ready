package cn.aiedge.erp.stock;

import cn.aiedge.erp.stock.entity.Stock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 库存管理Controller集成测试
 * 
 * @author qa-lead
 * @since 2026-04-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("库存管理Controller集成测试")
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_PRODUCT_ID = 9999L;
    private static final Long TEST_WAREHOUSE_ID = 8888L;

    @BeforeEach
    void setUp() {
        // 测试前的准备工作
    }

    @Test
    @DisplayName("TC001: 查询库存详情API")
    @Transactional
    void testGetStockDetailAPI() throws Exception {
        mockMvc.perform(get("/api/stock/{productId}/{warehouseId}", TEST_PRODUCT_ID, TEST_WAREHOUSE_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @DisplayName("TC003: 库存增加API")
    @Transactional
    void testIncreaseStockAPI() throws Exception {
        mockMvc.perform(post("/api/stock/increase")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("TC005: 库存减少API")
    @Transactional
    void testDecreaseStockAPI() throws Exception {
        // 先增加库存
        mockMvc.perform(post("/api/stock/increase")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("quantity", "100"))
                .andExpect(status().isOk());

        // 再减少库存
        mockMvc.perform(post("/api/stock/decrease")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("quantity", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("TC006: 库存减少API - 库存不足")
    @Transactional
    void testDecreaseStockAPI_Insufficient() throws Exception {
        // 先增加少量库存
        mockMvc.perform(post("/api/stock/increase")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("quantity", "10"))
                .andExpect(status().isOk());

        // 尝试减少超过库存的数量
        mockMvc.perform(post("/api/stock/decrease")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("TC011: 库存盘点API")
    @Transactional
    void testCheckStockAPI() throws Exception {
        // 先增加库存
        mockMvc.perform(post("/api/stock/increase")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("quantity", "100"))
                .andExpect(status().isOk());

        // 盘点
        mockMvc.perform(post("/api/stock/check")
                        .param("productId", TEST_PRODUCT_ID.toString())
                        .param("warehouseId", TEST_WAREHOUSE_ID.toString())
                        .param("actualQuantity", "120"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("TC013: 库存预警API")
    @Transactional
    void testCheckStockAlertAPI() throws Exception {
        mockMvc.perform(get("/api/stock/alert"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @DisplayName("TC015: 查询库存列表API")
    @Transactional
    void testGetStockListAPI() throws Exception {
        mockMvc.perform(get("/api/stock/list")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.current").value(1));
    }

    @Test
    @DisplayName("TC034: SQL注入测试")
    @Transactional
    void testSQLInjection() throws Exception {
        mockMvc.perform(get("/api/stock/{productId}/{warehouseId}", "1 OR 1=1", "1"))
                .andExpect(status().isOk());
        // 验证没有执行恶意SQL，返回null或正常结果
    }
}