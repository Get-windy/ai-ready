package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockBom;
import cn.aiedge.erp.stock.entity.StockBomItem;
import cn.aiedge.erp.stock.mapper.StockBomItemMapper;
import cn.aiedge.erp.stock.mapper.StockBomMapper;
import cn.aiedge.erp.stock.service.impl.StockBomServiceImpl;
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
@DisplayName("BOM物料清单服务测试")
class StockBomServiceImplTest {

    @Mock
    private StockBomMapper bomMapper;

    @Mock
    private StockBomItemMapper bomItemMapper;

    @InjectMocks
    private StockBomServiceImpl bomService;

    private StockBom testBom;
    private List<StockBomItem> testItems;

    @BeforeEach
    void setUp() {
        testBom = new StockBom();
        testBom.setId(1L);
        testBom.setBomNo("BOM20260610001");
        testBom.setBomName("测试BOM");
        testBom.setProductId(1001L);
        testBom.setProductCode("P001");
        testBom.setProductName("成品A");
        testBom.setBomType(1);
        testBom.setOutputQuantity(new BigDecimal("1"));
        testBom.setStatus(1);
        testBom.setCreateTime(LocalDateTime.now());

        testItems = new ArrayList<>();
        StockBomItem item = new StockBomItem();
        item.setId(1L);
        item.setBomId(1L);
        item.setProductId(2001L);
        item.setProductCode("M001");
        item.setProductName("原材料A");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitCost(new BigDecimal("10.00"));
        testItems.add(item);
    }

    @Test
    @DisplayName("分页查询BOM")
    void testPageList() {
        Page<StockBom> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testBom));
        when(bomMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<StockBom> result = bomService.pageList(null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("创建BOM")
    void testCreateBom() {
        when(bomMapper.insert(any(StockBom.class))).thenAnswer(invocation -> {
            StockBom b = invocation.getArgument(0);
            b.setId(2L);
            return 1;
        });

        StockBom bom = new StockBom();
        bom.setBomName("新产品BOM");
        bom.setProductId(1002L);
        bom.setProductCode("P002");
        bom.setProductName("成品B");
        bom.setBomType(1);
        bom.setOutputQuantity(new BigDecimal("1"));

        StockBomItem item = new StockBomItem();
        item.setProductId(2002L);
        item.setProductCode("M002");
        item.setProductName("原材料B");
        item.setQuantity(new BigDecimal("3"));
        item.setUnitCost(new BigDecimal("5.00"));

        StockBom result = bomService.createBom(bom, List.of(item));

        assertNotNull(result);
        assertEquals(1, result.getStatus()); // enabled by default
        assertTrue(result.getBomNo().startsWith("BOM"));
        verify(bomMapper).insert(any(StockBom.class));
        verify(bomItemMapper).insert(any(StockBomItem.class));
    }

    @Test
    @DisplayName("更新BOM")
    void testUpdateBom() {
        when(bomMapper.selectById(1L)).thenReturn(testBom);

        StockBom update = new StockBom();
        update.setBomName("更新后的BOM");
        update.setRemark("备注更新");

        StockBomItem newItem = new StockBomItem();
        newItem.setProductId(2003L);
        newItem.setProductCode("M003");
        newItem.setProductName("新原材料");
        newItem.setQuantity(new BigDecimal("1"));
        newItem.setUnitCost(new BigDecimal("20.00"));

        StockBom result = bomService.updateBom(1L, update, List.of(newItem));

        assertNotNull(result);
        verify(bomMapper).selectById(1L);
        verify(bomItemMapper).delete(any(LambdaQueryWrapper.class));
        verify(bomItemMapper).insert(any(StockBomItem.class));
    }

    @Test
    @DisplayName("更新BOM - 不存在异常")
    void testUpdateBomNotFound() {
        when(bomMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> bomService.updateBom(999L, new StockBom(), null));
    }

    @Test
    @DisplayName("启用BOM")
    void testEnableBom() {
        testBom.setStatus(0);
        when(bomMapper.selectById(1L)).thenReturn(testBom);

        StockBom result = bomService.enableBom(1L);
        assertEquals(1, result.getStatus());
        verify(bomMapper).updateById(argThat(b -> ((StockBom) b).getStatus() == 1));
    }

    @Test
    @DisplayName("停用BOM")
    void testDisableBom() {
        when(bomMapper.selectById(1L)).thenReturn(testBom);

        StockBom result = bomService.disableBom(1L);
        assertEquals(0, result.getStatus());
        verify(bomMapper).updateById(argThat(b -> ((StockBom) b).getStatus() == 0));
    }

    @Test
    @DisplayName("停用BOM - 已停用重复操作")
    void testDisableAlreadyDisabled() {
        testBom.setStatus(0);
        when(bomMapper.selectById(1L)).thenReturn(testBom);

        StockBom result = bomService.disableBom(1L);
        assertEquals(0, result.getStatus());
    }

    @Test
    @DisplayName("获取BOM明细")
    void testGetItems() {
        when(bomItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);

        List<StockBomItem> items = bomService.getItems(1L);
        assertEquals(1, items.size());
        assertEquals("原材料A", items.get(0).getProductName());
    }

    @Test
    @DisplayName("BOM属性验证")
    void testBomProperties() {
        assertEquals("测试BOM", testBom.getBomName());
        assertEquals(1001L, testBom.getProductId());
        assertEquals("P001", testBom.getProductCode());
        assertEquals("成品A", testBom.getProductName());
        assertEquals(1, testBom.getBomType());
        assertEquals(new BigDecimal("1"), testBom.getOutputQuantity());
    }

    @Test
    @DisplayName("BOM明细金额计算")
    void testItemCostCalculation() {
        StockBomItem item = testItems.get(0);
        BigDecimal totalCost = item.getQuantity().multiply(item.getUnitCost());
        assertEquals(new BigDecimal("20.00"), totalCost);
    }
}
