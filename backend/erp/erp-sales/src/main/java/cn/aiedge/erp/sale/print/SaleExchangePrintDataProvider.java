package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeItemMapper;
import cn.aiedge.erp.sale.saleexchange.mapper.SaleExchangeMapper;
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
 * 销售换货单打印数据装配器（pageCode = sale-exchange）。
 *
 * 两个需要说明的地方：
 * <ul>
 *   <li><b>换入 / 换出在同一张明细表里</b>，靠 {@code warehouseType} 区分（1=换入，2=换出）。
 *       这里把两个方向合成一个 {@code items} 列表，并派生 {@code warehouseTypeName} 供模板直接打，
 *       同时按「换出在前、换入在后」排序 —— 一张纸上一张表，靠类型列分开，
 *       不必为两个方向准备两张表。</li>
 *   <li><b>实体没有行号字段</b>（SaleExchangeItem 无 lineNo），所以行号由装配器按顺序生成。</li>
 * </ul>
 *
 * <p>已知边界：v2 引擎目前一个模板只支持一个明细表（{@code sections.items} 单例），
 * 所以「换出、换入各打一张表」这种版式暂时表达不了，先用类型列代替。</p>
 */
@Component
@RequiredArgsConstructor
public class SaleExchangePrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="sale-exchange">}、sys_print_template.page_code 一致 */
    public static final String PAGE_CODE = "sale-exchange";

    /** 仓库类型：1=换入 */
    private static final int TYPE_IN = 1;
    /** 仓库类型：2=换出 */
    private static final int TYPE_OUT = 2;

    private final SaleExchangeMapper exchangeMapper;
    private final SaleExchangeItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        SaleExchange doc = exchangeMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        List<SaleExchangeItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SaleExchangeItem>()
                        .eq(SaleExchangeItem::getExchangeId, documentId));
        if (items == null) {
            items = List.of();
        }
        // 换出在前、换入在后（打印时先看退出去的，再看换回来的）
        List<SaleExchangeItem> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparingInt(it -> it.getWarehouseType() == null ? Integer.MAX_VALUE
                : (TYPE_OUT == it.getWarehouseType() ? 0 : 1)));

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("exchangeNo", doc.getExchangeNo());
        data.put("exchangeDate", doc.getExchangeDate());
        data.put("status", doc.getStatus());
        data.put("salesType", doc.getSalesType());
        data.put("settleStatus", doc.getSettleStatus());
        data.put("summary", doc.getSummary());
        data.put("creatorName", doc.getCreatorName());
        data.put("createTime", doc.getCreateTime());
        // 客户与开票
        data.put("customerCode", doc.getCustomerCode());
        data.put("customerName", doc.getCustomerName());
        data.put("customerLevel", doc.getCustomerLevel());
        data.put("contactName", doc.getContactName());
        data.put("contactPhone", doc.getContactPhone());
        data.put("contactAddress", doc.getContactAddress());
        data.put("bankName", doc.getBankName());
        data.put("bankAccount", doc.getBankAccount());
        data.put("taxNo", doc.getTaxNo());
        // 换入 / 换出仓库
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
        data.put("receivedAmount", doc.getReceivedAmount());
        data.put("totalWeight", doc.getTotalWeight());
        data.put("totalVolume", doc.getTotalVolume());
        // 欠款与信用
        data.put("prevDebt", doc.getPrevDebt());
        data.put("currentDebt", doc.getCurrentDebt());
        data.put("debtBalance", doc.getDebtBalance());
        data.put("creditLimit", doc.getCreditLimit());
        data.put("availableCredit", doc.getAvailableCredit());
        data.put("collectionDeadline", doc.getCollectionDeadline());
        // 审批
        data.put("approvedByName", doc.getApprovedByName());
        data.put("approvedTime", doc.getApprovedTime());
        data.put("completedTime", doc.getCompletedTime());

        // ── 明细（含换入换出，行号由装配器生成）──
        List<Map<String, Object>> rows = new ArrayList<>(ordered.size());
        int lineNo = 1;
        for (SaleExchangeItem item : ordered) {
            Map<String, Object> row = itemRow(item);
            row.put("lineNo", lineNo++);
            rows.add(row);
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalQuantity", sum(ordered, SaleExchangeItem::getQuantity));
        data.put("totalLineAmount", sum(ordered, SaleExchangeItem::getAmount));
        data.put("outQuantityTotal", doc.getOutQuantityTotal() != null
                ? doc.getOutQuantityTotal()
                : sum(filterByType(ordered, TYPE_OUT), SaleExchangeItem::getQuantity));
        data.put("inQuantityTotal", doc.getInQuantityTotal() != null
                ? doc.getInQuantityTotal()
                : sum(filterByType(ordered, TYPE_IN), SaleExchangeItem::getQuantity));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        SaleExchange doc = exchangeMapper.selectById(documentId);
        return doc != null ? doc.getExchangeNo() : null;
    }

    private Map<String, Object> itemRow(SaleExchangeItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("warehouseType", item.getWarehouseType());
        row.put("warehouseTypeName", TYPE_IN == intOf(item.getWarehouseType()) ? "换入" : "换出");
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("specification", item.getSpecification());
        row.put("model", item.getModel());
        row.put("brand", item.getBrand());
        row.put("unit", item.getUnit());
        row.put("location", item.getLocation());
        row.put("batchBarcode", item.getBatchBarcode());
        row.put("productionDate", item.getProductionDate());
        row.put("expiryDate", item.getExpiryDate());
        row.put("shelfLife", item.getShelfLife());
        row.put("quantity", item.getQuantity());
        row.put("unitPrice", item.getUnitPrice());
        row.put("amount", item.getAmount());
        row.put("discountAmount", item.getDiscountAmount());
        row.put("isGift", item.getIsGift());
        row.put("remark", item.getRemark());
        return row;
    }

    private List<SaleExchangeItem> filterByType(List<SaleExchangeItem> items, int type) {
        List<SaleExchangeItem> out = new ArrayList<>();
        for (SaleExchangeItem item : items) {
            if (intOf(item.getWarehouseType()) == type) {
                out.add(item);
            }
        }
        return out;
    }

    private int intOf(Integer value) {
        return value == null ? -1 : value;
    }

    private BigDecimal sum(List<SaleExchangeItem> items,
                           Function<SaleExchangeItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleExchangeItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
