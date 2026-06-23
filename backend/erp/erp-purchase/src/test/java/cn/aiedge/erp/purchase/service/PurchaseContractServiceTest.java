package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseContractItem;
import cn.aiedge.erp.purchase.enums.ContractStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseContractMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseContractItemMapper;
import cn.aiedge.erp.purchase.service.impl.PurchaseContractServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 采购合同管理Service测试
 * 覆盖合同生成、审批、归档等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("采购合同管理测试")
class PurchaseContractServiceTest {

    @Mock
    private PurchaseContractMapper contractMapper;

    @Mock
    private PurchaseContractItemMapper itemMapper;

    @InjectMocks
    private PurchaseContractServiceImpl contractService;

    private PurchaseContract contract;
    private List<PurchaseContractItem> contractItems;

    @BeforeEach
    void setUp() {
        // 创建测试合同数据
        contract = new PurchaseContract();
        contract.setId(1L);
        contract.setContractNo("CT-2026-0001");
        contract.setInquiryId(100L);
        contract.setQuoteId(1L);
        contract.setSupplierId(1L);
        contract.setSupplierName("测试供应商");
        contract.setContractTitle("采购物资合同");
        contract.setContractType("采购合同");
        contract.setTotalAmount(BigDecimal.valueOf(10000));
        contract.setContractStatus(ContractStatus.DRAFT);
        contract.setStartDate(LocalDateTime.now());
        contract.setEndDate(LocalDateTime.now().plusYears(1));
        contract.setPaymentTerms("月结30天");
        contract.setDeliveryTerms("送货上门");
        contract.setCreatedBy(1L);

        // 创建合同明细
        contractItems = new ArrayList<>();
        PurchaseContractItem item1 = new PurchaseContractItem();
        item1.setContractId(1L);
        item1.setMaterialName("物料A");
        item1.setQuantity(BigDecimal.valueOf(100));
        item1.setUnitPrice(BigDecimal.valueOf(50));
        item1.setAmount(BigDecimal.valueOf(5000));

        PurchaseContractItem item2 = new PurchaseContractItem();
        item2.setContractId(1L);
        item2.setMaterialName("物料B");
        item2.setQuantity(BigDecimal.valueOf(50));
        item2.setUnitPrice(BigDecimal.valueOf(100));
        item2.setAmount(BigDecimal.valueOf(5000));

        contractItems.add(item1);
        contractItems.add(item2);
    }

    @Test
    @DisplayName("合同自动生成 - 从报价生成合同")
    void testGenerateContractFromQuote() {
        // Mock报价数据
        cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote quote =
            new cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote();
        quote.setId(1L);
        quote.setInquiryId(100L);
        quote.setSupplierId(1L);
        quote.setSupplierName("测试供应商");
        quote.setTotalAmount(BigDecimal.valueOf(10000));

        when(contractMapper.insert(any())).thenReturn(1);
        when(contractMapper.findById(anyLong())).thenReturn(contract);
        when(itemMapper.batchInsert(any())).thenReturn(contractItems.size());

        // 执行合同生成
        PurchaseContract generated = contractService.generateContractFromQuote(quote, contractItems);

        // 验证生成结果
        assertNotNull(generated, "生成的合同不应为空");
        assertNotNull(generated.getContractNo(), "合同编号应自动生成");
        assertTrue(generated.getContractNo().startsWith("CT-"), "合同编号应以CT开头");
        assertEquals(ContractStatus.DRAFT, generated.getContractStatus(), "初始状态应为草稿");
        assertEquals(BigDecimal.valueOf(10000), generated.getTotalAmount(), "合同金额应等于报价金额");
        assertEquals(1L, generated.getSupplierId(), "供应商ID应匹配");
        assertEquals(100L, generated.getInquiryId(), "询价单ID应匹配");

        verify(contractMapper).insert(any());
        verify(itemMapper).batchInsert(any());
    }

    @Test
    @DisplayName("合同审批提交 - 提交审批流程")
    void testSubmitForApproval() {
        // Mock数据
        contract.setContractStatus(ContractStatus.DRAFT);
        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);

        // 执行审批提交
        PurchaseContract submitted = contractService.submitForApproval(1L, "提交审批申请");

        // 验证提交结果
        assertNotNull(submitted, "提交结果不应为空");
        assertEquals(ContractStatus.PENDING_APPROVAL, submitted.getContractStatus(),
            "状态应变为待审批");
        assertNotNull(submitted.getSubmitTime(), "提交时间应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.PENDING_APPROVAL.name(), any());
    }

