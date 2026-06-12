package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockSplit;
import cn.aiedge.erp.stock.entity.StockSplitItem;
import cn.aiedge.erp.stock.mapper.StockSplitItemMapper;
import cn.aiedge.erp.stock.mapper.StockSplitMapper;
import cn.aiedge.erp.stock.service.impl.StockSplitServiceImpl;
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
@DisplayName("拆分单服务测试")
class StockSplitServiceImplTest {

    @Mock
    private StockSplitMapper splitMapper;

    @Mock
    private StockSplitItemMapper splitItemMapper;

    @InjectMocks
    private StockSplitServiceImpl splitService;

    private StockSplit testSplit;

    @BeforeEach
    void setUp() {
        testSplit = new StockSplit();
        testSplit.setId(1L);
        testSplit.setSplitNo("SP20260610TEST01");
        testSplit.setWarehouseId(10L);
        testSplit.setBomId(1L);
        testSplit.setProductCode("P001");
        testSplit.setProductName("成品A");
        testSplit.setSplitQuantity(new BigDecimal("5"));
        testSplit.setOutputTotalCost(new BigDecimal("2500"));
        testSplit.setStatus(0);
        testSplit.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("分页查询")
    void testPageList() {
        Page<StockSplit> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testSplit));
        when(splitMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockSplit> result = splitService.pageList(null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("创建拆分单")
    void testCreateSplit() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(splitMapper.insert(any(StockSplit.class))).thenAnswer(invocation -> {
                StockSplit s = invocation.getArgument(0);
                s.setId(2L);
                return 1;
            });

            StockSplit split = new StockSplit();
            split.setWarehouseId(10L);
            split.setBomId(1L);
            split.setSplitQuantity(new BigDecimal("3"));

            StockSplitItem item = new StockSplitItem();
            item.setProductId(2001L);
            item.setProductCode("M001");
            item.setProductName("子件A");
            item.setQuantity(new BigDecimal("6"));
            item.setUnitCost(new BigDecimal("15"));

            StockSplit result = splitService.createSplit(split, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertTrue(result.getSplitNo().startsWith("SP"));
            verify(splitMapper).insert(any(StockSplit.class));
            verify(splitItemMapper).insert(any(StockSplitItem.class));
        }
    }

    @Test
    @DisplayName("提交审批")
    void testSubmit() {
        when(splitMapper.selectById(1L)).thenReturn(testSplit);
        StockSplit result = splitService.submitForApproval(1L);
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("审批通过")
    void testApprove() {
        testSplit.setStatus(1);
        when(splitMapper.selectById(1L)).thenReturn(testSplit);
        StockSplit result = splitService.approve(1L, 200L, null);
        assertEquals(2, result.getStatus());
    }

    @Test
    @DisplayName("审批拒绝")
    void testReject() {
        testSplit.setStatus(1);
        when(splitMapper.selectById(1L)).thenReturn(testSplit);
        StockSplit result = splitService.reject(1L, "拆分方案不合理");
        assertEquals(4, result.getStatus());
    }

    @Test
    @DisplayName("执行拆分")
    void testExecute() {
        testSplit.setStatus(2);
        when(splitMapper.selectById(1L)).thenReturn(testSplit);
        StockSplit result = splitService.execute(1L);
        assertEquals(3, result.getStatus());
    }

    @Test
    @DisplayName("执行拆分 - 未审批异常")
    void testExecuteNotApproved() {
        when(splitMapper.selectById(1L)).thenReturn(testSplit); // status 0
        assertThrows(BusinessException.class, () -> splitService.execute(1L));
    }

    @Test
    @DisplayName("取消拆分单")
    void testCancel() {
        when(splitMapper.selectById(1L)).thenReturn(testSplit); // status 0
        StockSplit result = splitService.cancel(1L, "取消拆分");
        assertEquals(5, result.getStatus());
    }

    @Test
    @DisplayName("取消 - 已执行异常")
    void testCancelExecuted() {
        testSplit.setStatus(3);
        when(splitMapper.selectById(1L)).thenReturn(testSplit);
        assertThrows(BusinessException.class, () -> splitService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("取消 - 单据不存在")
    void testCancelNotFound() {
        when(splitMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> splitService.cancel(999L, "test"));
    }

    @Test
    @DisplayName("获取明细")
    void testGetItems() {
        List<StockSplitItem> items = new ArrayList<>();
        StockSplitItem item = new StockSplitItem();
        item.setId(1L);
        item.setSplitId(1L);
        item.setProductName("子件A");
        items.add(item);

        when(splitItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);

        List<StockSplitItem> result = splitService.getItemList(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("状态异常 - 重复提交")
    void testDoubleSubmit() {
        testSplit.setStatus(1);
        when(splitMapper.selectById(1L)).thenReturn(testSplit);
        assertThrows(BusinessException.class, () -> splitService.submitForApproval(1L));
    }

    @Test
    @DisplayName("状态异常 - 草稿审批")
    void testApproveDraft() {
        when(splitMapper.selectById(1L)).thenReturn(testSplit); // status 0
        assertThrows(BusinessException.class, () -> splitService.approve(1L, 200L, null));
    }

    @Test
    @DisplayName("实体属性验证")
    void testEntityProperties() {
        assertEquals(1L, testSplit.getId());
        assertEquals("SP20260610TEST01", testSplit.getSplitNo());
        assertEquals(10L, testSplit.getWarehouseId());
        assertEquals(new BigDecimal("5"), testSplit.getSplitQuantity());
        assertEquals(0, testSplit.getStatus());
    }
}
