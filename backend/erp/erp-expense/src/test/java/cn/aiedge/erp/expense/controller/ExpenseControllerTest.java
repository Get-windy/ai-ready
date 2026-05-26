package cn.aiedge.erp.expense.controller;

import cn.aiedge.erp.expense.dto.ExpenseRequest;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testApplyExpense() throws Exception {
        ExpenseRequest request = new ExpenseRequest();
        request.setApplicantId("user001");
        request.setApplicantName("张三");
        request.setApplyDate(LocalDate.now());
        request.setExpenseType(ExpenseType.TRAVEL);
        request.setTotalAmount(new BigDecimal("1500.00"));
        request.setPurpose("北京出差差旅费");
        
        mockMvc.perform(post("/expense/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationCode").exists())
                .andExpect(jsonPath("$.data.applicantId").value("user001"))
                .andExpect(jsonPath("$.data.totalAmount").value(1500.00));
    }
    
    @Test
    void testGetExpenseDetail() throws Exception {
        mockMvc.perform(get("/expense/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.applicationCode").exists());
    }
    
    @Test
    void testUpdateExpense() throws Exception {
        ExpenseRequest request = new ExpenseRequest();
        request.setApplicantId("user001");
        request.setApplicantName("张三");
        request.setApplyDate(LocalDate.now());
        request.setExpenseType(ExpenseType.TRAVEL);
        request.setTotalAmount(new BigDecimal("1800.00"));
        request.setPurpose("北京出差差旅费（更新）");
        
        mockMvc.perform(put("/expense/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("费用单更新成功"));
    }
    
    @Test
    void testDeleteExpense() throws Exception {
        mockMvc.perform(delete("/expense/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("费用单删除成功"));
    }
    
    @Test
    void testSubmitForApproval() throws Exception {
        mockMvc.perform(post("/expense/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("费用单已提交审批"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }
    
    @Test
    void testApproveExpense() throws Exception {
        mockMvc.perform(post("/expense/1/approve")
                .param("comment", "同意报销"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("费用单审批通过"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }
    
    @Test
    void testRejectExpense() throws Exception {
        mockMvc.perform(post("/expense/1/reject")
                .param("reason", "金额超出标准"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("费用单审批拒绝"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
    
    @Test
    void testGetExpenseList() throws Exception {
        mockMvc.perform(get("/expense/list")
                .param("applicantId", "user001")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
    
    @Test
    void testGetExpenseStatistics() throws Exception {
        mockMvc.perform(get("/expense/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.expenseCount").exists());
    }
    
    @Test
    void testValidation() throws Exception {
        // 测试缺少必填字段
        ExpenseRequest request = new ExpenseRequest();
        request.setApplicantId("user001");
        // 缺少applicantName、applyDate等必填字段
        
        mockMvc.perform(post("/expense/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}