/**
 * 销售退货申请单服务单元测试
 * 使用纯 Mockito 测试，不加载 Spring 上下文
 */
package cn.aiedge.erp.sale;

import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnItemMapper;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnMapper;
import cn.aiedge.erp.sale.salereturn.service.impl.SaleReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SaleReturnServiceTest {

    @Mock
    private SaleReturnMapper saleReturnMapper;

    @Mock
    private SaleReturnItemMapper saleReturnItemMapper;

    @Spy
    @InjectMocks
    private SaleReturnServiceImpl saleReturnService;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus ServiceImpl.baseMapper 通过字段注入，@InjectMocks 构造注入后为 null
        ReflectionTestUtils.setField(saleReturnService, "baseMapper", saleReturnMapper);
    }

    @Test
    @DisplayName("服务实例化 - 基本冒烟测试")
    void testServiceInstantiation() {
        assertNotNull(saleReturnService, "SaleReturnServiceImpl 应成功实例化");
    }

    @Test
    @DisplayName("退货单号生成 - 格式正确")
    void testGenerateReturnNo() {
        String returnNo = saleReturnService.generateReturnNo();

        assertNotNull(returnNo, "退货单号不应为空");
        assertTrue(returnNo.startsWith("XSTHSQD-"), "退货单号应以 XSTHSQD- 前缀生成，实际: " + returnNo);
        // 格式: XSTHSQD-yyyyMMdd-XXXXXX
        assertTrue(returnNo.length() >= 20, "退货单号长度应不小于20，实际: " + returnNo.length());
    }
}
