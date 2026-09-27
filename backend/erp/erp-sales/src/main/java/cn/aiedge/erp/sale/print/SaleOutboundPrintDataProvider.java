package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundItemMapper;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 销售出库单打印数据装配器（pageCode = sale-outbound）。
 *
 * <p>单据头上的客户/仓库/经手人/部门都是**快照列**（实体上就有名称），不需要回表取名。</p>
 *
 * <p>⚠️ 明细的「出库数量」要用 {@code outboundQuantity}（落库列 {@code outbound_quantity}），
 * <b>不是</b> {@code quantity} —— 后者在 {@link SaleOutboundItem} 上是
 * {@code @TableField(exist = false)} 的临时别名（表里根本没有这一列），
 * 直接查库时它是 null，打出来就是空数量。</p>
 * <p>明细金额字段同样是实体的原名 {@code lineAmount}（不是 amount）。</p>
 */
@Component
@RequiredArgsConstructor
public class SaleOutboundPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="sale-outbound">}、sys_print_template.page_code 一致 */
    public static final String PAGE_CODE = "sale-outbound";

    private final SaleOutboundMapper outboundMapper;
    private final SaleOutboundItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        SaleOutbound doc = outboundMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        List<SaleOutboundItem> items = itemMapper.selectByOutboundId(documentId);
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("outboundNo", doc.getOutboundNo());
        data.put("outboundDate", doc.getOutboundDate());
        data.put("orderNo", doc.getOrderNo());
        data.put("status", doc.getStatus());
        data.put("outboundType", doc.getOutboundType());
        data.put("generationMethod", doc.getGenerationMethod());
        data.put("summary", doc.getSummary());
        data.put("creatorName", doc.getCreatorName());
        data.put("auditorName", doc.getAuditorName());
        data.put("bookkeeperName", doc.getBookkeeperName());
        data.put("bookkeepingTime", doc.getBookkeepingTime());
        data.put("printCount", doc.getPrintCount());
        // 客户与开票
        data.put("customerCode", doc.getCustomerCode());
        data.put("customerName", doc.getCustomerName());
        data.put("customerLevel", doc.getCustomerLevel());
        data.put("contactName", doc.getContactName());
        data.put("customerRemark", doc.getCustomerRemark());
        data.put("bankName", doc.getBankName());
        data.put("bankAccount", doc.getBankAccount());
        data.put("taxNo", doc.getTaxNo());
        // 仓库 / 经手
        data.put("warehouseName", doc.getWarehouseName());
        data.put("salesPersonName", doc.getSalesPersonName());
        data.put("departmentName", doc.getDepartmentName());
        data.put("location", doc.getLocation());
        data.put("region", doc.getRegion());
        // 收货
        data.put("receiverName", doc.getReceiverName());
        data.put("receiverPhone", doc.getReceiverPhone());
        data.put("shippingAddress", doc.getShippingAddress());
        // 配送
        data.put("deliveryMethod", doc.getDeliveryMethod());
        data.put("logisticsCompany", doc.getLogisticsCompany());
        data.put("logisticsBranch", doc.getLogisticsBranch());
        data.put("trackingNumber", doc.getTrackingNumber());
        data.put("waybillNo", doc.getWaybillNo());
        data.put("deliveryOrderNo", doc.getDeliveryOrderNo());
        data.put("deliveryDriver", doc.getDeliveryDriver());
        data.put("freightPayer", doc.getFreightPayer());
        data.put("freight", doc.getFreight());
        data.put("codAmount", doc.getCodAmount());
        // 金额
        data.put("totalQuantity", doc.getTotalQuantity());
        data.put("totalAmount", doc.getTotalAmount());
        data.put("promoDiscount", doc.getPromoDiscount());
        data.put("couponAmount", doc.getCouponAmount());
        data.put("directDiscount", doc.getDirectDiscount());
        data.put("otherFee", doc.getOtherFee());
        data.put("roundingAmount", doc.getRoundingAmount());
        data.put("settledAmount", doc.getSettledAmount());
        data.put("settlementMethod", doc.getSettlementMethod());
        data.put("settlementStatus", doc.getSettlementStatus());
        // 欠款与信用
        data.put("prevArrears", doc.getPrevArrears());
        data.put("currentArrears", doc.getCurrentArrears());
        data.put("arrearsBalance", doc.getArrearsBalance());
        data.put("creditLimit", doc.getCreditLimit());
        data.put("availableCredit", doc.getAvailableCredit());
        // 其他
        data.put("totalWeight", doc.getTotalWeight());
        data.put("totalVolume", doc.getTotalVolume());
        data.put("returnQuantity", doc.getReturnQuantity());
        data.put("returnAmount", doc.getReturnAmount());
        data.put("boxCount", doc.getBoxCount());
        data.put("expectedShipTime", doc.getExpectedShipTime());
        data.put("actualShipTime", doc.getActualShipTime());
        data.put("remark", doc.getRemark());
        data.put("internalNote", doc.getInternalNote());
        data.put("buyerRemark", doc.getBuyerRemark());

        // ── 明细（行号为空时按顺序补）──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (SaleOutboundItem item : items) {
            Map<String, Object> row = itemRow(item);
            if (row.get("lineNo") == null) {
                row.put("lineNo", fallbackLineNo);
            }
            fallbackLineNo++;
            rows.add(row);
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalLineAmount", sum(items, SaleOutboundItem::getLineAmount));
        data.put("totalOutboundQuantity", sum(items, SaleOutboundItem::getOutboundQuantity));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        SaleOutbound doc = outboundMapper.selectById(documentId);
        return doc != null ? doc.getOutboundNo() : null;
    }

    private Map<String, Object> itemRow(SaleOutboundItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineNo", item.getLineNo());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("specification", item.getSpecification());
        row.put("model", item.getModel());
        row.put("brand", item.getBrand());
        row.put("origin", item.getOrigin());
        // 单位在这张实体上叫 productUnit（表列 product_unit）
        row.put("productUnit", item.getProductUnit());
        row.put("location", item.getLocation());
        row.put("orderNo", item.getOrderNo());
        row.put("orderQuantity", item.getOrderQuantity());
        // 出库数量：必须用 outboundQuantity（quantity 是 exist=false 的临时别名）
        row.put("outboundQuantity", item.getOutboundQuantity());
        row.put("unitPrice", item.getUnitPrice());
        row.put("lineAmount", item.getLineAmount());
        row.put("discountRate", item.getDiscountRate());
        row.put("discountedAmount", item.getDiscountedAmount());
        row.put("taxRate", item.getTaxRate());
        row.put("batchNo", item.getBatchNo());
        row.put("shelfLife", item.getShelfLife());
        row.put("productionDate", item.getProductionDate());
        row.put("expiryDate", item.getExpiryDate());
        row.put("remark", item.getRemark());
        return row;
    }

    private BigDecimal sum(List<SaleOutboundItem> items,
                           Function<SaleOutboundItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleOutboundItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
