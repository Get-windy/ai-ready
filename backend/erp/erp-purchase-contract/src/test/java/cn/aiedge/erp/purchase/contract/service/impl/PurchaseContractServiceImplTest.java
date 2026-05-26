package cn.aiedge.erp.purchase.contract.service.impl;

import cn.aiedge.erp.purchase.contract.entity.PurchaseContract;
import cn.aiedge.erp.purchase.contract.repository.PurchaseContractRepository;
import cn.aiedge.erp.purchase.contract.service.dto.PurchaseContractDTO;
import cn.aiedge.erp.purchase.contract.service.dto.PurchaseContractQueryDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 采购合同服务实现类单元测试 - 更全面的测试覆盖
 */
@ExtendWith(MockitoExtension.class)
class PurchaseContractServiceImplTest {

    @Mock
    private PurchaseContractRepository purchaseContractRepository;

    @InjectMocks
    private PurchaseContractServiceImpl purchaseContractService;

    private PurchaseContract mockContract;
    private PurchaseContractDTO mockContractDTO;

    @BeforeEach
    void setUp() {
        // 创建模拟合同数据
        mockContract = new PurchaseContract();
        mockContract.setId(1L);
        mockContract.setContractNumber("PC-20240505001");
        mockContract.setContractName("年度服务器采购合同");
        mockContract.setSupplierId(1001L);
        mockContract.setSupplierName("戴尔科技有限公司");
        mockContract.setTotalAmount(new BigDecimal("500000.00"));
        mockContract.setTaxAmount(new BigDecimal("65000.00"));
        mockContract.setTotalAmountWithTax(new BigDecimal("565000.00"));
        mockContract.setCurrency("CNY");
        mockContract.setStartDate(new Date());
        mockContract.setEndDate(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
        mockContract.setPaymentTerms("货到付款，30天账期");
        mockContract.setStatus(1); // 草稿状态
        mockContract.setCreatedAt(new Date());
        mockContract.setUpdatedAt(new Date());
        mockContract.setIsDeleted(0);
        mockContract.setIsArchived(0);

        // 创建模拟DTO
        mockContractDTO = new PurchaseContractDTO();
        BeanUtils.copyProperties(mockContract, mockContractDTO);
    }

    @Test
    void testCreateContractWithValidData() {
        // Arrange
        when(purchaseContractRepository.insert(any(PurchaseContract.class)))
                .thenAnswer(invocation -> {
                    PurchaseContract contract = invocation.getArgument(0);
                    contract.setId(1L);
                    return 1;
                });

        // Act
        PurchaseContractDTO result = purchaseContractService.createContract(mockContractDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PC-20240505001", result.getContractNumber());
        assertEquals(new BigDecimal("500000.00"), result.getTotalAmount());
        assertEquals(1, result.getStatus());
        assertNotNull(result.getCreatedAt());

        verify(purchaseContractRepository, times(1)).insert(any(PurchaseContract.class));
    }

    @Test
    void testCreateContractWithNullTotalAmount() {
        // Arrange
        mockContractDTO.setTotalAmount(null);
        when(purchaseContractRepository.insert(any(PurchaseContract.class)))
                .thenAnswer(invocation -> {
                    PurchaseContract contract = invocation.getArgument(0);
                    contract.setId(1L);
                    return 1;
                });

        // Act
        PurchaseContractDTO result = purchaseContractService.createContract(mockContractDTO);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
    }

    @Test
    void testUpdateContractNotFound() {
        // Arrange
        when(purchaseContractRepository.selectById(999L)).thenReturn(null);

        mockContractDTO.setId(999L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> purchaseContractService.updateContract(mockContractDTO));

        assertEquals("采购合同不存在", exception.getMessage());
    }

    @Test
    void testDeleteContractSuccess() {
        // Arrange
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        // Act
        purchaseContractService.deleteContract(1L);

        // Assert
        verify(purchaseContractRepository, times(1)).updateById(argThat(contract ->
                contract.getIsDeleted() == 1 &&
                contract.getUpdatedAt() != null));
    }

    @Test
    void testGetContractByIdNotFound() {
        // Arrange
        when(purchaseContractRepository.selectById(999L)).thenReturn(null);

        // Act
        PurchaseContractDTO result = purchaseContractService.getContractById(999L);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetContractPageWithMultipleFilters() {
        // Arrange
        PurchaseContractQueryDTO queryDTO = new PurchaseContractQueryDTO();
        queryDTO.setContractNumber("PC-2024");
        queryDTO.setContractName("服务器");
        queryDTO.setSupplierId(1001L);
        queryDTO.setStatus(1);

        Page<PurchaseContract> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(mockContract));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        
        when(purchaseContractRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        Page<PurchaseContractDTO> result = purchaseContractService.getContractPage(queryDTO, 1, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPages());
        assertEquals("PC-20240505001", result.getRecords().get(0).getContractNumber());
    }

    @Test
    void testSubmitForApprovalInvalidStatus() {
        // Arrange
        mockContract.setStatus(5); // 已签署状态，不能提交审批
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> purchaseContractService.submitForApproval(1L));

        assertEquals("只有草稿状态的合同才能提交审批", exception.getMessage());
        verify(purchaseContractRepository, never()).updateById(any());
    }

    @Test
    void testApproveContractWithApprovedTrue() {
        // Arrange
        mockContract.setStatus(2); // 待审批状态
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        // Act
        purchaseContractService.approveContract(1L, true, "审批通过，符合采购政策");

        // Assert
        verify(purchaseContractRepository, times(1)).updateById(argThat(contract ->
                contract.getStatus() == 3 && // 已审批状态
                contract.getApprovalComment().equals("审批通过，符合采购政策") &&
                contract.getApprovalDate() != null &&
                contract.getRejectionReason() == null));
    }

    @Test
    void testApproveContractWithApprovedFalse() {
        // Arrange
        mockContract.setStatus(2); // 待审批状态
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        // Act
        purchaseContractService.approveContract(1L, false, "金额超出预算");

        // Assert
        verify(purchaseContractRepository, times(1)).updateById(argThat(contract ->
                contract.getStatus() == 4 && // 审批拒绝状态
                contract.getRejectionReason().equals("金额超出预算") &&
                contract.getApprovalDate() == null));
    }

    @Test
    void testSignContractSuccess() {
        // Arrange
        mockContract.setStatus(3); // 已审批状态
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        // Act
        purchaseContractService.signContract(1L, 2, "/contracts/signed/PC-20240505001.pdf");

        // Assert
        verify(purchaseContractRepository, times(1)).updateById(argThat(contract ->
                contract.getStatus() == 5 && // 已签署状态
                contract.getSignatureMethod() == 2 &&
                contract.getSignatureFilePath().equals("/contracts/signed/PC-20240505001.pdf") &&
                contract.getSignatureDate() != null));
    }

    @Test
    void testExportContract() {
        // Act
        byte[] result = purchaseContractService.exportContract(1L, "pdf");

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testGetContractStatistics() {
        // Act
        Object result = purchaseContractService.getContractStatistics("2024-01-01", "2024-05-05");

        // Assert
        assertNotNull(result);
    }

    @Test
    void testChangeContractSuccess() {
        // Arrange
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        mockContractDTO.setContractName("变更后的合同名称");
        mockContractDTO.setTotalAmount(new BigDecimal("550000.00"));

        // Act
        PurchaseContractDTO result = purchaseContractService.changeContract(1L, "价格调整", mockContractDTO);

        // Assert
        assertNotNull(result);
        assertEquals("变更后的合同名称", result.getContractName());
        assertEquals(new BigDecimal("550000.00"), result.getTotalAmount());

        verify(purchaseContractRepository, times(1)).updateById(any(PurchaseContract.class));
    }

    @Test
    void testTerminateContractSuccess() {
        // Arrange
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        // Act
        purchaseContractService.terminateContract(1L, "供应商停止营业");

        // Assert
        verify(purchaseContractRepository, times(1)).updateById(argThat(contract ->
                contract.getStatus() == 8 && // 已终止状态
                contract.getTerminationReason().equals("供应商停止营业") &&
                contract.getTerminationDate() != null));
    }

    @Test
    void testRenewContractCreatesNewContract() {
        // Arrange
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.insert(any(PurchaseContract.class)))
                .thenAnswer(invocation -> {
                    PurchaseContract contract = invocation.getArgument(0);
                    contract.setId(2L);
                    return 1;
                });

        PurchaseContractDTO newContractDTO = new PurchaseContractDTO();
        newContractDTO.setContractNumber("PC-20250505001");
        newContractDTO.setContractName("年度服务器采购续签合同");
        newContractDTO.setTotalAmount(new BigDecimal("520000.00"));

        // Act
        PurchaseContractDTO result = purchaseContractService.renewContract(1L, newContractDTO);

        // Assert
        assertNotNull(result);
        assertEquals("PC-20250505001", result.getContractNumber());
        assertEquals("年度服务器采购续签合同", result.getContractName());
        assertEquals(new BigDecimal("520000.00"), result.getTotalAmount());
        assertEquals(1L, result.getOriginalContractId());
        assertEquals(1, result.getStatus()); // 草稿状态

        verify(purchaseContractRepository, times(1)).insert(argThat(contract ->
                contract.getOriginalContractId() == 1L));
    }

    @Test
    void testArchiveContractSuccess() {
        // Arrange
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class))).thenReturn(1);

        // Act
        purchaseContractService.archiveContract(1L);

        // Assert
        verify(purchaseContractRepository, times(1)).updateById(argThat(contract ->
                contract.getIsArchived() == 1 &&
                contract.getArchivedDate() != null));
    }

    @Test
    void testGetContractExecutionProgress() {
        // Act
        Object result = purchaseContractService.getContractExecutionProgress(1L);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testImportContracts() {
        // Arrange
        byte[] fileBytes = "合同编号,合同名称,供应商,金额\nPC-001,服务器采购,戴尔,500000".getBytes();
        String fileName = "contracts.csv";

        // Act
        Object result = purchaseContractService.importContracts(fileBytes, fileName);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testExceptionHandlingInUpdateContract() {
        // Arrange
        when(purchaseContractRepository.selectById(1L)).thenReturn(mockContract);
        when(purchaseContractRepository.updateById(any(PurchaseContract.class)))
                .thenThrow(new RuntimeException("数据库更新失败"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> purchaseContractService.updateContract(mockContractDTO));

        assertEquals("数据库更新失败", exception.getMessage());
    }
}