package cn.aiedge.erp.purchase.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundItemMapper;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundMapper;
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
 * 采购入库单打印数据装配器（pageCode = purchase-inbound）。
 *
 * 金额口径按库里实测选：{@code totalAmount}（{@code total_amount}）有值，
 * {@code taxAmount} 全为 0 —— 所以模板的「金额合计」用 {@code totalAmount}，
 * 含税金额单独给 {@code totalAmountWithTax}，不要在模板里指望 taxAmount。
 *
 * 明细的入库数量是 {@code inboundQuantity}（表列 {@code inbound_quantity}），
 * 与订单数量的差额留在 {@code orderQuantity}，两个都给，模板按需选。
 */
@Component
@RequiredArgsConstructor
public class PurchaseInboundPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="purchase-inbound">} 一致 */
    public static final String PAGE_CODE = "purchase-inbound";

    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        PurchaseInbound doc = inboundMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        List<PurchaseInboundItem> items = itemMapper.selectByInboundId(documentId);
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("inboundNo", doc.getInboundNo());
        data.put("inboundDate", doc.getInboundDate());
        data.put("inboundType", doc.getInboundType());
        data.put("status", doc.getStatus());
        data.put("settleStatus", doc.getSettleStatus());
        data.put("summary", doc.getSummary());
        data.put("orderNo", doc.getOrderNo());
        data.put("contractNo", doc.getContractNo());
        data.put("createByName", doc.getCreateByName());
        data.put("createTime", doc.getCreateTime());
        data.put("posterName", doc.getPosterName());
        data.put("postTime", doc.getPostTime());
        data.put("approvedByName", doc.getApprovedByName());
        data.put("approvedTime", doc.getApprovedTime());
        data.put("approvedNote", doc.getApprovedNote());
        data.put("printCount", doc.getPrintCount());
        // 供应商
        data.put("supplierName", doc.getSupplierName());
        // 仓库 / 经手
        data.put("warehouseName", doc.getWarehouseName());
        data.put("purchaserName", doc.getPurchaserName());
        data.put("departmentName", doc.getDepartmentName());
        // 物流与到货
        data.put("trackingNumber", doc.getTrackingNumber());
        data.put("logisticsCompany", doc.getLogisticsCompany());
        data.put("expectedArrivalTime", doc.getExpectedArrivalTime());
        data.put("actualArrivalTime", doc.getActualArrivalTime());
        // 收货 / 质检 / 完成节点
        data.put("receivedTime", doc.getReceivedTime());
        data.put("qualityCheckedTime", doc.getQualityCheckedTime());
        data.put("qualityCheckResult", doc.getQualityCheckResult());
        data.put("warehouseConfirmedTime", doc.getWarehouseConfirmedTime());
        data.put("completedTime", doc.getCompletedTime());
        // 金额
        data.put("totalQuantity", doc.getTotalQuantity());
        data.put("totalAmount", doc.getTotalAmount());
        data.put("taxAmount", doc.getTaxAmount());
        data.put("totalAmountWithTax", doc.getTotalAmountWithTax());
        data.put("discountAmount", doc.getDiscountAmount());
        data.put("settledAmount", doc.getSettledAmount());
        data.put("fee", doc.getFee());
        data.put("weight", doc.getWeight());
        data.put("volume", doc.getVolume());
        // 备注
        data.put("remark", doc.getRemark());
        data.put("internalNote", doc.getInternalNote());

        // ── 明细 ──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (PurchaseInboundItem item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineNo", item.getLineNo() != null ? item.getLineNo() : fallbackLineNo);
            row.put("productCode", item.getProductCode());
            row.put("productName", item.getProductName());
            row.put("productSpec", item.getProductSpec());
            row.put("productUnit", item.getProductUnit());
            row.put("orderQuantity", item.getOrderQuantity());
            row.put("inboundQuantity", item.getInboundQuantity());
            row.put("pendingQuantity", item.getPendingQuantity());
            row.put("unitPrice", item.getUnitPrice());
            row.put("lineAmount", item.getLineAmount());
            row.put("taxRate", item.getTaxRate());
            row.put("taxAmount", item.getTaxAmount());
            row.put("lineTotal", item.getLineTotal());
            row.put("batchNo", item.getBatchNo());
            row.put("productionDate", item.getProductionDate());
            row.put("validityDate", item.getValidityDate());
            row.put("warehouseLocationCode", item.getWarehouseLocationCode());
            row.put("qualityStatus", item.getQualityStatus());
            row.put("qualityNote", item.getQualityNote());
            row.put("remark", item.getRemark());
            rows.add(row);
            fallbackLineNo++;
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalLineAmount", sum(items, PurchaseInboundItem::getLineAmount));
        data.put("totalInboundQuantity", sum(items, PurchaseInboundItem::getInboundQuantity));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        PurchaseInbound doc = inboundMapper.selectById(documentId);
        return doc != null ? doc.getInboundNo() : null;
    }

    private BigDecimal sum(List<PurchaseInboundItem> items,
                           Function<PurchaseInboundItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseInboundItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
