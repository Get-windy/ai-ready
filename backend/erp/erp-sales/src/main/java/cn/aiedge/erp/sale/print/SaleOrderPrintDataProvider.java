package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderItem;
import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 销售订单打印数据装配器（pageCode = sale）。
 *
 * 把 SaleOrder + SaleOrderItem 组装成标准打印数据包，字段名**就是实体的属性名**，
 * 这样模板里的 {@code field} 能一眼对回实体/数据库列，排查时不用再翻译一层。
 *
 * 明细行的金额字段用实体原名 {@code amount}（不是 ql361 那套 {@code totalmoney}）。
 */
@Component
@RequiredArgsConstructor
public class SaleOrderPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="sale">}、sys_print_template.page_code 三处一致 */
    public static final String PAGE_CODE = "sale";

    private final SaleOrderMapper orderMapper;
    private final SaleOrderItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        SaleOrder order = orderMapper.selectById(documentId);
        if (order == null) {
            return null;
        }
        List<SaleOrderItem> items = itemMapper.selectByOrderId(documentId);
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("orderNo", order.getOrderNo());
        data.put("orderDate", order.getOrderDate());
        data.put("status", order.getStatus());
        data.put("saleType", order.getSaleType());
        data.put("customerCode", order.getCustomerCode());
        data.put("customerName", order.getCustomerName());
        data.put("customerLevel", order.getCustomerLevel());
        data.put("customerRemark", order.getCustomerRemark());
        data.put("warehouseName", order.getWarehouseName());
        data.put("salesmanName", order.getSalesmanName());
        data.put("deptName", order.getDeptName());
        data.put("promoterName", order.getPromoterName());
        // 收货
        data.put("receiverName", order.getReceiverName());
        data.put("receiverPhone", order.getReceiverPhone());
        data.put("contactName", order.getContactName());
        data.put("contactPhone", order.getContactPhone());
        data.put("shippingAddress", order.getShippingAddress());
        data.put("pickupAddress", order.getPickupAddress());
        // 配送
        data.put("deliveryMethod", order.getDeliveryMethod());
        data.put("deliveryRoute", order.getDeliveryRoute());
        data.put("settlementMethod", order.getSettlementMethod());
        data.put("logisticsCompany", order.getLogisticsCompany());
        data.put("waybillNo", order.getWaybillNo());
        data.put("driverName", order.getDriverName());
        data.put("deliveryVehicle", order.getDeliveryVehicle());
        data.put("freightPayer", order.getFreightPayer());
        data.put("shippingFee", order.getShippingFee());
        data.put("codAmount", order.getCodAmount());
        // 金额与结算
        data.put("productAmount", order.getProductAmount());
        data.put("discountAmount", order.getDiscountAmount());
        data.put("billAmount", order.getBillAmount());
        data.put("settledAmount", order.getSettledAmount());
        data.put("receivedAmount", order.getReceivedAmount());
        data.put("promoDiscount", order.getPromoDiscount());
        data.put("couponAmount", order.getCouponAmount());
        // 开票与备注
        data.put("bankName", order.getBankName());
        data.put("bankAccount", order.getBankAccount());
        data.put("taxNo", order.getTaxNo());
        data.put("remark", order.getRemark());
        data.put("buyerRemark", order.getBuyerRemark());
        data.put("orderRemark", order.getOrderRemark());
        data.put("expectedShipTime", order.getExpectedShipTime());

        // ── 明细 ──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        for (SaleOrderItem item : items) {
            rows.add(itemRow(item));
        }
        data.put("items", rows);

        // ── 跨行汇总（实体已存了就用实体的，它是权威值；为空才按明细算）──
        data.put("totalQuantity", order.getTotalQuantity() != null
                ? order.getTotalQuantity() : sum(items, SaleOrderItem::getQuantity));
        data.put("totalAmount", sum(items, SaleOrderItem::getAmount));
        data.put("totalLineDiscount", sum(items, SaleOrderItem::getDiscountAmount));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        SaleOrder order = orderMapper.selectById(documentId);
        return order != null ? order.getOrderNo() : null;
    }

    private Map<String, Object> itemRow(SaleOrderItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineNo", item.getLineNo());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("specification", item.getSpecification());
        row.put("model", item.getModel());
        row.put("brand", item.getBrand());
        row.put("origin", item.getOrigin());
        row.put("unit", item.getUnit());
        row.put("quantity", item.getQuantity());
        row.put("unitPrice", item.getUnitPrice());
        row.put("amount", item.getAmount());
        row.put("discountPercent", item.getDiscountPercent());
        row.put("discountAmount", item.getDiscountAmount());
        row.put("taxRate", item.getTaxRate());
        row.put("taxAmount", item.getTaxAmount());
        row.put("amountWithTax", item.getAmountWithTax());
        row.put("batchCode", item.getBatchCode());
        row.put("shelfLife", item.getShelfLife());
        row.put("shippedQuantity", item.getShippedQuantity());
        row.put("remark", item.getRemark());
        return row;
    }

    private BigDecimal sum(List<SaleOrderItem> items,
                           java.util.function.Function<SaleOrderItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleOrderItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
