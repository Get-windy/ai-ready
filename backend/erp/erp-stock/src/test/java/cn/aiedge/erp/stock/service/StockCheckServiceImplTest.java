package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockCheck;
import cn.aiedge.erp.stock.entity.StockCheckItem;
import cn.aiedge.erp.stock.mapper.StockCheckItemMapper;
import cn.aiedge.erp.stock.mapper.StockCheckMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.impl.StockCheckServiceImpl;
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
@DisplayName("盘点单服务测试")
class StockCheckServiceImplTest {

    @Mock
    private StockCheckMapper checkMapper;

    @Mock
    private StockCheckItemMapper checkItemMapper;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockCheckServiceImpl checkService;

    private StockCheck testCheck;
    private StockCheckItem testItem;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        testCheck = new StockCheck();
        testCheck.setId(1L);
        testCheck.setTenantId(1L);
        testCheck.setCheckNo("SC20260610001");
        testCheck.setWarehouseId(10L);
        testCheck.setCheckType(1);
        testCheck.setStatus(0); // DRAFT
        testCheck.setTotalItems(2);
        testCheck.setCheckedItems(0);
        testCheck.setDiffItems(0);
        testCheck.setTotalBookQuantity(new BigDecimal("100"));
        testCheck.setTotalActualQuantity(BigDecimal.ZERO);
        testCheck.setTotalDiffQuantity(new BigDecimal("-100"));
        testCheck.setCreateTime(LocalDateTime.now());

        testItem = new StockCheckItem();
        testItem.setId(1L);
        testItem.setCheckId(1L);
        testItem.setProductId(1001L);
        testItem.setProductCode("P001");
        testItem.setProductName("测试产品");
        testItem.setBookQuantity(new BigDecimal("50"));
        testItem.setActualQuantity(BigDecimal.ZERO);
        testItem.setDiffQuantity(BigDecimal.ZERO);
        testItem.setStatus(0);

