package cn.aiedge.erp.supplier.controller;

import cn.aiedge.erp.supplier.dto.SupplierDTO;
import cn.aiedge.erp.supplier.dto.SupplierPerformanceDTO;
import cn.aiedge.erp.supplier.dto.SupplierQueryDTO;
import cn.aiedge.erp.supplier.service.SupplierService;
import cn.aiedge.common.core.domain.PageResult;
import cn.aiedge.common.core.domain.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 供应商控制器测试
 * 测试用例：PM-SUPP-001 ~ PM-SUPP-007
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
@WebMvcTest(SupplierController.class)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierService supplierService;

    private SupplierDTO testSupplier;

    @BeforeEach
    void setUp() {
        testSupplier = new SupplierDTO();
        testSupplier.setId(1L);
        testSupplier.setSupplierCode("SUP-20260505-001");
        testSupplier.setSupplierName("测试供应商有限公司");
        testSupplier.setSupplierType(1); // 1-生产型
        testSupplier.setContactPerson("张经理");
        testSupplier.setContactPhone("13800138000");
        testSupplier.setContactEmail("supplier@test.com");
        testSupplier.setAddress("北京市朝阳区测试路123号");
        testSupplier.setBusinessScope("电子产品、机械设备");
        testSupplier.setRegistrationDate(LocalDateTime.now().minusYears(2));
        testSupplier.setStatus(1); // 1-正常
        testSupplier.setCreditRating("AAA");
        testSupplier.setPerformanceScore(new BigDecimal("95.5"));
        testSupplier.setRemarks("优质供应商，合作稳定");
    }

    @Test
    @DisplayName("PM-SUPP-001: 供应商注册功能测试")
    void testCreateSupplier() throws Exception {
        // 准备测试数据
        SupplierDTO newSupplier = new SupplierDTO();
        newSupplier.setSupplierCode("SUP-20260505-002");
        newSupplier.setSupplierName("新测试供应商");
        newSupplier.setSupplierType(2);
        newSupplier.setContactPerson("王经理");
        newSupplier.setContactPhone("13900139000");
        newSupplier.setStatus(1);

        // 模拟服务层返回
        R<SupplierDTO> mockResponse = R.ok("创建成功", newSupplier);
        when(supplierService.createSupplier(any(SupplierDTO.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSupplier)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("创建成功"))
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-20260505-002"));
    }

    @Test
    @DisplayName("PM-SUPP-002: 供应商审核流程测试")
    void testApproveSupplier() throws Exception {
        // 模拟服务层返回
        R<SupplierDTO> mockResponse = R.ok("审核通过", testSupplier);
        when(supplierService.approveSupplier(1L, "资质齐全，审核通过")).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier/{id}/approve", 1L)
                .param("comment", "资质齐全，审核通过"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("审核通过"));
    }

    @Test
    @DisplayName("PM-SUPP-003: 供应商分类管理测试")
    void testUpdateSupplierCategory() throws Exception {
        // 准备更新数据
        SupplierDTO updateData = new SupplierDTO();
        updateData.setSupplierType(3); // 3-服务型
        updateData.setCategoryId(100L);

        // 模拟服务层返回
        testSupplier.setSupplierType(3);
        testSupplier.setCategoryId(100L);
        R<SupplierDTO> mockResponse = R.ok("分类更新成功", testSupplier);
        when(supplierService.updateSupplier(any(SupplierDTO.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(put("/api/supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("分类更新成功"));
    }

    @Test
    @DisplayName("PM-SUPP-004: 供应商资质验证测试")
    void testVerifySupplierQualification() throws Exception {
        // 模拟资质验证通过
        R<Boolean> mockResponse = R.ok("资质验证通过", true);
        when(supplierService.verifyQualification(1L)).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier/{id}/verify-qualification", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("资质验证通过"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("PM-SUPP-005: 供应商绩效评估测试")
    void testEvaluateSupplierPerformance() throws Exception {
        // 准备绩效评估数据
        SupplierPerformanceDTO performanceDTO = new SupplierPerformanceDTO();
        performanceDTO.setQualityScore(new BigDecimal("98.5"));
        performanceDTO.setDeliveryScore(new BigDecimal("96.0"));
        performanceDTO.setServiceScore(new BigDecimal("92.0"));
        performanceDTO.setPriceScore(new BigDecimal("88.5"));
        performanceDTO.setOverallScore(new BigDecimal("93.75"));

        // 模拟服务层返回
        R<SupplierPerformanceDTO> mockResponse = R.ok("绩效评估完成", performanceDTO);
        when(supplierService.evaluatePerformance(1L)).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier/{id}/evaluate-performance", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("绩效评估完成"))
                .andExpect(jsonPath("$.data.overallScore").value(93.75));
    }

    @Test
    @DisplayName("PM-SUPP-006: 供应商黑名单管理测试")
    void testManageSupplierBlacklist() throws Exception {
        // 模拟加入黑名单
        R<Boolean> mockResponse = R.ok("已加入黑名单", true);
        when(supplierService.addToBlacklist(1L, "多次延迟交货")).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier/{id}/blacklist", 1L)
                .param("reason", "多次延迟交货"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("已加入黑名单"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("PM-SUPP-007: 供应商信息更新测试")
    void testUpdateSupplierInformation() throws Exception {
        // 准备更新数据
        SupplierDTO updateData = new SupplierDTO();
        updateData.setId(1L);
        updateData.setContactPerson("李总监");
        updateData.setContactPhone("13600136000");
        updateData.setContactEmail("new-contact@test.com");

        // 模拟服务层返回
        testSupplier.setContactPerson("李总监");
        testSupplier.setContactPhone("13600136000");
        testSupplier.setContactEmail("new-contact@test.com");
        R<SupplierDTO> mockResponse = R.ok("信息更新成功", testSupplier);
        when(supplierService.updateSupplier(any(SupplierDTO.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(put("/api/supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("信息更新成功"));
    }

    @Test
    @DisplayName("获取供应商详情测试")
    void testGetSupplierById() throws Exception {
        // 模拟服务层返回
        R<SupplierDTO> mockResponse = R.ok("查询成功", testSupplier);
        when(supplierService.getSupplierById(1L)).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(get("/api/supplier/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-20260505-001"))
                .andExpect(jsonPath("$.data.supplierName").value("测试供应商有限公司"));
    }

    @Test
    @DisplayName("查询供应商列表测试")
    void testGetSupplierList() throws Exception {
        // 准备分页数据
        PageResult<SupplierDTO> pageResult = new PageResult<>();
        pageResult.setTotal(2L);
        pageResult.setRecords(Arrays.asList(testSupplier, createAnotherSupplier()));

        // 模拟服务层返回
        R<PageResult<SupplierDTO>> mockResponse = R.ok("查询成功", pageResult);
        when(supplierService.getSupplierList(any(SupplierQueryDTO.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(get("/api/supplier/list")
                .param("pageNum", "1")
                .param("pageSize", "10")
                .param("supplierType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].supplierCode").value("SUP-20260505-001"));
    }

    @Test
    @DisplayName("供应商统计信息测试")
    void testGetSupplierStatistics() throws Exception {
        // 模拟统计信息
        R<Map<String, Object>> mockResponse = R.ok("统计成功", 
                Map.of(
                    "totalCount", 150,
                    "activeCount", 135,
                    "blacklistCount", 5,
                    "pendingReviewCount", 10,
                    "averagePerformanceScore", 88.5
                ));
        when(supplierService.getStatistics()).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(get("/api/supplier/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(150))
                .andExpect(jsonPath("$.data.averagePerformanceScore").value(88.5));
    }

    @Test
    @DisplayName("供应商资质文件上传测试")
    void testUploadQualificationFile() throws Exception {
        // 模拟文件上传成功
        R<String> mockResponse = R.ok("文件上传成功", "qualification_20260505_001.pdf");
        when(supplierService.uploadQualificationFile(1L, "营业执照", "test-file-content"))
                .thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier/{id}/upload-qualification", 1L)
                .param("fileType", "营业执照")
                .param("fileContent", "test-file-content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("文件上传成功"));
    }

    @Test
    @DisplayName("异常测试：重复供应商编码")
    void testCreateSupplierWithDuplicateCode() throws Exception {
        // 准备测试数据
        SupplierDTO duplicateSupplier = new SupplierDTO();
        duplicateSupplier.setSupplierCode("SUP-20260505-001"); // 重复编码

        // 模拟服务层抛出异常
        when(supplierService.createSupplier(any(SupplierDTO.class)))
                .thenThrow(new RuntimeException("供应商编码已存在"));

        // 执行测试
        mockMvc.perform(post("/api/supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateSupplier)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("异常测试：更新不存在的供应商")
    void testUpdateNonExistentSupplier() throws Exception {
        // 准备更新数据
        SupplierDTO updateData = new SupplierDTO();
        updateData.setId(99999L); // 不存在的ID

        // 模拟服务层返回失败
        when(supplierService.updateSupplier(any(SupplierDTO.class)))
                .thenThrow(new RuntimeException("供应商不存在"));

        // 执行测试
        mockMvc.perform(put("/api/supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("边界测试：创建超大企业供应商")
    void testCreateLargeEnterpriseSupplier() throws Exception {
        // 准备测试数据 - 超大企业
        SupplierDTO largeSupplier = new SupplierDTO();
        largeSupplier.setSupplierCode("SUP-ENTERPRISE-001");
        largeSupplier.setSupplierName("国际大型企业集团股份有限公司");
        largeSupplier.setSupplierType(1);
        largeSupplier.setContactPerson("国际业务部总监");
        largeSupplier.setContactPhone("+86-10-12345678");
        largeSupplier.setBusinessScope("涵盖电子、机械、化工、金融、房地产等多个领域的大型跨国企业集团");

        // 模拟服务层返回
        R<SupplierDTO> mockResponse = R.ok("创建成功", largeSupplier);
        when(supplierService.createSupplier(any(SupplierDTO.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeSupplier)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("性能测试：批量查询供应商")
    void testBatchQuerySuppliers() throws Exception {
        // 执行测试 - 查询大量数据
        mockMvc.perform(get("/api/supplier/list")
                .param("pageNum", "1")
                .param("pageSize", "100") // 较大分页
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private SupplierDTO createAnotherSupplier() {
        SupplierDTO anotherSupplier = new SupplierDTO();
        anotherSupplier.setId(2L);
        anotherSupplier.setSupplierCode("SUP-20260505-002");
        anotherSupplier.setSupplierName("第二测试供应商");
        anotherSupplier.setSupplierType(2);
        anotherSupplier.setContactPerson("刘经理");
        anotherSupplier.setContactPhone("13700137000");
        anotherSupplier.setStatus(1);
        return anotherSupplier;
    }
}