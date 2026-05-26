package com.aiready.finance;

import com.aiready.finance.dto.*;
import com.aiready.finance.entity.AccountSubject;
import com.aiready.finance.mapper.AccountSubjectMapper;
import com.aiready.finance.service.FinanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 财务管理模块单元测试
 */
@SpringBootTest
@Transactional
public class FinanceModuleTest {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private AccountSubjectMapper accountSubjectMapper;

    // ==================== 会计科目测试 ====================

    @Test
    public void testCreateSubject() {
        AccountSubjectSaveRequest request = new AccountSubjectSaveRequest();
        request.setSubjectCode("1003");
        request.setSubjectName("测试科目");
        request.setSubjectType(1);
        request.setSubjectLevel(1);
        request.setBalanceDirection(1);
        request.setOpeningBalance(new BigDecimal("10000.00"));
        request.setIsDetail(1);
        request.setStatus(1);

        AccountSubjectDTO result = financeService.createSubject(request, 1L);

        assertNotNull(result);
        assertEquals("1003", result.getSubjectCode());
        assertEquals("测试科目", result.getSubjectName());
        assertEquals(new BigDecimal("10000.00"), result.getOpeningBalance());
    }

    @Test
    public void testUpdateSubject() {
        // 先创建
        AccountSubjectSaveRequest createRequest = new AccountSubjectSaveRequest();
        createRequest.setSubjectCode("1004");
        createRequest.setSubjectName("测试科目");
        createRequest.setSubjectType(1);
        createRequest.setSubjectLevel(1);
        createRequest.setBalanceDirection(1);
        AccountSubjectDTO created = financeService.createSubject(createRequest, 1L);

        // 再更新
        AccountSubjectSaveRequest updateRequest = new AccountSubjectSaveRequest();
        updateRequest.setSubjectCode("1004");
        updateRequest.setSubjectName("更新后的科目");
        updateRequest.setSubjectType(1);
        updateRequest.setSubjectLevel(1);
        updateRequest.setBalanceDirection(1);

        AccountSubjectDTO updated = financeService.updateSubject(created.getId(), updateRequest, 1L);

        assertNotNull(updated);
        assertEquals("更新后的科目", updated.getSubjectName());
    }

    @Test
    public void testDeleteSubject() {
        AccountSubjectSaveRequest request = new AccountSubjectSaveRequest();
        request.setSubjectCode("1005");
        request.setSubjectName("待删除科目");
        request.setSubjectType(1);
        request.setSubjectLevel(1);
        request.setBalanceDirection(1);
        AccountSubjectDTO created = financeService.createSubject(request, 1L);

        financeService.deleteSubject(created.getId(), 1L);

        AccountSubjectDTO deleted = financeService.getSubjectById(created.getId());
        assertNull(deleted);
    }

    @Test
    public void testGetSubjectByCode() {
        AccountSubjectDTO subject = financeService.getSubjectByCode("1001");
        assertNotNull(subject);
        assertEquals("库存现金", subject.getSubjectName());
    }

    @Test
    public void testQuerySubjects() {
        AccountSubjectQueryRequest request = new AccountSubjectQueryRequest();
        request.setSubjectType(1);
        request.setCurrent(1L);
        request.setSize(10L);

        IPage<AccountSubjectDTO> result = financeService.querySubjects(request);

        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
    }

    @Test
    public void testGetSubjectTree() {
        List<AccountSubjectDTO> tree = financeService.getSubjectTree();
        assertNotNull(tree);
        // 验证树形结构
        for (AccountSubjectDTO dto : tree) {
            assertNotNull(dto.getSubjectCode());
        }
    }

    @Test
    public void testValidateSubjectCode() {
        boolean valid = financeService.validateSubjectCode("9999", null);
        assertTrue(valid);

        boolean invalid = financeService.validateSubjectCode("1001", null);
        assertFalse(invalid);
    }

    // ==================== 凭证管理测试 ====================

    @Test
    public void testCreateVoucher() {
        VoucherSaveRequest request = new VoucherSaveRequest();
        request.setVoucherDate(LocalDate.now());
        request.setVoucherType(3);
        request.setSummary("测试凭证");

        List<VoucherItemSaveRequest> items = new ArrayList<>();

        // 借方分录
        VoucherItemSaveRequest debitItem = new VoucherItemSaveRequest();
        debitItem.setSubjectId(1L);
        debitItem.setSummary("借方摘要");
        debitItem.setDebitAmount(new BigDecimal("1000.00"));
        items.add(debitItem);

        // 贷方分录
        VoucherItemSaveRequest creditItem = new VoucherItemSaveRequest();
        creditItem.setSubjectId(2L);
        creditItem.setSummary("贷方摘要");
        creditItem.setCreditAmount(new BigDecimal("1000.00"));
        items.add(creditItem);

        request.setItems(items);

        VoucherDTO result = financeService.createVoucher(request, 1L);

        assertNotNull(result);
        assertNotNull(result.getVoucherNo());
        assertEquals(new BigDecimal("1000.00"), result.getTotalDebit());
        assertEquals(new BigDecimal("1000.00"), result.getTotalCredit());
    }

