package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockTransfer;
import cn.aiedge.erp.stock.entity.StockTransferItem;
import cn.aiedge.erp.stock.mapper.StockTransferItemMapper;
import cn.aiedge.erp.stock.mapper.StockTransferMapper;
import cn.aiedge.erp.stock.service.impl.StockTransferServiceImpl;
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
@DisplayName("调拨单服务测试")
class StockTransferServiceImplTest {

    @Mock
    private StockTransferMapper transferMapper;

    @Mock
    private StockTransferItemMapper transferItemMapper;

    @InjectMocks
    private StockTransferServiceImpl transferService;

    private StockTransfer testTransfer;
    private List<StockTransferItem> testItems;

    @BeforeEach
    void setUp() {
        testTransfer = new StockTransfer();
        testTransfer.setId(1L);
        testTransfer.setTransferNo("TR20260610TEST01");
        testTransfer.setFromWarehouseId(10L);
        testTransfer.setToWarehouseId(20L);
        testTransfer.setStatus(0);
        testTransfer.setTotalQuantity(new BigDecimal("100"));
        testTransfer.setTotalAmount(new BigDecimal("5000"));
        testTransfer.setApplicantId(100L);
        testTransfer.setCreateTime(LocalDateTime.now());

        testItems = new ArrayList<>();
        StockTransferItem item = new StockTransferItem();
        item.setId(1L);
        item.setTransferId(1L);
        item.setProductId(1001L);
        item.setProductCode("P001");
        item.setProductName("调拨产品");
        item.setQuantity(new BigDecimal("100"));
        item.setUnitCost(new BigDecimal("50"));
        item.setLineAmount(new BigDecimal("5000"));
        testItems.add(item);
    }

    @Test
    @DisplayName("分页查询")
    void testPageList() {
        Page<StockTransfer> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testTransfer));
        when(transferMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockTransfer> result = transferService.pageList(null, null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("创建调拨单")
    void testCreateTransfer() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(transferMapper.insert(any(StockTransfer.class))).thenAnswer(invocation -> {
                StockTransfer t = invocation.getArgument(0);
                t.setId(2L);
                return 1;
            });

            StockTransfer transfer = new StockTransfer();
            transfer.setFromWarehouseId(10L);
            transfer.setToWarehouseId(20L);
            transfer.setRemark("常规调拨");

            StockTransferItem item = new StockTransferItem();
            item.setProductId(1001L);
            item.setProductCode("P001");
            item.setProductName("产品A");
            item.setQuantity(new BigDecimal("50"));
            item.setUnitCost(new BigDecimal("100"));

            StockTransfer result = transferService.createTransfer(transfer, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertTrue(result.getTransferNo().startsWith("TR"));
            verify(transferMapper).insert(any(StockTransfer.class));
            verify(transferItemMapper).insert(any(StockTransferItem.class));
        }
    }

    @Test
    @DisplayName("创建调拨单 - 无明细时使用空列表")
    void testCreateTransferEmptyItems() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(transferMapper.insert(any(StockTransfer.class))).thenAnswer(invocation -> {
                StockTransfer t = invocation.getArgument(0);
                t.setId(3L);
                return 1;
            });

            StockTransfer transfer = new StockTransfer();
            transfer.setFromWarehouseId(10L);
            transfer.setToWarehouseId(20L);

            StockTransfer result = transferService.createTransfer(transfer, null);

            assertNotNull(result);
            assertEquals(0, result.getTotalItems());
            verify(transferItemMapper, never()).insert(any(StockTransferItem.class));
        }
    }

    @Test
    @DisplayName("提交审批")
    void testSubmit() {
        when(transferMapper.selectById(1L)).thenReturn(testTransfer);
        StockTransfer result = transferService.submitForApproval(1L);
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("审批通过")
    void testApprove() {
        testTransfer.setStatus(1);
        when(transferMapper.selectById(1L)).thenReturn(testTransfer);
        StockTransfer result = transferService.approve(1L, 200L, null);
        assertEquals(2, result.getStatus());
    }

    @Test
    @DisplayName("审批拒绝")
    void testReject() {
        testTransfer.setStatus(1);
        when(transferMapper.selectById(1L)).thenReturn(testTransfer);
        StockTransfer result = transferService.reject(1L, "库存不足");
        assertEquals(4, result.getStatus());
    }

    @Test
    @DisplayName("执行调拨")
    void testExecute() {
        testTransfer.setStatus(2);
        when(transferMapper.selectById(1L)).thenReturn(testTransfer);
        StockTransfer result = transferService.execute(1L);
        assertEquals(3, result.getStatus());
    }

    @Test
    @DisplayName("取消调拨单")
    void testCancel() {
        when(transferMapper.selectById(1L)).thenReturn(testTransfer); // status 0
        StockTransfer result = transferService.cancel(1L, "不再调拨");
        assertEquals(5, result.getStatus());
    }

    @Test
    @DisplayName("取消 - 已执行异常")
    void testCancelExecuted() {
        testTransfer.setStatus(3);
        when(transferMapper.selectById(1L)).thenReturn(testTransfer);
        assertThrows(BusinessException.class, () -> transferService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("获取明细")
    void testGetItems() {
        when(transferItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);
        List<StockTransferItem> items = transferService.getItems(1L);
        assertEquals(1, items.size());
    }

    @Test
    @DisplayName("状态异常 - 重复提交")
    void testDoubleSubmit() {
        testTransfer.setStatus(1);
        when(transferMapper.selectById(1L)).thenReturn(testTransfer);
        assertThrows(BusinessException.class, () -> transferService.submitForApproval(1L));
    }

    @Test
    @DisplayName("状态异常 - 草稿执行")
    void testExecuteDraft() {
        when(transferMapper.selectById(1L)).thenReturn(testTransfer); // status 0
        assertThrows(BusinessException.class, () -> transferService.execute(1L));
    }

    @Test
    @DisplayName("单据不存在")
    void testNotFound() {
        when(transferMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> transferService.submitForApproval(999L));
    }
}
