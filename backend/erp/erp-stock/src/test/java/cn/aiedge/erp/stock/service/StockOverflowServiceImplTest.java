package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockOverflowItemVO;
import cn.aiedge.erp.stock.dto.StockOverflowQuery;
import cn.aiedge.erp.stock.entity.StockOverflow;
import cn.aiedge.erp.stock.entity.StockOverflowItem;
import cn.aiedge.erp.stock.mapper.StockOverflowItemMapper;
import cn.aiedge.erp.stock.mapper.StockOverflowMapper;
import cn.aiedge.erp.stock.service.impl.StockOverflowServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("报溢单服务测试")
class StockOverflowServiceImplTest {

    @Mock
    private StockOverflowMapper overflowMapper;

    @Mock
    private StockOverflowItemMapper overflowItemMapper;

    @InjectMocks
    private StockOverflowServiceImpl overflowService;

    private StockOverflow testOverflow;
    private List<StockOverflowItem> testItems;

    @BeforeEach
    void setUp() {
        testOverflow = new StockOverflow();
        testOverflow.setId(1L);
        testOverflow.setTenantId(1L);
        testOverflow.setOverflowNo("BYD20260610TEST01");
        testOverflow.setWarehouseId(10L);
        testOverflow.setOverflowDate(LocalDate.now());
        testOverflow.setSourceType("1");
        testOverflow.setStatus(0);
        testOverflow.setTotalQuantity(new BigDecimal("50"));
        testOverflow.setTotalAmount(new BigDecimal("5000"));
        testOverflow.setTotalItems(2);
        testOverflow.setApplicantId(100L);
        testOverflow.setCreateTime(LocalDateTime.now());

        testItems = new ArrayList<>();
        StockOverflowItem item1 = new StockOverflowItem();
        item1.setId(1L);
        item1.setOverflowId(1L);
        item1.setProductId(1001L);
        item1.setProductCode("P001");
        item1.setProductName("测试产品A");
        item1.setQuantity(new BigDecimal("30"));
        item1.setUnitCost(new BigDecimal("100"));
        item1.setAmount(new BigDecimal("3000"));
        testItems.add(item1);

        StockOverflowItem item2 = new StockOverflowItem();
        item2.setId(2L);
        item2.setOverflowId(1L);
        item2.setProductId(1002L);
        item2.setProductCode("P002");
        item2.setProductName("测试产品B");
        item2.setQuantity(new BigDecimal("20"));
        item2.setUnitCost(new BigDecimal("100"));
        item2.setAmount(new BigDecimal("2000"));
        testItems.add(item2);
    }

