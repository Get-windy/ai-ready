package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDocItem;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocItemMapper;
import cn.aiedge.erp.sale.returnDoc.mapper.SaleReturnDocMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * 销售退货单打印数据装配器（pageCode = sale-return-doc）。
 *
 * 字段名**就是 SaleReturnDoc / SaleReturnDocItem 的属性名**，模板里的 field 能一眼对回实体列。
 * ⚠️ 明细金额在这里叫 {@code lineAmount}（SaleReturnDocItem 的原名），
 * 而销售订单明细叫 {@code amount}（SaleOrderItem 的原名）—— 各单据的契约就是它自己的实体，
 * 装配器负责翻译，模板照着对应单据的实体写。
 */
@Component
@RequiredArgsConstructor
public class SaleReturnDocPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="sale-return-doc">}、sys_print_template.page_code 一致 */
    public static final String PAGE_CODE = "sale-return-doc";

    private final SaleReturnDocMapper docMapper;
    private final SaleReturnDocItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        SaleReturnDoc doc = docMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        List<SaleReturnDocItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SaleReturnDocItem>()
                        .eq(SaleReturnDocItem::getReturnDocId, documentId)
                        .orderByAsc(SaleReturnDocItem::getLineNo));
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("returnDocNo", doc.getReturnDocNo());
        data.put("orderDate", doc.getOrderDate());
        data.put("status", doc.getStatus());
        data.put("salesType", doc.getSalesType());
        data.put("generateType", doc.getGenerateType());
        data.put("settleStatus", doc.getSettleStatus());
        data.put("creatorName", doc.getCreatorName());
        data.put("createTime", doc.getCreateTime());
        // 客户与开票
        data.put("customerCode", doc.getCustomerCode());
        data.put("customerName", doc.getCustomerName());
        data.put("customerLevel", doc.getCustomerLevel());
        data.put("contactName", doc.getContactName());
        data.put("contactPhone", doc.getContactPhone());
        data.put("contactAddress", doc.getContactAddress());
        data.put("customerRemark", doc.getCustomerRemark());
        data.put("customerTicket", doc.getCustomerTicket());
        data.put("bankName", doc.getBankName());
        data.put("bankAccount", doc.getBankAccount());
        data.put("taxNo", doc.getTaxNo());
        // 仓库 / 经手
        data.put("warehouseName", doc.getWarehouseName());
        data.put("handlerName", doc.getHandlerName());
        data.put("deptName", doc.getDeptName());
        // 收货
        data.put("receiverName", doc.getReceiverName());
        data.put("receiverPhone", doc.getReceiverPhone());
        data.put("shippingAddress", doc.getShippingAddress());
        data.put("region", doc.getRegion());
        // 配送
        data.put("deliveryMethod", doc.getDeliveryMethod());
        data.put("deliveryRoute", doc.getDeliveryRoute());
        data.put("logisticsCompany", doc.getLogisticsCompany());
        data.put("waybillNo", doc.getWaybillNo());
        data.put("driverName", doc.getDriverName());
        data.put("deliveryVehicle", doc.getDeliveryVehicle());
        data.put("shippingFee", doc.getShippingFee());
        data.put("freightPayer", doc.getFreightPayer());
        data.put("codAmount", doc.getCodAmount());
        // 金额
        data.put("productAmount", doc.getProductAmount());
        data.put("promoDiscount", doc.getPromoDiscount());
        data.put("couponAmount", doc.getCouponAmount());
        data.put("directDiscount", doc.getDirectDiscount());
        data.put("discountAmount", doc.getDiscountAmount());
        data.put("otherFee", doc.getOtherFee());
        data.put("billAmount", doc.getBillAmount());
        data.put("totalAmount", doc.getTotalAmount());
        data.put("discountBillAmount", doc.getDiscountBillAmount());
        data.put("settledAmount", doc.getSettledAmount());
        data.put("receivableReduce", doc.getReceivableReduce());
        // 欠款（打印在退货单上供对账）
        data.put("prevDebt", doc.getPrevDebt());
        data.put("currentDebt", doc.getCurrentDebt());
        data.put("debtBalance", doc.getDebtBalance());
        // 来源与退货事由
        data.put("sourceOrder", doc.getSourceOrder());
        data.put("returnApplyNo", doc.getReturnApplyNo());
        data.put("returnType", doc.getReturnType());
        data.put("reason", doc.getReason());
        data.put("remark", doc.getRemark());

        // ── 明细（行号可能为 null —— 库里有 line_no 空的单，打印时按顺序补上）──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (SaleReturnDocItem item : items) {
            Map<String, Object> row = itemRow(item);
            if (row.get("lineNo") == null) {
                row.put("lineNo", fallbackLineNo);
            }
            fallbackLineNo++;
            rows.add(row);
        }
        data.put("items", rows);

        // ── 跨行汇总（实体存了就用实体的，它是权威值；为空才按明细算）──
        data.put("totalQuantity", doc.getTotalQuantity() != null
                ? doc.getTotalQuantity() : sum(items, SaleReturnDocItem::getReturnQuantity));
        data.put("totalLineAmount", sum(items, SaleReturnDocItem::getLineAmount));
        data.put("totalDiscountedAmount", sum(items, SaleReturnDocItem::getDiscountedAmount));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        SaleReturnDoc doc = docMapper.selectById(documentId);
        return doc != null ? doc.getReturnDocNo() : null;
    }

    private Map<String, Object> itemRow(SaleReturnDocItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineNo", item.getLineNo());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("specification", item.getSpecification());
        row.put("modelNo", item.getModelNo());
        row.put("brand", item.getBrand());
        row.put("unit", item.getUnit());
        row.put("storageLocation", item.getStorageLocation());
        row.put("batchBarcode", item.getBatchBarcode());
        row.put("productionDate", item.getProductionDate());
        row.put("expiryDate", item.getExpiryDate());
        row.put("shelfLife", item.getShelfLife());
        // ⚠️ 退货数量在这张实体上叫 returnQuantity（不是 quantity）
        row.put("returnQuantity", item.getReturnQuantity());
        row.put("receivedQuantity", item.getReceivedQuantity());
        row.put("unitPrice", item.getUnitPrice());
        row.put("lineAmount", item.getLineAmount());
        row.put("discountRate", item.getDiscountRate());
        row.put("discountedAmount", item.getDiscountedAmount());
        row.put("isGift", item.getIsGift());
        row.put("remark", item.getRemark());
        return row;
    }

    private BigDecimal sum(List<SaleReturnDocItem> items,
                           Function<SaleReturnDocItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleReturnDocItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
