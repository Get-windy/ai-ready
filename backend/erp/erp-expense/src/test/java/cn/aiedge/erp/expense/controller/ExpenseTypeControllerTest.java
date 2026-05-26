package cn.aiedge.erp.expense.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseTypeControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetExpenseTypeList() throws Exception {
        mockMvc.perform(get("/expense/type/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(16)) // 有16种费用类型
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].description").exists());
    }
    
    @Test
    void testGetExpenseTypeDetail() throws Exception {
        mockMvc.perform(get("/expense/type/TRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TRA"))
                .andExpect(jsonPath("$.data.description").value("差旅费"))
                .andExpect(jsonPath("$.data.requiresApproval").value(true));
    }
    
    @Test
    void testGetExpenseTypeDescriptions() throws Exception {
        mockMvc.perform(get("/expense/type/descriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(16))
                .andExpect(jsonPath("$.data[0]").value("差旅费"));
    }
    
    @Test
    void testGetExpenseTypeCodes() throws Exception {
        mockMvc.perform(get("/expense/type/codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(16))
                .andExpect(jsonPath("$.data[0]").value("TRA"));
    }
    
    @Test
    void testInvalidExpenseTypeCode() throws Exception {
        mockMvc.perform(get("/expense/type/INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("OTH")) // 无效code返回OTHER
                .andExpect(jsonPath("$.data.description").value("其他"));
    }
}