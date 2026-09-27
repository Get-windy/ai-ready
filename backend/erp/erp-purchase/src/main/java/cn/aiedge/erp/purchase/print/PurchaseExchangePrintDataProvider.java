package cn.aiedge.erp.purchase.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchangeItem;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.PurchaseExchangeItemMapper;
import cn.aiedge.erp.purchase.purchaseexchange.mapper.PurchaseExchangeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 采购换货单打印数据装配器（pageCode = purchase-exchange）。
 *
 * 结构与销售换货单同构，所以这里的处理也一致：
 * <ul>
 *   <li>换入 / 换出在同一张明细表里，靠 {@code warehouseType} 区分（1=换入，2=换出）；
 *       合成一个 {@code items} 列表并派生 {@code warehouseTypeName}，免去模板做枚举映射。</li>
 *   <li>实体没有行号字段，行号由装配器按顺序生成。</li>
 * </ul>
 *
 * <p>⚠️ 库中 {@code erp_purchase_exchange} 目前**一行数据都没有**（2026-09-27 实测），
 * 所以这份契约是照着实体与销售换货单镜像出来的，没有真实数据校验过字段是否有值。
 * 一有真单，先跑 `tools/verify-print-document.cjs purchase-exchange &lt;id&gt;` 复核。</p>
 */
@Component
@RequiredArgsConstructor
public class PurchaseExchangePrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="purchase-exchange">} 一致 */
    public static final String PAGE_CODE = "purchase-exchange";

    private static final int TYPE_IN = 1;
    private static final int TYPE_OUT = 2;

    private final PurchaseExchangeMapper exchangeMapper;
    private final PurchaseExchangeItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        PurchaseExchange doc = exchangeMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        List<PurchaseExchangeItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseExchangeItem>()
                        .eq(PurchaseExchangeItem::getExchangeId, documentId));
        if (items == null) {
            items = List.of();
        }
        // 换出在前、换入在后
        List<PurchaseExchangeItem> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparingInt(it -> it.getWarehouseType() == null ? Integer.MAX_VALUE
                : (TYPE_OUT == it.getWarehouseType() ? 0 : 1)));

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("exchangeNo", doc.getExchangeNo());
        data.put("exchangeDate", doc.getExchangeDate());
        data.put("status", doc.getStatus());
        data.put("settleStatus", doc.getSettleStatus());
        data.put("summary", doc.getSummary());
        data.put("printCount", doc.getPrintCount());
        data.put("bookingTime", doc.getBookkeepingTime());
        // 供应商
        data.put("supplierCode", doc.getSupplierCode());
        data.put("supplierName", doc.getSupplierName());
        data.put("supplierRemark", doc.getSupplierRemark());
        data.put("contactName", doc.getContactName());
        data.put("contactPhone", doc.getContactPhone());
        data.put("contactAddress", doc.getContactAddress());
        data.put("bankName", doc.getBankName());
        data.put("bankAccount", doc.getBankAccount());
        data.put("taxNo", doc.getTaxNo());
        // 换入 / 换出仓库与经办
        data.put("inWarehouseName", doc.getInWarehouseName());
        data.put("outWarehouseName", doc.getOutWarehouseName());
        data.put("handlerName", doc.getHandlerName());
        data.put("deptName", doc.getDeptName());
        data.put("bookkeeperName", doc.getBookkeeperName());
        // 来源与事由
        data.put("originalOrderNo", doc.getOriginalOrderNo());
        data.put("exchangeReason", doc.getExchangeReason());
        data.put("exchangeType", doc.getExchangeType());
        data.put("remark", doc.getRemark());
        // 金额
        data.put("productAmount", doc.getProductAmount());
        data.put("totalAmount", doc.getTotalAmount());
        data.put("discountAmount", doc.getDiscountAmount());
        data.put("settledAmount", doc.getSettledAmount());
        data.put("paidAmount", doc.getPaidAmount());
        data.put("totalWeight", doc.getTotalWeight());
        data.put("totalVolume", doc.getTotalVolume());
        // 欠款
        data.put("prevDebt", doc.getPrevDebt());
        data.put("currentDebt", doc.getCurrentDebt());
        data.put("debtBalance", doc.getDebtBalance());
        data.put("paymentDeadline", doc.getPaymentDeadline());

        // ── 明细（含换入换出，行号由装配器生成）──
        List<Map<String, Object>> rows = new ArrayList<>(ordered.size());
        int lineNo = 1;
        for (PurchaseExchangeItem item : ordered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineNo", lineNo++);
            row.put("warehouseType", item.getWarehouseType());
            row.put("warehouseTypeName", TYPE_IN == intOf(item.getWarehouseType()) ? "换入" : "换出");
            row.put("productCode", item.getProductCode());
            row.put("productName", item.getProductName());
            row.put("specification", item.getSpecification());
            row.put("model", item.getModel());
            row.put("brand", item.getBrand());
            row.put("origin", item.getOrigin());
            row.put("unit", item.getUnit());
            row.put("location", item.getLocation());
            row.put("batchBarcode", item.getBatchBarcode());
            row.put("productionDate", item.getProductionDate());
            row.put("expiryDate", item.getExpiryDate());
            row.put("quantity", item.getQuantity());
            row.put("unitPrice", item.getUnitPrice());
            row.put("amount", item.getAmount());
            row.put("discount", item.getDiscount());
            row.put("remark", item.getRemark());
            rows.add(row);
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalQuantity", sum(ordered, PurchaseExchangeItem::getQuantity));
        data.put("totalLineAmount", sum(ordered, PurchaseExchangeItem::getAmount));
        data.put("outQuantityTotal", doc.getOutQuantityTotal() != null
                ? doc.getOutQuantityTotal()
                : sum(filterByType(ordered, TYPE_OUT), PurchaseExchangeItem::getQuantity));
        data.put("inQuantityTotal", doc.getInQuantityTotal() != null
                ? doc.getInQuantityTotal()
                : sum(filterByType(ordered, TYPE_IN), PurchaseExchangeItem::getQuantity));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        PurchaseExchange doc = exchangeMapper.selectById(documentId);
        return doc != null ? doc.getExchangeNo() : null;
    }

    private List<PurchaseExchangeItem> filterByType(List<PurchaseExchangeItem> items, int type) {
        List<PurchaseExchangeItem> out = new ArrayList<>();
        for (PurchaseExchangeItem item : items) {
            if (intOf(item.getWarehouseType()) == type) {
                out.add(item);
            }
        }
        return out;
    }

    private int intOf(Integer value) {
        return value == null ? -1 : value;
    }

    private BigDecimal sum(List<PurchaseExchangeItem> items,
                           Function<PurchaseExchangeItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseExchangeItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
