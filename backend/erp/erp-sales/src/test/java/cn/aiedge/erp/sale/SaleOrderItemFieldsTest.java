/**
 * 销售订单明细字段功能测试
 * 验证重构后的字段映射和持久化（基于生产级架构）
 * 使用纯 Mockito 单元测试，不加载 Spring 上下文
 */
package cn.aiedge.erp.sale;

import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleOrderItemFieldsTest {

    @Mock
    private SaleOrderItemMapper saleOrderItemMapper;

    @Test
    @DisplayName("测试销售订单明细核心字段的CRUD操作")
    void testCoreFieldsCRUD() {
        // 创建测试数据 - 只包含快照字段，符合生产级架构
        SaleOrderItem item = new SaleOrderItem();
        item.setId(1L);
        item.setOrderId(1L);
        item.setLineNo(1);
        item.setProductId(1L);
        item.setProductCode("P001");
        item.setProductName("测试商品");
        item.setPreOrderNo("PRE20260622001");
        item.setSpecification("规格型号");
        item.setModel("Model-ABC");
        item.setLocation("A01");
        item.setRemark("测试备注");

        // 批次保质期快照字段
        item.setBatchCode("BATCH001");
        item.setProductionDate(LocalDate.now());
        item.setExpiryDate(LocalDate.now().plusDays(365));

        // 数量快照字段
        item.setQuantity(BigDecimal.valueOf(10));

        // 价格快照字段
        item.setUnitPrice(BigDecimal.valueOf(10.00));
        item.setAmount(BigDecimal.valueOf(100.00));
        item.setCostPrice(BigDecimal.valueOf(5.00));
        item.setOriginalPrice(BigDecimal.valueOf(12.00));
        item.setTaxRate(BigDecimal.valueOf(13.00));
        item.setUnitPriceWithTax(BigDecimal.valueOf(11.30));
        item.setAmountWithTax(BigDecimal.valueOf(113.00));
        item.setDiscountRate(BigDecimal.valueOf(10.00));
        item.setDiscountPercent(BigDecimal.valueOf(5.00));
        item.setDiscountAmount(BigDecimal.valueOf(1.00));
        item.setUsePreOrderAmount(BigDecimal.valueOf(100.50));
        item.setCalculatedPrice(BigDecimal.valueOf(9.50));

        // 客户价格等级快照字段
        item.setCustomerGradeCode("VIP");
        item.setCustomerGradeName("VIP客户");
        item.setPriceGradeCode("GRADE_A");
        item.setPriceSource("CUSTOMER_GRADE");
        item.setDiscountApplied("{\"rule\":\"VIP_DISCOUNT\",\"rate\":10}");

        // 营销快照字段
        item.setGift(true);
        item.setGiftItem("赠品A");
        item.setExchangePoints(BigDecimal.valueOf(100.00));
        item.setUsedPoints(BigDecimal.valueOf(50.00));

        // 包装快照字段
        item.setBigPack(BigDecimal.valueOf(1));
        item.setMidPack(BigDecimal.valueOf(10));
        item.setSmallPack(BigDecimal.valueOf(100));

        // 区域快照字段
        item.setArea("华南区");

        // 小单位快照字段
        item.setSmallUnit("个");
        item.setSmallUnitPrice(BigDecimal.valueOf(0.95));
        item.setSmallUnitQuantity(BigDecimal.valueOf(100));

        // 自定义字段
        item.setCustomField1(BigDecimal.valueOf(123.45));
        item.setCustomField2(BigDecimal.valueOf(678.90));
        item.setCustomField3(BigDecimal.valueOf(246.80));
        item.setCustomField4("自定义文本4");
        item.setCustomField5("自定义文本5");
        item.setCustomField6(BigDecimal.valueOf(111.11));
        item.setCustomField7(BigDecimal.valueOf(222.22));
        item.setCustomField8(1001L);
        item.setCustomField9(2001L);
        item.setCustomField10(3001L);

        // 设置模拟行为
        when(saleOrderItemMapper.insert(any(SaleOrderItem.class))).thenReturn(1);
        when(saleOrderItemMapper.selectById(1L)).thenReturn(item);
        when(saleOrderItemMapper.deleteById(any(Long.class))).thenReturn(1);

        // 保存测试数据
        int insertResult = saleOrderItemMapper.insert(item);
        assertThat(insertResult).isEqualTo(1);
        assertThat(item.getId()).isNotNull();

        // 查询刚保存的数据
        SaleOrderItem retrievedItem = saleOrderItemMapper.selectById(item.getId());
        assertThat(retrievedItem).isNotNull();

        // 验证核心字段值
        assertThat(retrievedItem.getProductCode()).isEqualTo("P001");
        assertThat(retrievedItem.getProductName()).isEqualTo("测试商品");
        assertThat(retrievedItem.getPreOrderNo()).isEqualTo("PRE20260622001");
        assertThat(retrievedItem.getSpecification()).isEqualTo("规格型号");
        assertThat(retrievedItem.getModel()).isEqualTo("Model-ABC");
        assertThat(retrievedItem.getLocation()).isEqualTo("A01");
        assertThat(retrievedItem.getRemark()).isEqualTo("测试备注");

        // 批次保质期快照字段
        assertThat(retrievedItem.getBatchCode()).isEqualTo("BATCH001");
        assertThat(retrievedItem.getProductionDate()).isEqualTo(LocalDate.now());
        assertThat(retrievedItem.getExpiryDate()).isEqualTo(LocalDate.now().plusDays(365));

        // 数量快照字段
        assertThat(retrievedItem.getQuantity()).isEqualTo(BigDecimal.valueOf(10));

        // 价格快照字段
        assertThat(retrievedItem.getUnitPrice()).isEqualTo(BigDecimal.valueOf(10.00));
        assertThat(retrievedItem.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(retrievedItem.getCostPrice()).isEqualTo(BigDecimal.valueOf(5.00));
        assertThat(retrievedItem.getOriginalPrice()).isEqualTo(BigDecimal.valueOf(12.00));
        assertThat(retrievedItem.getTaxRate()).isEqualTo(BigDecimal.valueOf(13.00));
        assertThat(retrievedItem.getUnitPriceWithTax()).isEqualTo(BigDecimal.valueOf(11.30));
        assertThat(retrievedItem.getAmountWithTax()).isEqualTo(BigDecimal.valueOf(113.00));
        assertThat(retrievedItem.getDiscountRate()).isEqualTo(BigDecimal.valueOf(10.00));
        assertThat(retrievedItem.getDiscountPercent()).isEqualTo(BigDecimal.valueOf(5.00));
        assertThat(retrievedItem.getDiscountAmount()).isEqualTo(BigDecimal.valueOf(1.00));
        assertThat(retrievedItem.getUsePreOrderAmount()).isEqualTo(BigDecimal.valueOf(100.50));
        assertThat(retrievedItem.getCalculatedPrice()).isEqualTo(BigDecimal.valueOf(9.50));

        // 客户价格等级快照字段
        assertThat(retrievedItem.getCustomerGradeCode()).isEqualTo("VIP");
        assertThat(retrievedItem.getCustomerGradeName()).isEqualTo("VIP客户");
        assertThat(retrievedItem.getPriceGradeCode()).isEqualTo("GRADE_A");
        assertThat(retrievedItem.getPriceSource()).isEqualTo("CUSTOMER_GRADE");
        assertThat(retrievedItem.getDiscountApplied()).isEqualTo("{\"rule\":\"VIP_DISCOUNT\",\"rate\":10}");

        // 营销快照字段
        assertThat(retrievedItem.getGift()).isEqualTo(true);
        assertThat(retrievedItem.getGiftItem()).isEqualTo("赠品A");
        assertThat(retrievedItem.getExchangePoints()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(retrievedItem.getUsedPoints()).isEqualTo(BigDecimal.valueOf(50.00));

        // 包装快照字段
        assertThat(retrievedItem.getBigPack()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(retrievedItem.getMidPack()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(retrievedItem.getSmallPack()).isEqualTo(BigDecimal.valueOf(100));

        // 区域快照字段
        assertThat(retrievedItem.getArea()).isEqualTo("华南区");

        // 小单位快照字段
        assertThat(retrievedItem.getSmallUnit()).isEqualTo("个");
        assertThat(retrievedItem.getSmallUnitPrice()).isEqualTo(BigDecimal.valueOf(0.95));
        assertThat(retrievedItem.getSmallUnitQuantity()).isEqualTo(BigDecimal.valueOf(100));

        // 自定义字段
        assertThat(retrievedItem.getCustomField1()).isEqualTo(BigDecimal.valueOf(123.45));
        assertThat(retrievedItem.getCustomField2()).isEqualTo(BigDecimal.valueOf(678.90));
        assertThat(retrievedItem.getCustomField3()).isEqualTo(BigDecimal.valueOf(246.80));
        assertThat(retrievedItem.getCustomField4()).isEqualTo("自定义文本4");
        assertThat(retrievedItem.getCustomField5()).isEqualTo("自定义文本5");
        assertThat(retrievedItem.getCustomField6()).isEqualTo(BigDecimal.valueOf(111.11));
        assertThat(retrievedItem.getCustomField7()).isEqualTo(BigDecimal.valueOf(222.22));
        assertThat(retrievedItem.getCustomField8()).isEqualTo(1001L);
        assertThat(retrievedItem.getCustomField9()).isEqualTo(2001L);
        assertThat(retrievedItem.getCustomField10()).isEqualTo(3001L);

        // 清理测试数据
        int deleteResult = saleOrderItemMapper.deleteById(item.getId());
        assertThat(deleteResult).isEqualTo(1);
    }
}