    @Test
    public void testCreateVoucherWithUnbalancedAmount() {
        VoucherSaveRequest request = new VoucherSaveRequest();
        request.setVoucherDate(LocalDate.now());
        request.setVoucherType(3);
        request.setSummary("借贷不平衡凭证");

        List<VoucherItemSaveRequest> items = new ArrayList<>();

        VoucherItemSaveRequest debitItem = new VoucherItemSaveRequest();
        debitItem.setSubjectId(1L);
        debitItem.setDebitAmount(new BigDecimal("1000.00"));
        items.add(debitItem);

        VoucherItemSaveRequest creditItem = new VoucherItemSaveRequest();
        creditItem.setSubjectId(2L);
        creditItem.setCreditAmount(new BigDecimal("500.00"));
        items.add(creditItem);

        request.setItems(items);

        assertThrows(RuntimeException.class, () -> {
            financeService.createVoucher(request, 1L);
        });
    }

    @Test
    public void testGetVoucherById() {
        // 先创建凭证
        VoucherSaveRequest request = new VoucherSaveRequest();
        request.setVoucherDate(LocalDate.now());
        request.setVoucherType(3);
        request.setSummary("查询测试凭证");

        List<VoucherItemSaveRequest> items = new ArrayList<>();
        VoucherItemSaveRequest item1 = new VoucherItemSaveRequest();
        item1.setSubjectId(1L);
        item1.setDebitAmount(new BigDecimal("500.00"));
        items.add(item1);

        VoucherItemSaveRequest item2 = new VoucherItemSaveRequest();
        item2.setSubjectId(2L);
        item2.setCreditAmount(new BigDecimal("500.00"));
        items.add(item2);

        request.setItems(items);

        VoucherDTO created = financeService.createVoucher(request, 1L);

        // 查询
        VoucherDTO found = financeService.getVoucherById(created.getId());

        assertNotNull(found);
        assertEquals("查询测试凭证", found.getSummary());
        assertNotNull(found.getItems());
        assertEquals(2, found.getItems().size());
    }

    @Test
    public void testReviewVoucher() {
        // 创建凭证
        VoucherSaveRequest request = new VoucherSaveRequest();
        request.setVoucherDate(LocalDate.now());
        request.setVoucherType(3);
        request.setSummary("审核测试凭证");

        List<VoucherItemSaveRequest> items = new ArrayList<>();
        VoucherItemSaveRequest item1 = new VoucherItemSaveRequest();
        item1.setSubjectId(1L);
        item1.setDebitAmount(new BigDecimal("1000.00"));
        items.add(item1);

        VoucherItemSaveRequest item2 = new VoucherItemSaveRequest();
        item2.setSubjectId(2L);
        item2.setCreditAmount(new BigDecimal("1000.00"));
        items.add(item2);

        request.setItems(items);

        VoucherDTO created = financeService.createVoucher(request, 1L);
        assertEquals(1, created.getStatus()); // 待审核

        // 审核
        financeService.reviewVoucher(created.getId(), 2L);

        VoucherDTO reviewed = financeService.getVoucherById(created.getId());
        assertEquals(2, reviewed.getStatus()); // 已审核
    }

    @Test
    public void testQueryVouchers() {
        VoucherQueryRequest request = new VoucherQueryRequest();
        request.setCurrent(1L);
        request.setSize(10L);

        IPage<VoucherDTO> result = financeService.queryVouchers(request);

        assertNotNull(result);
    }

    // ==================== 账簿管理测试 ====================

    @Test
    public void testQueryDetailLedger() {
        LedgerQueryRequest request = new LedgerQueryRequest();
        request.setCurrent(1L);
        request.setSize(10L);

        IPage<LedgerDTO> result = financeService.queryDetailLedger(request);

        assertNotNull(result);
    }

    @Test
    public void testQueryGeneralLedger() {
        LedgerQueryRequest request = new LedgerQueryRequest();
        request.setCurrent(1L);
        request.setSize(10L);

        IPage<LedgerDTO> result = financeService.queryGeneralLedger(request);

        assertNotNull(result);
    }

    // ==================== 财务报表测试 ====================

    @Test
    public void testGenerateBalanceSheet() {
        Map<String, Object> result = financeService.generateBalanceSheet("2026-04");

        assertNotNull(result);
        assertTrue(result.containsKey("assets"));
        assertTrue(result.containsKey("liabilities"));
        assertTrue(result.containsKey("equity"));
        assertTrue(result.containsKey("totalAssets"));
        assertTrue(result.containsKey("totalLiabilities"));
        assertTrue(result.containsKey("totalEquity"));
    }

    @Test
    public void testGenerateIncomeStatement() {
        Map<String, Object> result = financeService.generateIncomeStatement("2026-01", "2026-04");

        assertNotNull(result);
        assertTrue(result.containsKey("revenues"));
        assertTrue(result.containsKey("expenses"));
        assertTrue(result.containsKey("totalRevenue"));
        assertTrue(result.containsKey("totalExpense"));
        assertTrue(result.containsKey("grossProfit"));
    }

    @Test
    public void testGenerateCashFlowStatement() {
        Map<String, Object> result = financeService.generateCashFlowStatement("2026-01", "2026-04");

        assertNotNull(result);
        assertTrue(result.containsKey("operatingActivities"));
        assertTrue(result.containsKey("investingActivities"));
        assertTrue(result.containsKey("financingActivities"));
        assertTrue(result.containsKey("netCashFlow"));
    }
}
