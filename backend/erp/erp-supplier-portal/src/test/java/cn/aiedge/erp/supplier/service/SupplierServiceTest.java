package cn.aiedge.erp.supplier.service;

import cn.aiedge.erp.supplier.dto.SupplierDTO;
import cn.aiedge.erp.supplier.dto.SupplierPerformanceDTO;
import cn.aiedge.erp.supplier.dto.SupplierQueryDTO;
import cn.aiedge.erp.supplier.model.entity.SupplierEntity;
import cn.aiedge.erp.supplier.model.entity.SupplierPerformanceEntity;
import cn.aiedge.erp.supplier.repository.SupplierPerformanceRepository;
import cn.aiedge.erp.supplier.repository.SupplierRepository;
import cn.aiedge.erp.supplier.service.impl.SupplierServiceImpl;
import cn.aiedge.common.core.domain.PageResult;
import cn.aiedge.common.core.domain.R;
import cn.aiedge.common.core.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 供应商服务单元测试
 */
@ExtendWith(MockitoExtension.class)
public class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierPerformanceRepository supplierPerformanceRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @BeforeEach
    void setUp() {
        // 初始化SecurityUtils的mock
    }

    @Test
    void testCreateSupplier() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierDTO supplierDTO = new SupplierDTO();
            supplierDTO.setSupplierCode("SUP20240001");
            supplierDTO.setSupplierName("北京科技有限公司");
            supplierDTO.setSupplierType(1);
            supplierDTO.setContactPerson("张三");
            supplierDTO.setContactPhone("13800138000");

            when(supplierRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(supplierRepository.insert(any(SupplierEntity.class))).thenReturn(1);

            R<SupplierDTO> result = supplierService.createSupplier(supplierDTO);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals("SUP20240001", result.getData().getSupplierCode());
        }
    }

    @Test
    void testCreateSupplier_DuplicateCode() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");

            SupplierDTO supplierDTO = new SupplierDTO();
            supplierDTO.setSupplierCode("SUP20240001");
            supplierDTO.setSupplierName("北京科技有限公司");
            supplierDTO.setSupplierType(1);

            when(supplierRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            R<SupplierDTO> result = supplierService.createSupplier(supplierDTO);

            assertFalse(result.isSuccess());
            assertTrue(result.getMsg().contains("供应商编码已存在"));
        }
    }

    @Test
    void testGetSupplierById() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");

            SupplierEntity entity = new SupplierEntity();
            entity.setId(1L);
            entity.setSupplierCode("SUP20240001");
            entity.setSupplierName("北京科技有限公司");
            entity.setTenantId("default");
            entity.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(entity);

            R<SupplierDTO> result = supplierService.getSupplierById(1L);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals("SUP20240001", result.getData().getSupplierCode());
        }
    }

    @Test
    void testGetSupplierById_NotFound() {
        when(supplierRepository.selectById(1L)).thenReturn(null);

        R<SupplierDTO> result = supplierService.getSupplierById(1L);

        assertFalse(result.isSuccess());
        assertTrue(result.getMsg().contains("不存在"));
    }

    @Test
    void testDeleteSupplier() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierEntity entity = new SupplierEntity();
            entity.setId(1L);
            entity.setTenantId("default");
            entity.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(entity);
            when(supplierRepository.updateById(any(SupplierEntity.class))).thenReturn(1);

            R<Boolean> result = supplierService.deleteSupplier(1L);

            assertTrue(result.isSuccess());
            assertTrue(result.getData());
        }
    }

    @Test
    void testEvaluateSupplierPerformance() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierEntity supplierEntity = new SupplierEntity();
            supplierEntity.setId(1L);
            supplierEntity.setTenantId("default");
            supplierEntity.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(supplierEntity);
            when(supplierPerformanceRepository.insert(any(SupplierPerformanceEntity.class))).thenReturn(1);
            when(supplierRepository.updateById(any(SupplierEntity.class))).thenReturn(1);

            SupplierPerformanceDTO performanceDTO = new SupplierPerformanceDTO();
            performanceDTO.setSupplierId(1L);
            performanceDTO.setEvaluationPeriod("2024-01");
            performanceDTO.setEvaluationType(1);
            performanceDTO.setQualityScore(95.0);
            performanceDTO.setDeliveryScore(90.0);
            performanceDTO.setPriceScore(85.0);
            performanceDTO.setServiceScore(92.0);
            performanceDTO.setResponseScore(88.0);

            R<Boolean> result = supplierService.evaluateSupplierPerformance(performanceDTO);

            assertTrue(result.isSuccess());
            assertTrue(result.getData());
        }
    }

    @Test
    void testActivateSupplierPortal() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierEntity entity = new SupplierEntity();
            entity.setId(1L);
            entity.setTenantId("default");
            entity.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(entity);
            when(supplierRepository.updateById(any(SupplierEntity.class))).thenReturn(1);

            R<Boolean> result = supplierService.activateSupplierPortal(1L, "user123456");

            assertTrue(result.isSuccess());
            assertTrue(result.getData());
        }
    }

    @Test
    void testUpdateSupplierLevel() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierEntity entity = new SupplierEntity();
            entity.setId(1L);
            entity.setTenantId("default");
            entity.setSupplierLevel("B");
            entity.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(entity);
            when(supplierRepository.updateById(any(SupplierEntity.class))).thenReturn(1);

            R<Boolean> result = supplierService.updateSupplierLevel(1L, "A", "绩效优秀");

            assertTrue(result.isSuccess());
            assertTrue(result.getData());
        }
    }

    @Test
    void testQuerySupplierPage() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");

            SupplierQueryDTO queryDTO = new SupplierQueryDTO();
            queryDTO.setPageNum(1);
            queryDTO.setPageSize(20);

            SupplierEntity entity1 = new SupplierEntity();
            entity1.setId(1L);
            entity1.setSupplierCode("SUP20240001");
            entity1.setSupplierName("北京科技有限公司");

            SupplierEntity entity2 = new SupplierEntity();
            entity2.setId(2L);
            entity2.setSupplierCode("SUP20240002");
            entity2.setSupplierName("上海贸易有限公司");

            List<SupplierEntity> entityList = Arrays.asList(entity1, entity2);
            Page<SupplierEntity> page = new Page<>(1, 20);
            page.setRecords(entityList);
            page.setTotal(2);

            when(supplierRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            R<PageResult<SupplierDTO>> result = supplierService.querySupplierPage(queryDTO);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(2, result.getData().getTotal());
        }
    }

    @Test
    void testBatchDeleteSupplier() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierEntity entity1 = new SupplierEntity();
            entity1.setId(1L);
            entity1.setTenantId("default");
            entity1.setDeleted(0);

            SupplierEntity entity2 = new SupplierEntity();
            entity2.setId(2L);
            entity2.setTenantId("default");
            entity2.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(entity1);
            when(supplierRepository.selectById(2L)).thenReturn(entity2);
            when(supplierRepository.updateById(any(SupplierEntity.class))).thenReturn(1);

            List<Long> ids = Arrays.asList(1L, 2L);
            R<Boolean> result = supplierService.batchDeleteSupplier(ids);

            assertTrue(result.isSuccess());
            assertTrue(result.getData());
        }
    }

    @Test
    void testGetSupplierPerformanceHistory() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");

            SupplierPerformanceEntity entity1 = new SupplierPerformanceEntity();
            entity1.setId(1L);
            entity1.setSupplierId(1L);
            entity1.setEvaluationPeriod("2024-01");
            entity1.setComprehensiveScore(90.5);

            SupplierPerformanceEntity entity2 = new SupplierPerformanceEntity();
            entity2.setId(2L);
            entity2.setSupplierId(1L);
            entity2.setEvaluationPeriod("2024-02");
            entity2.setComprehensiveScore(92.0);

            List<SupplierPerformanceEntity> entityList = Arrays.asList(entity1, entity2);

            when(supplierPerformanceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(entityList);

            R<List<SupplierPerformanceDTO>> result = supplierService.getSupplierPerformanceHistory(1L, 1, 10);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(2, result.getData().size());
        }
    }

    @Test
    void testComprehensiveScoreCalculation() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTenantId).thenReturn("default");
            securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            SupplierEntity supplierEntity = new SupplierEntity();
            supplierEntity.setId(1L);
            supplierEntity.setTenantId("default");
            supplierEntity.setDeleted(0);

            when(supplierRepository.selectById(1L)).thenReturn(supplierEntity);
            when(supplierPerformanceRepository.insert(any(SupplierPerformanceEntity.class))).thenReturn(1);
            when(supplierRepository.updateById(any(SupplierEntity.class))).thenReturn(1);

            SupplierPerformanceDTO performanceDTO = new SupplierPerformanceDTO();
            performanceDTO.setSupplierId(1L);
            performanceDTO.setEvaluationPeriod("2024-01");
            performanceDTO.setEvaluationType(1);
            performanceDTO.setQualityScore(100.0);
            performanceDTO.setDeliveryScore(100.0);
            performanceDTO.setPriceScore(100.0);
            performanceDTO.setServiceScore(100.0);
            performanceDTO.setResponseScore(100.0);

            R<Boolean> result = supplierService.evaluateSupplierPerformance(performanceDTO);

            assertTrue(result.isSuccess());
            // 综合评分应该是100分（所有维度都是满分）
            verify(supplierPerformanceRepository).insert(any(SupplierPerformanceEntity.class));
        }
    }
}