        testStock = new Stock();
        testStock.setId(1L);
        testStock.setProductId(1001L);
        testStock.setWarehouseId(10L);
        testStock.setQuantity(new BigDecimal("50"));
        testStock.setAvailableQuantity(new BigDecimal("50"));
    }

    @Test
    @DisplayName("分页查询")
    void testPageList() {
        Page<StockCheck> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testCheck));
        when(checkMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockCheck> result = checkService.pageList(null, null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("按盘点单号查询")
    void testGetByCheckNo() {
        when(checkMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testCheck));

        StockCheck result = checkService.getByCheckNo("SC20260610001");
        assertNotNull(result);
        assertEquals("SC20260610001", result.getCheckNo());
    }

    @Test
    @DisplayName("生成盘点单号")
    void testGenerateCheckNo() {
        String no = checkService.generateCheckNo();
        assertNotNull(no);
        assertTrue(no.startsWith("SC"));
        assertEquals(16, no.length()); // SC + yyyyMMdd(8) + random(6)
    }

    @Test
    @DisplayName("创建盘点单")
    void testCreateCheck() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(checkMapper.insert(any(StockCheck.class))).thenAnswer(invocation -> {
                StockCheck c = invocation.getArgument(0);
                c.setId(2L);
                return 1;
            });

            StockCheck check = new StockCheck();
            check.setWarehouseId(10L);
            check.setCheckType(1);

            StockCheck result = checkService.createCheck(check);

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertTrue(result.getCheckNo().startsWith("SC"));
            verify(checkMapper).insert(any(StockCheck.class));
        }
    }

    @Test
    @DisplayName("创建盘点单并自动生成明细")
    void testCreateCheckWithItems() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(checkMapper.insert(any(StockCheck.class))).thenAnswer(invocation -> {
                StockCheck c = invocation.getArgument(0);
                c.setId(3L);
                return 1;
            });
            when(stockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testStock));

            StockCheck result = checkService.createCheckWithItems(10L);

            assertNotNull(result);
            assertEquals(1, result.getTotalItems());
            verify(checkItemMapper).insert(any(StockCheckItem.class));
            verify(checkMapper).updateById(argThat((StockCheck c) ->
                    c.getTotalItems() == 1));
        }
    }

    @Test
    @DisplayName("更新盘点单")
    void testUpdateCheck() {
        when(checkMapper.selectById(1L)).thenReturn(testCheck);

        StockCheck update = new StockCheck();
        update.setWarehouseId(20L);
        update.setRemark("更新备注");

        StockCheck result = checkService.updateCheck(1L, update);

        assertNotNull(result);
        verify(checkMapper).updateById(argThat((StockCheck c) ->
                c.getWarehouseId() == 20L));
    }

    @Test
    @DisplayName("更新盘点单 - 非草稿状态异常")
    void testUpdateCheckNotDraft() {
        testCheck.setStatus(1);
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        assertThrows(BusinessException.class,
                () -> checkService.updateCheck(1L, new StockCheck()));
    }

    @Test
    @DisplayName("提交审批")
    void testSubmit() {
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        StockCheck result = checkService.submitForApproval(1L);
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("审批通过")
    void testApprove() {
        testCheck.setStatus(1);
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        StockCheck result = checkService.approve(1L, 200L, null);
        assertEquals(2, result.getStatus());
    }

    @Test
    @DisplayName("审批拒绝")
    void testReject() {
        testCheck.setStatus(1);
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        StockCheck result = checkService.reject(1L, "数据不符");
        assertEquals(7, result.getStatus());
    }

    @Test
    @DisplayName("开始盘点")
    void testStartCheck() {
        testCheck.setStatus(2); // APPROVED
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        StockCheck result = checkService.startCheck(1L);
        assertEquals(3, result.getStatus());
    }

    @Test
    @DisplayName("开始盘点 - 非已审批状态异常")
    void testStartCheckNotApproved() {
        when(checkMapper.selectById(1L)).thenReturn(testCheck); // DRAFT
        assertThrows(BusinessException.class, () -> checkService.startCheck(1L));
    }

    @Test
    @DisplayName("盘点单项 - 录入实盘数量")
    void testCheckItem() {
        testCheck.setStatus(3); // IN_PROGRESS
        when(checkItemMapper.selectById(1L)).thenReturn(testItem);
        when(checkMapper.selectById(1L)).thenReturn(testCheck);

        StockCheckItem result = checkService.checkItem(1L, new BigDecimal("55"), "实盘55");

        assertNotNull(result);
        assertEquals(new BigDecimal("55"), result.getActualQuantity());
        assertEquals(new BigDecimal("5"), result.getDiffQuantity()); // 55-50
        assertEquals(1, result.getStatus());
        verify(checkItemMapper).updateById(any(StockCheckItem.class));
    }

    @Test
    @DisplayName("完成盘点")
    void testCompleteCheck() {
        testCheck.setStatus(3); // IN_PROGRESS
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        StockCheck result = checkService.completeCheck(1L);
        assertEquals(4, result.getStatus()); // COMPLETED
    }

    @Test
    @DisplayName("调整库存")
    void testAdjustStock() {
        testItem.setDiffQuantity(new BigDecimal("5"));
        testCheck.setStatus(4); // COMPLETED
        testCheck.setWarehouseId(10L);
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testItem));
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);

        StockCheck result = checkService.adjust(1L);

        assertEquals(5, result.getStatus()); // ADJUSTED
        verify(stockMapper).updateById(argThat((Stock s) ->
                s.getQuantity().compareTo(new BigDecimal("55")) == 0)); // 50+5
        verify(checkItemMapper).updateById(argThat((StockCheckItem i) ->
                i.getStatus() == 2));
    }

    @Test
    @DisplayName("取消盘点单")
    void testCancel() {
        when(checkMapper.selectById(1L)).thenReturn(testCheck); // DRAFT
        StockCheck result = checkService.cancel(1L, "取消盘点");
        assertEquals(6, result.getStatus()); // CANCELLED
    }

    @Test
    @DisplayName("取消 - 已调整异常")
    void testCancelAdjusted() {
        testCheck.setStatus(5); // ADJUSTED
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        assertThrows(BusinessException.class, () -> checkService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("获取盘点明细")
    void testGetItems() {
        List<StockCheckItem> items = List.of(testItem);
        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);

        List<StockCheckItem> result = checkService.getItems(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取差异明细")
    void testGetDiffItems() {
        testItem.setDiffQuantity(new BigDecimal("5"));
        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testItem));

        List<StockCheckItem> result = checkService.getDiffItems(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取差异明细 - 无差异")
    void testGetDiffItemsEmpty() {
        testItem.setDiffQuantity(BigDecimal.ZERO);
        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StockCheckItem> result = checkService.getDiffItems(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("导出列表")
    void testExportList() {
        when(checkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testCheck));

        List<StockCheck> result = checkService.exportList(null, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按仓库查询")
    void testListByWarehouseId() {
        when(checkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testCheck));

        List<StockCheck> result = checkService.listByWarehouseId(10L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("计算盘点统计")
    void testCalculateTotals() {
        testItem.setActualQuantity(new BigDecimal("55"));
        testItem.setDiffQuantity(new BigDecimal("5"));
        testItem.setStatus(1);

        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testItem));
        when(checkMapper.selectById(1L)).thenReturn(testCheck);

        checkService.calculateTotals(1L);

        verify(checkMapper).updateById(argThat((StockCheck s) ->
            s.getTotalItems() == 1
                    && s.getCheckedItems() == 1
                    && s.getDiffItems() == 1
        ));
    }

    @Test
    @DisplayName("获取差异项数量")
    void testGetDiffItemCount() {
        when(checkItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        Integer count = checkService.getDiffItemCount(1L);
        assertEquals(3, count);
    }

    @Test
    @DisplayName("完整状态流程: 草稿→提交→审批→开始→盘点→完成→调整")
    void testFullWorkflow() {
        // DRAFT -> submit -> PENDING_APPROVAL
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        StockCheck submitted = checkService.submitForApproval(1L);
        assertEquals(1, submitted.getStatus());
        testCheck.setStatus(1);

        // PENDING_APPROVAL -> approve -> APPROVED
        StockCheck approved = checkService.approve(1L, 200L, null);
        assertEquals(2, approved.getStatus());
        testCheck.setStatus(2);

        // APPROVED -> start -> IN_PROGRESS
        StockCheck started = checkService.startCheck(1L);
        assertEquals(3, started.getStatus());
        testCheck.setStatus(3);

        // IN_PROGRESS -> check items -> complete -> COMPLETED
        when(checkItemMapper.selectById(1L)).thenReturn(testItem);
        testCheck.setWarehouseId(10L);
        StockCheckItem checked = checkService.checkItem(1L, new BigDecimal("55"), null);
        assertEquals(new BigDecimal("5"), checked.getDiffQuantity());

        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testItem));
        when(checkMapper.selectById(1L)).thenReturn(testCheck);
        StockCheck completed = checkService.completeCheck(1L);
        assertEquals(4, completed.getStatus());
        testCheck.setStatus(4);

        // COMPLETED -> adjust -> ADJUSTED
        testItem.setDiffQuantity(new BigDecimal("5"));
        when(checkItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testItem));
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        StockCheck adjusted = checkService.adjust(1L);
        assertEquals(5, adjusted.getStatus());
    }
}
