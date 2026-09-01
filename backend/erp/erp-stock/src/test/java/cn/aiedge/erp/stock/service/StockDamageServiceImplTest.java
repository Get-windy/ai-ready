package cn.aiedge.erp.stock.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockDamageQuery;
import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import cn.aiedge.erp.stock.mapper.StockDamageItemMapper;
import cn.aiedge.erp.stock.mapper.StockDamageMapper;
import cn.aiedge.erp.stock.service.impl.StockDamageServiceImpl;
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
@DisplayName("报损单服务测试")
class StockDamageServiceImplTest {

    @Mock
    private StockDamageMapper damageMapper;

    @Mock
    private StockDamageItemMapper damageItemMapper;

    @InjectMocks
    private StockDamageServiceImpl damageService;

    private StockDamage testDamage;
    private List<StockDamageItem> testItems;

    @BeforeEach
    void setUp() {
        testDamage = new StockDamage();
        testDamage.setId(1L);
        testDamage.setTenantId(1L);
        testDamage.setDamageNo("DM20260610TEST01");
        testDamage.setWarehouseId(10L);
        testDamage.setDamageDate(java.time.LocalDate.now());
        testDamage.setDamageCause(2);
        testDamage.setStatus(0);
        testDamage.setTotalQuantity(new BigDecimal("20"));
        testDamage.setTotalAmount(new BigDecimal("2000"));
        testDamage.setTotalItems(1);
        testDamage.setCreateTime(LocalDateTime.now());

        testItems = new ArrayList<>();
        StockDamageItem item = new StockDamageItem();
        item.setId(1L);
        item.setDamageId(1L);
        item.setProductId(2001L);
        item.setProductCode("P003");
        item.setProductName("损坏产品");
        item.setQuantity(new BigDecimal("20"));
        item.setUnitCost(new BigDecimal("100"));
        item.setAmount(new BigDecimal("2000"));
        testItems.add(item);
    }

    @Test
    @DisplayName("分页查询")
    void testPageList() {
        Page<StockDamage> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(List.of(testDamage));
        when(damageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        StockDamageQuery query = new StockDamageQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        Page<StockDamage> result = damageService.pageList(query);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("创建报损单 - 含明细")
    void testCreateDamageWithItems() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(damageMapper.insert(any(StockDamage.class))).thenAnswer(invocation -> {
                StockDamage d = invocation.getArgument(0);
                d.setId(2L);
                return 1;
            });

            StockDamage damage = new StockDamage();
            damage.setWarehouseId(10L);
            damage.setDamageDate(java.time.LocalDate.now());
            damage.setDamageCause(1);
            damage.setRemark("自然损耗");

            StockDamageItem item = new StockDamageItem();
            item.setProductId(2001L);
            item.setProductCode("P003");
            item.setProductName("损坏产品");
            item.setQuantity(new BigDecimal("5"));
            item.setUnitCost(new BigDecimal("100"));

            StockDamage result = damageService.createStockDamage(damage, List.of(item));

            assertNotNull(result);
            assertEquals(0, result.getStatus());
            assertTrue(result.getDamageNo().startsWith("BSD-"));
            verify(damageMapper).insert(any(StockDamage.class));
            verify(damageItemMapper).insert(any(StockDamageItem.class));
        }
    }

    @Test
    @DisplayName("记帐（草稿→已记账）")
    void testExecute() {
        when(damageMapper.selectById(1L)).thenReturn(testDamage); // status 0
        StockDamage result = damageService.execute(1L);
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("作废报损单（草稿→已作废）")
    void testCancel() {
        when(damageMapper.selectById(1L)).thenReturn(testDamage); // status 0
        StockDamage result = damageService.cancel(1L, "不再报损");
        assertEquals(2, result.getStatus());
    }

    @Test
    @DisplayName("作废 - 已记账抛出异常")
    void testCancelBooked() {
        testDamage.setStatus(1);
        when(damageMapper.selectById(1L)).thenReturn(testDamage);
        assertThrows(BusinessException.class, () -> damageService.cancel(1L, "test"));
    }

    @Test
    @DisplayName("获取明细")
    void testGetItems() {
        when(damageItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testItems);
        List<StockDamageItem> items = damageService.getItems(1L);
        assertEquals(1, items.size());
    }
}