    @Test
    @DisplayName("合同审批通过 - 审批通过流程")
    void testApproveContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        contract.setSubmitTime(LocalDateTime.now().minusDays(1));

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateApprovalInfo(anyLong(), any(), any(), any())).thenReturn(1);

        // 执行审批通过
        PurchaseContract approved = contractService.approveContract(
            1L, 2L, "审批通过，合同条款合理", true
        );

        // 验证审批结果
        assertNotNull(approved, "审批结果不应为空");
        assertEquals(ContractStatus.APPROVED, approved.getContractStatus(),
            "状态应变为已审批");
        assertNotNull(approved.getApproverId(), "审批人ID应记录");
        assertNotNull(approved.getApprovalTime(), "审批时间应记录");
        assertNotNull(approved.getApprovalComment(), "审批意见应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.APPROVED.name(), any());
        verify(contractMapper).updateApprovalInfo(1L, 2L, "审批通过，合同条款合理", any());
    }

    @Test
    @DisplayName("合同审批驳回 - 审批驳回流程")
    void testRejectContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        contract.setSubmitTime(LocalDateTime.now().minusDays(1));

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateApprovalInfo(anyLong(), any(), any(), any())).thenReturn(1);

        // 执行审批驳回
        PurchaseContract rejected = contractService.approveContract(
            1L, 2L, "价格过高，需要重新协商", false
        );

        // 验证驳回结果
        assertNotNull(rejected, "驳回结果不应为空");
        assertEquals(ContractStatus.REJECTED, rejected.getContractStatus(),
            "状态应变为已驳回");
        assertNotNull(rejected.getApproverId(), "审批人ID应记录");
        assertEquals("价格过高，需要重新协商", rejected.getApprovalComment(),
            "驳回原因应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.REJECTED.name(), any());
    }

    @Test
    @DisplayName("合同生效 - 合同生效流程")
    void testActivateContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.APPROVED);

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateActivationTime(anyLong(), any())).thenReturn(1);

        // 执行合同生效
        PurchaseContract activated = contractService.activateContract(1L);

        // 验证生效结果
        assertNotNull(activated, "生效结果不应为空");
        assertEquals(ContractStatus.ACTIVE, activated.getContractStatus(),
            "状态应变为生效中");
        assertNotNull(activated.getActivationTime(), "生效时间应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.ACTIVE.name(), any());
        verify(contractMapper).updateActivationTime(1L, any());
    }

    @Test
    @DisplayName("合同履行监控 - 履行状态更新")
    void testUpdateExecutionProgress() {
        // Mock数据
        contract.setContractStatus(ContractStatus.ACTIVE);

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateExecutionProgress(anyLong(), any())).thenReturn(1);

        // 执行履行进度更新
        PurchaseContract updated = contractService.updateExecutionProgress(
            1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(50)
        );

        // 验证更新结果
        assertNotNull(updated, "更新结果不应为空");
        assertEquals(BigDecimal.valueOf(5000), updated.getExecutedAmount(),
            "已履行金额应更新");
        assertEquals(BigDecimal.valueOf(50), updated.getExecutedPercent(),
            "履行百分比应更新");

        verify(contractMapper).updateExecutionProgress(1L, any());
    }

    @Test
    @DisplayName("合同履行完成 - 合同完成流程")
    void testCompleteContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setExecutedAmount(BigDecimal.valueOf(10000));
        contract.setExecutedPercent(BigDecimal.valueOf(100));

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateCompletionTime(anyLong(), any())).thenReturn(1);

        // 执行合同完成
        PurchaseContract completed = contractService.completeContract(1L);

        // 验证完成结果
        assertNotNull(completed, "完成结果不应为空");
        assertEquals(ContractStatus.COMPLETED, completed.getContractStatus(),
            "状态应变为已完成");
        assertNotNull(completed.getCompletionTime(), "完成时间应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.COMPLETED.name(), any());
        verify(contractMapper).updateCompletionTime(1L, any());
    }

    @Test
    @DisplayName("合同终止 - 合同终止流程")
    void testTerminateContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.ACTIVE);

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateTerminationInfo(anyLong(), any(), any())).thenReturn(1);

        // 执行合同终止
        PurchaseContract terminated = contractService.terminateContract(
            1L, "供应商违约，提前终止"
        );

        // 验证终止结果
        assertNotNull(terminated, "终止结果不应为空");
        assertEquals(ContractStatus.TERMINATED, terminated.getContractStatus(),
            "状态应变为已终止");
        assertNotNull(terminated.getTerminationTime(), "终止时间应记录");
        assertEquals("供应商违约，提前终止", terminated.getTerminationReason(),
            "终止原因应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.TERMINATED.name(), any());
        verify(contractMapper).updateTerminationInfo(1L, any(), "供应商违约，提前终止");
    }

    @Test
    @DisplayName("合同归档 - 合同归档流程")
    void testArchiveContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.COMPLETED);

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateArchiveInfo(anyLong(), any(), any())).thenReturn(1);

        // 执行合同归档
        PurchaseContract archived = contractService.archiveContract(
            1L, "ARCH-2026-0001", "合同已履行完毕，归档保存"
        );

        // 验证归档结果
        assertNotNull(archived, "归档结果不应为空");
        assertEquals(ContractStatus.ARCHIVED, archived.getContractStatus(),
            "状态应变为已归档");
        assertEquals("ARCH-2026-0001", archived.getArchiveNo(),
            "归档编号应记录");
        assertNotNull(archived.getArchiveTime(), "归档时间应记录");

        verify(contractMapper).updateStatus(1L, ContractStatus.ARCHIVED.name(), any());
        verify(contractMapper).updateArchiveInfo(1L, "ARCH-2026-0001", any());
    }

    @Test
    @DisplayName("合同查询 - 根据供应商查询合同")
    void testQueryContractsBySupplier() {
        // Mock数据
        PurchaseContract contract2 = new PurchaseContract();
        contract2.setId(2L);
        contract2.setSupplierId(1L);
        contract2.setContractStatus(ContractStatus.ACTIVE);

        when(contractMapper.findBySupplierId(1L)).thenReturn(Arrays.asList(contract, contract2));

        // 执行查询
        List<PurchaseContract> contracts = contractService.queryContractsBySupplier(1L);

        // 验证查询结果
        assertNotNull(contracts, "查询结果不应为空");
        assertEquals(2, contracts.size(), "应有2个合同");
        assertTrue(contracts.stream().allMatch(c -> c.getSupplierId().equals(1L)),
            "所有合同应属于同一供应商");
    }

    @Test
    @DisplayName("合同查询 - 根据状态查询合同")
    void testQueryContractsByStatus() {
        // Mock数据
        PurchaseContract contract2 = new PurchaseContract();
        contract2.setId(2L);
        contract2.setContractStatus(ContractStatus.ACTIVE);

        when(contractMapper.findByStatus(ContractStatus.ACTIVE))
            .thenReturn(Arrays.asList(contract2));

        // 执行查询
        List<PurchaseContract> contracts = contractService.queryContractsByStatus(ContractStatus.ACTIVE);

        // 验证查询结果
        assertNotNull(contracts, "查询结果不应为空");
        assertTrue(contracts.stream().allMatch(c -> c.getContractStatus() == ContractStatus.ACTIVE),
            "所有合同状态应匹配");
    }

    @Test
    @DisplayName("合同明细查询 - 查询合同明细")
    void testGetContractItems() {
        // Mock数据
        when(itemMapper.findByContractId(1L)).thenReturn(contractItems);

        // 执行查询
        List<PurchaseContractItem> items = contractService.getContractItems(1L);

        // 验证查询结果
        assertNotNull(items, "明细查询结果不应为空");
        assertEquals(2, items.size(), "应有2个明细项");
        assertTrue(items.stream().allMatch(i -> i.getContractId().equals(1L)),
            "所有明细应属于同一合同");

        // 验证金额计算
        BigDecimal totalAmount = items.stream()
            .map(PurchaseContractItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.valueOf(10000), totalAmount, "明细总金额应为10000");
    }

    @Test
    @DisplayName("合同编号生成 - 编号规则验证")
    void testGenerateContractNo() {
        // 执行编号生成
        String contractNo = contractService.generateContractNo();

        // 验证编号规则
        assertNotNull(contractNo, "合同编号不应为空");
        assertTrue(contractNo.startsWith("CT-"), "合同编号应以CT开头");
        assertTrue(contractNo.matches("CT-\\d{4}-\\d{4}"), "合同编号应符合格式CT-YYYY-XXXX");

        // 验证年份
        int year = LocalDateTime.now().getYear();
        assertTrue(contractNo.contains(String.valueOf(year)), "编号应包含当前年份");
    }

    @Test
    @DisplayName("合同金额计算 - 明细金额汇总")
    void testCalculateContractAmount() {
        // 执行金额计算
        BigDecimal totalAmount = contractService.calculateContractAmount(contractItems);

        // 验证金额计算
        assertNotNull(totalAmount, "总金额不应为空");
        assertEquals(BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP),
            totalAmount.setScale(2, RoundingMode.HALF_UP),
            "总金额应为明细汇总（5000+5000）");
    }

    @Test
    @DisplayName("合同条款验证 - 必填条款检查")
    void testValidateContractTerms() {
        // Mock数据 - 完整条款合同
        contract.setPaymentTerms("月结30天");
        contract.setDeliveryTerms("送货上门");
        contract.setQualityStandard("国家标准");
        contract.setWarrantyPeriod("12个月");

        when(contractMapper.findById(1L)).thenReturn(contract);

        // 执行条款验证
        boolean isValid = contractService.validateContractTerms(1L);

        // 验证验证结果
        assertTrue(isValid, "完整条款合同应验证通过");
    }

    @Test
    @DisplayName("合同条款验证 - 缺失条款检查")
    void testValidateContractTermsMissing() {
        // Mock数据 - 缺失条款合同
        contract.setPaymentTerms(null);
        contract.setDeliveryTerms(null);

        when(contractMapper.findById(1L)).thenReturn(contract);

        // 执行条款验证
        boolean isValid = contractService.validateContractTerms(1L);

        // 验证验证结果
        assertFalse(isValid, "缺失必要条款合同应验证失败");
    }

    @Test
    @DisplayName("合同到期提醒 - 到期合同查询")
    void testQueryExpiringContracts() {
        // Mock数据 - 即将到期合同
        PurchaseContract expiringContract = new PurchaseContract();
        expiringContract.setId(2L);
        expiringContract.setContractStatus(ContractStatus.ACTIVE);
        expiringContract.setEndDate(LocalDateTime.now().plusDays(7)); // 7天后到期

        when(contractMapper.findExpiringContracts(30)).thenReturn(Arrays.asList(expiringContract));

        // 执行到期查询
        List<PurchaseContract> expiringContracts = contractService.queryExpiringContracts(30);

        // 验证查询结果
        assertNotNull(expiringContracts, "查询结果不应为空");
        assertFalse(expiringContracts.isEmpty(), "应有即将到期合同");
        assertTrue(expiringContracts.stream().allMatch(c -> c.getContractStatus() == ContractStatus.ACTIVE),
            "所有到期提醒合同应为生效状态");
    }

    @Test
    @DisplayName("合同续签 - 合同续签流程")
    void testRenewContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setEndDate(LocalDateTime.now().plusDays(7));

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.insert(any())).thenReturn(1);

        // 执行合同续签
        PurchaseContract renewed = contractService.renewContract(1L, 12, "续签12个月");

        // 验证续签结果
        assertNotNull(renewed, "续签合同不应为空");
        assertNotNull(renewed.getContractNo(), "续签合同应有新编号");
        assertEquals(ContractStatus.DRAFT, renewed.getContractStatus(),
            "续签合同初始状态应为草稿");
        assertTrue(renewed.getEndDate().isAfter(contract.getEndDate()),
            "续签合同结束日期应延长");

        verify(contractMapper).insert(any());
    }

    @Test
    @DisplayName("合同变更 - 合同变更流程")
    void testModifyContract() {
        // Mock数据
        contract.setContractStatus(ContractStatus.ACTIVE);

        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.insertModification(anyLong(), anyString(), anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(contractMapper.update(any())).thenReturn(1);

        // 执行合同变更
        PurchaseContract modified = contractService.modifyContract(
            1L, "增加采购数量", "数量从100增加到150"
        );

        // 验证变更结果
        assertNotNull(modified, "变更结果不应为空");
        assertNotNull(modified.getModificationNo(), "变更应有编号");
        assertEquals("增加采购数量", modified.getModificationReason(), "变更原因应记录");

        verify(contractMapper).insertModification(anyLong(), anyString(), anyString(), any(LocalDateTime.class));
        verify(contractMapper).update(any());
    }

    @Test
    @DisplayName("合同统计 - 合同统计数据生成")
    void testGenerateContractStatistics() {
        // Mock数据
        when(contractMapper.countByStatus(ContractStatus.ACTIVE)).thenReturn(5L);
        when(contractMapper.countByStatus(ContractStatus.COMPLETED)).thenReturn(10L);
        when(contractMapper.countByStatus(ContractStatus.PENDING_APPROVAL)).thenReturn(2L);
        when(contractMapper.sumAmountByStatus(ContractStatus.ACTIVE))
            .thenReturn(BigDecimal.valueOf(50000));
        when(contractMapper.sumAmountByStatus(ContractStatus.COMPLETED))
            .thenReturn(BigDecimal.valueOf(100000));

        // 执行统计生成
        var statistics = contractService.generateContractStatistics();

        // 验证统计结果
        assertNotNull(statistics, "统计数据不应为空");
        assertEquals(5L, statistics.getActiveCount(), "生效合同数应为5");
        assertEquals(10L, statistics.getCompletedCount(), "已完成合同数应为10");
        assertEquals(2L, statistics.getPendingCount(), "待审批合同数应为2");
        assertEquals(BigDecimal.valueOf(50000), statistics.getActiveAmount(), "生效合同总金额应为50000");
        assertEquals(BigDecimal.valueOf(100000), statistics.getCompletedAmount(),
            "已完成合同总金额应为100000");
    }
}