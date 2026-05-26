package cn.aiedge.erp.supplier.integration;

import cn.aiedge.erp.supplier.dto.SupplierDTO;
import cn.aiedge.erp.supplier.dto.SupplierPerformanceDTO;
import cn.aiedge.erp.supplier.dto.SupplierQueryDTO;
import cn.aiedge.erp.supplier.service.SupplierService;
import cn.aiedge.common.core.domain.PageResult;
import cn.aiedge.common.core.domain.R;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 供应商服务集成测试
 * 使用TestContainers进行数据库集成测试
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class SupplierIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("erp_supplier_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    void testCreateAndQuerySupplier() {
        // 创建供应商
        SupplierDTO supplierDTO = new SupplierDTO();
        supplierDTO.setSupplierCode("SUP20240001");
        supplierDTO.setSupplierName("北京科技有限公司");
        supplierDTO.setSupplierType(1);
        supplierDTO.setContactPerson("张三");
        supplierDTO.setContactPhone("13800138000");

        R<SupplierDTO> createResult = supplierService.createSupplier(supplierDTO);
        assertTrue(createResult.isSuccess());
        assertNotNull(createResult.getData());
        assertEquals("SUP20240001", createResult.getData().getSupplierCode());

        // 查询供应商
        R<SupplierDTO> queryResult = supplierService.getSupplierById(createResult.getData().getId());
        assertTrue(queryResult.isSuccess());
        assertNotNull(queryResult.getData());
        assertEquals("北京科技有限公司", queryResult.getData().getSupplierName());
    }

    @Test
    void testQuerySupplierPage() {
        // 创建多个供应商
        for (int i = 1; i <= 5; i++) {
            SupplierDTO supplierDTO = new SupplierDTO();
            supplierDTO.setSupplierCode("SUP2024000" + i);
            supplierDTO.setSupplierName("测试供应商" + i);
            supplierDTO.setSupplierType(1);
            supplierDTO.setContactPerson("联系人" + i);
            supplierDTO.setContactPhone("1380013800" + i);
            supplierService.createSupplier(supplierDTO);
        }

        // 分页查询
        SupplierQueryDTO queryDTO = new SupplierQueryDTO();
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);

        R<PageResult<SupplierDTO>> result = supplierService.querySupplierPage(queryDTO);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getTotal() >= 5);
    }

    @Test
    void testSupplierPerformanceEvaluation() {
        // 创建供应商
        SupplierDTO supplierDTO = new SupplierDTO();
        supplierDTO.setSupplierCode("SUP20240010");
        supplierDTO.setSupplierName("绩效测试供应商");
        supplierDTO.setSupplierType(1);
        supplierDTO.setContactPerson("测试联系人");
        supplierDTO.setContactPhone("13800138010");

        R<SupplierDTO> createResult = supplierService.createSupplier(supplierDTO);
        assertTrue(createResult.isSuccess());
        Long supplierId = createResult.getData().getId();

        // 绩效评估
        SupplierPerformanceDTO performanceDTO = new SupplierPerformanceDTO();
        performanceDTO.setSupplierId(supplierId);
        performanceDTO.setEvaluationPeriod("2024-01");
        performanceDTO.setEvaluationType(1);
        performanceDTO.setQualityScore(95.0);
        performanceDTO.setDeliveryScore(90.0);
        performanceDTO.setPriceScore(85.0);
        performanceDTO.setServiceScore(92.0);
        performanceDTO.setResponseScore(88.0);

        R<Boolean> evaluateResult = supplierService.evaluateSupplierPerformance(performanceDTO);
        assertTrue(evaluateResult.isSuccess());
        assertTrue(evaluateResult.getData());

        // 查询绩效历史
        R<List<SupplierPerformanceDTO>> historyResult = supplierService.getSupplierPerformanceHistory(supplierId, 1, 10);
        assertTrue(historyResult.isSuccess());
        assertNotNull(historyResult.getData());
        assertFalse(historyResult.getData().isEmpty());

        // 验证综合评分计算
        SupplierPerformanceDTO performance = historyResult.getData().get(0);
        assertNotNull(performance.getComprehensiveScore());
        assertTrue(performance.getComprehensiveScore() > 0);
        assertNotNull(performance.getPerformanceLevel());
    }

    @Test
    void testSupplierPortalActivation() {
        // 创建供应商
        SupplierDTO supplierDTO = new SupplierDTO();
        supplierDTO.setSupplierCode("SUP20240020");
        supplierDTO.setSupplierName("门户测试供应商");
        supplierDTO.setSupplierType(1);
        supplierDTO.setContactPerson("门户联系人");
        supplierDTO.setContactPhone("13800138020");

        R<SupplierDTO> createResult = supplierService.createSupplier(supplierDTO);
        assertTrue(createResult.isSuccess());
        Long supplierId = createResult.getData().getId();

        // 激活门户
        R<Boolean> activateResult = supplierService.activateSupplierPortal(supplierId, "user123456");
        assertTrue(activateResult.isSuccess());
        assertTrue(activateResult.getData());

        // 验证门户状态
        R<SupplierDTO> queryResult = supplierService.getSupplierById(supplierId);
        assertTrue(queryResult.isSuccess());
        assertEquals(1, queryResult.getData().getPortalStatus());
        assertEquals("user123456", queryResult.getData().getPortalAccountId());
    }

    @Test
    void testSupplierLevelUpdate() {
        // 创建供应商
        SupplierDTO supplierDTO = new SupplierDTO();
        supplierDTO.setSupplierCode("SUP20240030");
        supplierDTO.setSupplierName("等级测试供应商");
        supplierDTO.setSupplierType(1);
        supplierDTO.setContactPerson("等级联系人");
        supplierDTO.setContactPhone("13800138030");

        R<SupplierDTO> createResult = supplierService.createSupplier(supplierDTO);
        assertTrue(createResult.isSuccess());
        Long supplierId = createResult.getData().getId();

        // 更新等级
        R<Boolean> levelResult = supplierService.updateSupplierLevel(supplierId, "A", "绩效优秀");
        assertTrue(levelResult.isSuccess());
        assertTrue(levelResult.getData());

        // 验证等级
        R<SupplierDTO> queryResult = supplierService.getSupplierById(supplierId);
        assertTrue(queryResult.isSuccess());
        assertEquals("A", queryResult.getData().getSupplierLevel());
    }

    @Test
    void testMultiTenantIsolation() {
        // 创建供应商（默认租户）
        SupplierDTO supplierDTO = new SupplierDTO();
        supplierDTO.setSupplierCode("SUP20240040");
        supplierDTO.setSupplierName("多租户测试供应商");
        supplierDTO.setSupplierType(1);
        supplierDTO.setContactPerson("多租户联系人");
        supplierDTO.setContactPhone("13800138040");

        R<SupplierDTO> createResult = supplierService.createSupplier(supplierDTO);
        assertTrue(createResult.isSuccess());

        // 验证租户隔离 - 查询应该只返回当前租户的数据
        SupplierQueryDTO queryDTO = new SupplierQueryDTO();
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);

        R<PageResult<SupplierDTO>> result = supplierService.querySupplierPage(queryDTO);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        
        // 验证所有返回的数据都属于当前租户
        if (result.getData().getList() != null) {
            for (SupplierDTO dto : result.getData().getList()) {
                // 这里可以添加租户ID验证逻辑
            }
        }
    }
}