    @Test
    @DisplayName("分页查询 - 无条件")
    void testPageListNoFilters() {
        Page<StockOverflow> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testOverflow));
        when(overflowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockOverflow> result = overflowService.pageList(new StockOverflowQuery());

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(overflowMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询 - 带条件")
    void testPageListWithFilters() {
        Page<StockOverflow> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testOverflow));
        when(overflowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        StockOverflowQuery q = new StockOverflowQuery();
        q.setOverflowNo("BYD");
        q.setWarehouseId(10L);
        q.setStatus(0);
        q.setHandlerName("张三");
        q.setDeptName("仓储部");
        q.setDateStart("2026-06-01");
        q.setDateEnd("2026-06-30");
        Page<StockOverflow> result = overflowService.pageList(q);

        assertNotNull(result);
        verify(overflowMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("按明细分页 - 无匹配单据返回空页")
    void testPageDetailNoMatch() {
        when(overflowMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Page<StockOverflowItemVO> result = overflowService.pageDetail(new StockOverflowQuery());

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    @DisplayName("按明细分页 - 有数据")
    void testPageDetailWithData() {
        StockOverflowItem itemPageRow = testItems.get(0);
        Page<StockOverflowItem> itemPage = new Page<>(1, 10);
        itemPage.setRecords(List.of(itemPageRow));
        itemPage.setTotal(1);
        when(overflowMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testOverflow));
        when(overflowItemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(itemPage);
        when(overflowMapper.selectBatchIds(anyCollection())).thenReturn(List.of(testOverflow));
        when(overflowMapper.selectByIds(anyCollection())).thenReturn(List.of(testOverflow));

        Page<StockOverflowItemVO> result = overflowService.pageDetail(new StockOverflowQuery());

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("BYD20260610TEST01", result.getRecords().get(0).getOverflowNo());
    }

    @Test
    @DisplayName("创建报溢单 - 含明细")
    void testCreateOverflowWithItems() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            stpUtil.when(StpUtil::getLoginId).thenReturn(100L);
            when(overflowMapper.insert(any(StockOverflow.class))).thenAnswer(invocation -> {
                StockOverflow o = invocation.getArgument(0);
                o.setId(2L);
                return 1;
            });

            StockOverflow overflow = new StockOverflow();
            overflow.setWarehouseId(10L);
            overflow.setOverflowDate(LocalDate.now());
            overflow.setSourceType("1");
            overflow.setRemark("测试报溢");

            StockOverflowItem item = new StockOverflowItem();
            item.setProductId(1001L);
            item.setProductCode("P001");
            item.setProductName("测试产品");
            item.setQuantity(new BigDecimal("10"));
            item.setUnitCost(new BigDecimal("50"));

            StockOverflow result = overflowService.createOverflow(overflow, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertTrue(result.getOverflowNo().startsWith("BYD-"));
            assertEquals(new BigDecimal("10"), result.getTotalQuantity());
            assertNotNull(result.getCreatorName());
            verify(overflowMapper).insert(any(StockOverflow.class));
            verify(overflowItemMapper).insert(any(StockOverflowItem.class));
        }
    }

    @Test
    @DisplayName("创建报溢单 - 无明细")
    void testCreateOverflowWithoutItems() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            stpUtil.when(StpUtil::getLoginId).thenReturn(100L);
            when(overflowMapper.insert(any(StockOverflow.class))).thenAnswer(invocation -> {
                StockOverflow o = invocation.getArgument(0);
                o.setId(3L);
                return 1;
            });

            StockOverflow overflow = new StockOverflow();
            overflow.setWarehouseId(10L);
            overflow.setOverflowDate(LocalDate.now());
            overflow.setSourceType("1");

            StockOverflow result = overflowService.createOverflow(overflow, null);

            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.getTotalQuantity());
            assertEquals(BigDecimal.ZERO, result.getTotalAmount());
            assertEquals(0, result.getTotalItems());
            verify(overflowItemMapper, never()).insert(any(StockOverflowItem.class));
        }
    }

    @Test
    @DisplayName("更新报溢单 - 草稿")
    void testUpdateOverflow() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(overflowMapper.selectById(1L)).thenReturn(testOverflow);
            when(overflowMapper.updateById(any(StockOverflow.class))).thenReturn(1);

            StockOverflow input = new StockOverflow();
            input.setOverflowNo("BYD20260610TEST01");
            input.setWarehouseId(10L);
            input.setOverflowDate(LocalDate.now());
            input.setSourceType("1");
            StockOverflowItem item = new StockOverflowItem();
            item.setProductId(1001L);
            item.setQuantity(new BigDecimal("5"));
            item.setUnitCost(new BigDecimal("60"));

            StockOverflow result = overflowService.updateOverflow(1L, input, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertEquals(new BigDecimal("5"), result.getTotalQuantity());
            verify(overflowItemMapper).delete(any(LambdaQueryWrapper.class));
            verify(overflowItemMapper).insert(any(StockOverflowItem.class));
        }
    }

    @Test
    @DisplayName("提交审批 - 草稿→待审批")
    void testSubmitForApproval() {
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        StockOverflow result = overflowService.submitForApproval(1L);

        assertEquals(1, result.getStatus());
        verify(overflowMapper).updateById(argThat((StockOverflow o) -> o.getStatus() == 1));
    }

    @Test
    @DisplayName("提交审批 - 非草稿状态抛出异常")
    void testSubmitForApprovalInvalidStatus() {
        testOverflow.setStatus(1);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        assertThrows(BusinessException.class, () -> overflowService.submitForApproval(1L));
    }

    @Test
    @DisplayName("审批通过 - 待审批→已审批")
    void testApprove() {
        testOverflow.setStatus(1);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        StockOverflow result = overflowService.approve(1L, 200L, "审批通过");

        assertEquals(2, result.getStatus());
        assertEquals(200L, result.getApprovedBy());
        verify(overflowMapper).updateById(argThat((StockOverflow o) -> o.getStatus() == 2));
    }

    @Test
    @DisplayName("审批通过 - 非待审批状态抛出异常")
    void testApproveInvalidStatus() {
        testOverflow.setStatus(0);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        assertThrows(BusinessException.class, () -> overflowService.approve(1L, 200L, null));
    }

    @Test
    @DisplayName("审批拒绝 - 待审批→已拒绝")
    void testReject() {
        testOverflow.setStatus(1);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        StockOverflow result = overflowService.reject(1L, "库存不符");

        assertEquals(4, result.getStatus());
        verify(overflowMapper).updateById(argThat((StockOverflow o) -> o.getStatus() == 4));
    }

    @Test
    @DisplayName("执行入库 - 已审批→已完成(写记账人)")
    void testExecute() {
        testOverflow.setStatus(2);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            stpUtil.when(StpUtil::getLoginId).thenReturn(100L);

            StockOverflow result = overflowService.execute(1L);

            assertEquals(3, result.getStatus());
            assertEquals(100L, result.getBookkeeperId());
            assertNotNull(result.getBookkeepingTime());
            verify(overflowMapper).updateById(argThat((StockOverflow o) -> o.getStatus() == 3));
        }
    }

    @Test
    @DisplayName("取消 - 草稿状态")
    void testCancelDraft() {
        testOverflow.setStatus(0);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        StockOverflow result = overflowService.cancel(1L, "不再需要");

        assertEquals(5, result.getStatus());
        assertEquals("不再需要", result.getCancelReason());
        verify(overflowMapper).updateById(argThat((StockOverflow o) -> o.getStatus() == 5));
    }

    @Test
    @DisplayName("取消 - 已审批状态")
    void testCancelApproved() {
        testOverflow.setStatus(2);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        StockOverflow result = overflowService.cancel(1L, "业务调整");

        assertEquals(5, result.getStatus());
        verify(overflowMapper).updateById(argThat((StockOverflow o) -> o.getStatus() == 5));
    }

    @Test
    @DisplayName("取消 - 已执行单据抛出异常")
    void testCancelExecuted() {
        testOverflow.setStatus(3);
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);

        assertThrows(BusinessException.class, () -> overflowService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("取消 - 不存在的单据抛出异常")
    void testCancelNotFound() {
        when(overflowMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> overflowService.cancel(999L, "test"));
    }

    @Test
    @DisplayName("获取明细")
    void testGetItems() {
        when(overflowItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);

        List<StockOverflowItem> items = overflowService.getItems(1L);

        assertEquals(2, items.size());
        verify(overflowItemMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取明细 - 无数据")
    void testGetItemsEmpty() {
        when(overflowItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StockOverflowItem> items = overflowService.getItems(999L);

        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("获取详情 - 带明细")
    void testGetDetail() {
        when(overflowMapper.selectById(1L)).thenReturn(testOverflow);
        when(overflowItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);

        StockOverflow result = overflowService.getDetail(1L);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
    }

    @Test
    @DisplayName("状态流程: 草稿→提交→审批→执行")
    void testFullWorkflow() {
        when(overflowMapper.selectById(1L))
                .thenReturn(testOverflow)  // submit
                .thenReturn(testOverflow); // approve (status updated by spy)
        testOverflow.setStatus(2);  // for execute
        when(overflowMapper.selectById(1L))
                .thenReturn(testOverflow);  // execute
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            stpUtil.when(StpUtil::getLoginId).thenReturn(100L);

            // Submit
            StockOverflow submitted = overflowService.submitForApproval(1L);
            assertEquals(1, submitted.getStatus());
            testOverflow.setStatus(1);

            // Approve
            StockOverflow approved = overflowService.approve(1L, 200L, null);
            assertEquals(2, approved.getStatus());
            testOverflow.setStatus(2);

            // Execute
            StockOverflow executed = overflowService.execute(1L);
            assertEquals(3, executed.getStatus());
        }
    }
}
