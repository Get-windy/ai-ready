package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockAssemble;
import cn.aiedge.erp.stock.entity.StockAssembleItem;
import cn.aiedge.erp.stock.mapper.StockAssembleItemMapper;
import cn.aiedge.erp.stock.mapper.StockAssembleMapper;
import cn.aiedge.erp.stock.service.impl.StockAssembleServiceImpl;
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
@DisplayName("组装单服务测试")
class StockAssembleServiceImplTest {

    @Mock
    private StockAssembleMapper assembleMapper;

    @Mock
    private StockAssembleItemMapper assembleItemMapper;

    @InjectMocks
    private StockAssembleServiceImpl assembleService;

    private StockAssemble testAssemble;

    @BeforeEach
    void setUp() {
        testAssemble = new StockAssemble();
        testAssemble.setId(1L);
        testAssemble.setAssembleNo("AS20260610TEST01");
        testAssemble.setWarehouseId(10L);
        testAssemble.setBomId(1L);
        testAssemble.setProductCode("P001");
        testAssemble.setProductName("成品A");
        testAssemble.setAssembleQuantity(new BigDecimal("10"));
        testAssemble.setAssembleFee(new BigDecimal("100"));
        testAssemble.setTotalCost(new BigDecimal("5000"));
        testAssemble.setStatus(0);
        testAssemble.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("分页查询")
    void testPageList() {
        Page<StockAssemble> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testAssemble));
        when(assembleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockAssemble> result = assembleService.pageList(null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("创建组装单")
    void testCreateAssemble() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(assembleMapper.insert(any(StockAssemble.class))).thenAnswer(invocation -> {
                StockAssemble a = invocation.getArgument(0);
                a.setId(2L);
                return 1;
            });

            StockAssemble assemble = new StockAssemble();
            assemble.setWarehouseId(10L);
            assemble.setBomId(1L);
            assemble.setAssembleQuantity(new BigDecimal("5"));
            assemble.setAssembleFee(new BigDecimal("50"));

            StockAssembleItem item = new StockAssembleItem();
            item.setProductId(2001L);
            item.setProductCode("M001");
            item.setProductName("原材料A");
            item.setQuantity(new BigDecimal("10"));
            item.setUnitCost(new BigDecimal("20"));

            StockAssemble result = assembleService.createAssemble(assemble, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertTrue(result.getAssembleNo().startsWith("AS"));
            verify(assembleMapper).insert(any(StockAssemble.class));
            verify(assembleItemMapper).insert(any(StockAssembleItem.class));
        }
    }

    @Test
    @DisplayName("创建组装单 - 计算总成本")
    void testCreateAssembleCalculateCost() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(assembleMapper.insert(any(StockAssemble.class))).thenAnswer(invocation -> {
                StockAssemble a = invocation.getArgument(0);
                a.setId(3L);
                return 1;
            });

            StockAssemble assemble = new StockAssemble();
            assemble.setWarehouseId(10L);
            assemble.setBomId(1L);
            assemble.setAssembleQuantity(new BigDecimal("2"));
            assemble.setAssembleFee(new BigDecimal("100"));

            StockAssembleItem item = new StockAssembleItem();
            item.setProductId(2001L);
            item.setProductCode("M001");
            item.setProductName("原材料A");
            item.setQuantity(new BigDecimal("5"));
            item.setUnitCost(new BigDecimal("20"));

            StockAssemble result = assembleService.createAssemble(assemble, List.of(item));

            // totalCost = qty * unitCost * assembleQuantity + assembleFee
            // = 5 * 20 * 2 + 100 = 300
            assertNotNull(result);
            verify(assembleItemMapper).insert(argThat((StockAssembleItem i) ->
                    i.getCost().compareTo(new BigDecimal("100")) == 0));
        }
    }

    @Test
    @DisplayName("提交审批")
    void testSubmit() {
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        StockAssemble result = assembleService.submitForApproval(1L);
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("审批通过")
    void testApprove() {
        testAssemble.setStatus(1);
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        StockAssemble result = assembleService.approve(1L, 200L, null);
        assertEquals(2, result.getStatus());
    }

    @Test
    @DisplayName("审批拒绝")
    void testReject() {
        testAssemble.setStatus(1);
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        StockAssemble result = assembleService.reject(1L, "BOM配置有误");
        assertEquals(4, result.getStatus());
    }

    @Test
    @DisplayName("执行组装")
    void testExecute() {
        testAssemble.setStatus(2);
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        StockAssemble result = assembleService.execute(1L);
        assertEquals(3, result.getStatus());
    }

    @Test
    @DisplayName("执行组装 - 未审批异常")
    void testExecuteNotApproved() {
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble); // status 0
        assertThrows(BusinessException.class, () -> assembleService.execute(1L));
    }

    @Test
    @DisplayName("取消组装单")
    void testCancel() {
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble); // status 0
        StockAssemble result = assembleService.cancel(1L, "取消组装");
        assertEquals(5, result.getStatus());
    }

    @Test
    @DisplayName("取消 - 已执行异常")
    void testCancelExecuted() {
        testAssemble.setStatus(3);
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        assertThrows(BusinessException.class, () -> assembleService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("获取明细")
    void testGetItems() {
        List<StockAssembleItem> items = new ArrayList<>();
        StockAssembleItem item = new StockAssembleItem();
        item.setId(1L);
        item.setAssembleId(1L);
        item.setProductName("原材料A");
        items.add(item);

        when(assembleItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);

        List<StockAssembleItem> result = assembleService.getItemList(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("状态异常 - 已审批再提交")
    void testSubmitApproved() {
        testAssemble.setStatus(2);
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        assertThrows(BusinessException.class, () -> assembleService.submitForApproval(1L));
    }

    @Test
    @DisplayName("状态异常 - 已执行再审批")
    void testApproveExecuted() {
        testAssemble.setStatus(3);
        when(assembleMapper.selectById(1L)).thenReturn(testAssemble);
        assertThrows(BusinessException.class, () -> assembleService.approve(1L, 200L, null));
    }
}
