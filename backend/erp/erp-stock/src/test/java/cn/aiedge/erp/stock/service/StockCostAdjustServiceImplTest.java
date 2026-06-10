package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import cn.aiedge.erp.stock.mapper.StockCostAdjustItemMapper;
import cn.aiedge.erp.stock.mapper.StockCostAdjustMapper;
import cn.aiedge.erp.stock.service.impl.StockCostAdjustServiceImpl;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("成本调价服务测试")
class StockCostAdjustServiceImplTest {

    @Mock
    private StockCostAdjustMapper adjustMapper;

    @Mock
    private StockCostAdjustItemMapper adjustItemMapper;

    @InjectMocks
    private StockCostAdjustServiceImpl adjustService;

    private StockCostAdjust testAdjust;
    private List<StockCostAdjustItem> testItems;

    @BeforeEach
    void setUp() {
        testAdjust = new StockCostAdjust();
        testAdjust.setId(1L);
        testAdjust.setAdjustNo("CA20260610TEST01");
        testAdjust.setWarehouseId(10L);
        testAdjust.setAdjustType(1);
        testAdjust.setStatus(0);
        testAdjust.setReasonType(1);
        testAdjust.setReasonDesc("市场波动");
        testAdjust.setTotalAdjustAmount(BigDecimal.ZERO);
        testAdjust.setApplicantId(100L);
        testAdjust.setApplyTime(LocalDateTime.now());

        testItems = new ArrayList<>();
        StockCostAdjustItem item = new StockCostAdjustItem();
        item.setId(1L);
        item.setAdjustId(1L);
        item.setProductId(1001L);
        item.setProductCode("P001");
        item.setProductName("测试产品");
        item.setCurrentQuantity(new BigDecimal("100"));
        item.setOldCost(new BigDecimal("10.00"));
        item.setNewCost(new BigDecimal("12.00"));
        testItems.add(item);
    }

    @Test
    @DisplayName("分页查询")
    void testPageList() {
        Page<StockCostAdjust> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testAdjust));
        when(adjustMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockCostAdjust> result = adjustService.pageList(null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("创建成本调价单")
    void testCreateAdjust() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(adjustMapper.insert(any(StockCostAdjust.class))).thenAnswer(invocation -> {
                StockCostAdjust a = invocation.getArgument(0);
                a.setId(2L);
                return 1;
            });

            StockCostAdjust adjust = new StockCostAdjust();
            adjust.setWarehouseId(10L);
            adjust.setAdjustType(1);
            adjust.setAdjustDate(java.time.LocalDate.now());
            adjust.setReasonType(2);
            adjust.setReasonDesc("供应商调价");

            StockCostAdjustItem item = new StockCostAdjustItem();
            item.setProductId(1001L);
            item.setProductCode("P001");
            item.setProductName("产品A");
            item.setCurrentQuantity(new BigDecimal("50"));
            item.setOldCost(new BigDecimal("10.00"));
            item.setNewCost(new BigDecimal("15.00"));

            StockCostAdjust result = adjustService.createAdjust(adjust, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            verify(adjustMapper).insert(any(StockCostAdjust.class));
            verify(adjustItemMapper).insert(any(StockCostAdjustItem.class));
        }
    }

    @Test
    @DisplayName("执行成本调价 - 计算差价")
    void testExecuteCalculateDiff() {
        testAdjust.setStatus(2);
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        when(adjustItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);

        StockCostAdjust result = adjustService.execute(1L);

        assertEquals(3, result.getStatus());
        // 价差 = (12 - 10) * 100 = 200
        assertEquals(new BigDecimal("200.00"), result.getTotalAdjustAmount());
        verify(adjustItemMapper).updateById(argThat(item ->
                ((StockCostAdjustItem) item).getDiffAmount().compareTo(new BigDecimal("200")) == 0));
    }

    @Test
    @DisplayName("执行成本调价 - 未审批状态异常")
    void testExecuteNotApproved() {
        testAdjust.setStatus(0);
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        assertThrows(BusinessException.class, () -> adjustService.execute(1L));
    }

    @Test
    @DisplayName("取消 - 草稿状态")
    void testCancelDraft() {
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        StockCostAdjust result = adjustService.cancel(1L, "取消调价");
        assertEquals(5, result.getStatus());
    }

    @Test
    @DisplayName("取消 - 已审批状态")
    void testCancelApproved() {
        testAdjust.setStatus(2);
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        StockCostAdjust result = adjustService.cancel(1L, "业务调整");
        assertEquals(5, result.getStatus());
    }

    @Test
    @DisplayName("取消 - 已执行异常")
    void testCancelExecuted() {
        testAdjust.setStatus(3);
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        assertThrows(BusinessException.class, () -> adjustService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("取消 - 已取消异常")
    void testCancelAlreadyCancelled() {
        testAdjust.setStatus(5);
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        assertThrows(BusinessException.class, () -> adjustService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("取消 - 单据不存在")
    void testCancelNotFound() {
        when(adjustMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> adjustService.cancel(999L, "test"));
    }

    @Test
    @DisplayName("获取明细")
    void testGetItems() {
        when(adjustItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);
        List<StockCostAdjustItem> items = adjustService.getItems(1L);
        assertEquals(1, items.size());
    }

    @Test
    @DisplayName("完整状态流转")
    void testFullWorkflow() {
        when(adjustMapper.selectById(1L)).thenReturn(testAdjust);
        StockCostAdjust submitted = adjustService.submitForApproval(1L);
        assertEquals(1, submitted.getStatus());

        testAdjust.setStatus(1);
        StockCostAdjust approved = adjustService.approve(1L, 200L, null);
        assertEquals(2, approved.getStatus());

        testAdjust.setStatus(2);
        when(adjustItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);
        StockCostAdjust executed = adjustService.execute(1L);
        assertEquals(3, executed.getStatus());
    }
}
