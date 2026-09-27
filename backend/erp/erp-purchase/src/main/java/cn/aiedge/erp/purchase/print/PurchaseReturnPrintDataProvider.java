package cn.aiedge.erp.purchase.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnItemMapper;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnMapper;
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
 * 采购退货单打印数据装配器（pageCode = purchase-return）。
 *
 * 单据头全是**快照列**（供应商/仓库/经手人/部门都有名称），不需要回表取名。
 * 金额口径按库里实测选：{@code totalAmount} 与 {@code totalAmountWithTax} 都有值，
 * 明细金额字段用实体原名 {@code lineAmount}，退货数量是 {@code returnQuantity}。
 */
@Component
@RequiredArgsConstructor
public class PurchaseReturnPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="purchase-return">} 一致 */
    public static final String PAGE_CODE = "purchase-return";

    private final PurchaseReturnMapper returnMapper;
    private final PurchaseReturnItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        PurchaseReturn doc = returnMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        List<PurchaseReturnItem> items = itemMapper.selectByReturnId(documentId);
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("returnNo", doc.getReturnNo());
        data.put("returnDate", doc.getReturnDate());
        data.put("status", doc.getStatus());
        data.put("settleStatus", doc.getSettleStatus());
        data.put("returnType", doc.getReturnType());
        data.put("returnTypeDesc", doc.getReturnTypeDesc());
        data.put("summary", doc.getSummary());
        data.put("purchaseOrderNo", doc.getPurchaseOrderNo());
        data.put("reason", doc.getReason());
        data.put("createByName", doc.getCreateByName());
        data.put("createTime", doc.getCreateTime());
        data.put("applicantName", doc.getApplicantName());
        data.put("applyTime", doc.getApplyTime());
        data.put("approvedTime", doc.getApprovedTime());
        data.put("approvedNote", doc.getApprovedNote());
        data.put("posterName", doc.getPosterName());
        data.put("postTime", doc.getPostTime());
        data.put("printCount", doc.getPrintCount());
        // 供应商与开票
        data.put("supplierNo", doc.getSupplierNo());
        data.put("supplierName", doc.getSupplierName());
        data.put("contactName", doc.getContactName());
        data.put("contactPhone", doc.getContactPhone());
        data.put("contactAddress", doc.getContactAddress());
        data.put("supplierRemark", doc.getSupplierRemark());
        data.put("bankName", doc.getBankName());
        data.put("bankAccount", doc.getBankAccount());
        data.put("taxNo", doc.getTaxNo());
        // 仓库 / 经手
        data.put("warehouseName", doc.getWarehouseName());
        data.put("purchaserName", doc.getPurchaserName());
        data.put("departmentName", doc.getDepartmentName());
        // 金额
        data.put("totalQuantity", doc.getTotalQuantity());
        data.put("totalAmount", doc.getTotalAmount());
        data.put("discountAmount", doc.getDiscountAmount());
        data.put("taxAmount", doc.getTaxAmount());
        data.put("totalAmountWithTax", doc.getTotalAmountWithTax());
        data.put("settledAmount", doc.getSettledAmount());
        data.put("paymentAmount", doc.getPaymentAmount());
        data.put("paymentAccount", doc.getPaymentAccount());
        data.put("paymentDeadline", doc.getPaymentDeadline());
        // 欠款
        data.put("prevDebt", doc.getPrevDebt());
        data.put("currentDebt", doc.getCurrentDebt());
        data.put("debtBalance", doc.getDebtBalance());
        data.put("prevPrepaid", doc.getPrevPrepaid());
        data.put("refundPrepay", doc.getRefundPrepay());
        data.put("prepaidBalance", doc.getPrepaidBalance());
        // 其他
        data.put("weight", doc.getWeight());
        data.put("volume", doc.getVolume());
        data.put("remark", doc.getRemark());

        // ── 明细 ──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (PurchaseReturnItem item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineNo", item.getLineNo() != null ? item.getLineNo() : fallbackLineNo);
            row.put("productCode", item.getProductCode());
            row.put("productName", item.getProductName());
            row.put("productSpec", item.getProductSpec());
            row.put("productUnit", item.getProductUnit());
            row.put("model", item.getModel());
            row.put("brand", item.getBrand());
            row.put("origin", item.getOrigin());
            row.put("location", item.getLocation());
            row.put("batchNo", item.getBatchNo());
            row.put("productionDate", item.getProductionDate());
            row.put("shelfLife", item.getShelfLife());
            row.put("expiryDate", item.getExpiryDate());
            row.put("returnQuantity", item.getReturnQuantity());
            row.put("unitPrice", item.getUnitPrice());
            row.put("lineAmount", item.getLineAmount());
            row.put("unitCost", item.getUnitCost());
            row.put("costAmount", item.getCostAmount());
            row.put("remark", item.getRemark());
            rows.add(row);
            fallbackLineNo++;
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalLineAmount", sum(items, PurchaseReturnItem::getLineAmount));
        data.put("totalLineQuantity", sum(items, PurchaseReturnItem::getReturnQuantity));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        PurchaseReturn doc = returnMapper.selectById(documentId);
        return doc != null ? doc.getReturnNo() : null;
    }

    private BigDecimal sum(List<PurchaseReturnItem> items,
                           Function<PurchaseReturnItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturnItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
