package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrder;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderItemMapper;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderMapper;
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
 * 预订货单打印数据装配器（pageCode = sale-pre-order）。
 *
 * <p>字段名**就是 SalePreOrder / SalePreOrderItem 的属性名**，模板里的 {@code field}
 * 能一眼对回实体列；明细金额用实体原名 {@code amount}（不是 ql361 那套 {@code totalmoney}）。</p>
 *
 * <p>单据头里只有 ID 的字段（客户/仓库/经手人/部门）在这张实体上**已经有名称快照**
 * （customerName / warehouseName / handlerName / deptName），所以不用再查一遍；
 * 查不到的就是空，不会打出 ID。</p>
 */
@Component
@RequiredArgsConstructor
public class SalePreOrderPrintDataProvider implements PrintDataProvider {

    /**
     * 与前端 {@code <PrintDialog page-code="sale-pre-order">}、sys_print_template.page_code 三处一致。
     *
     * <p>⚠️ 不能带 `/`：page-code 在业务级接口里是**路径段**
     * （/v2/print/documents/{pageCode}/templates），带斜杠前端会编成 %2F，Tomcat 直接判 400。</p>
     */
    public static final String PAGE_CODE = "sale-pre-order";

    private final SalePreOrderMapper orderMapper;
    private final SalePreOrderItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        SalePreOrder order = orderMapper.selectById(documentId);
        if (order == null) {
            return null;
        }
        List<SalePreOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SalePreOrderItem>()
                        .eq(SalePreOrderItem::getOrderId, documentId)
                        .orderByAsc(SalePreOrderItem::getLineNo));
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("orderNo", order.getOrderNo());
        data.put("orderDate", order.getOrderDate());
        data.put("status", order.getStatus());
        data.put("saleType", order.getSaleType());
        data.put("settlementStatus", order.getSettlementStatus());
        data.put("creatorName", order.getCreatorName());
        data.put("createTime", order.getCreateTime());
        data.put("submitterName", order.getSubmitterName());
        data.put("submitTime", order.getSubmitTime());
        data.put("auditorName", order.getAuditorName());
        data.put("approvedTime", order.getApprovedTime());
        // 客户与开票
        data.put("customerCode", order.getCustomerCode());
        data.put("customerName", order.getCustomerName());
        data.put("customerLevel", order.getCustomerLevel());
        data.put("customerTicket", order.getCustomerTicket());
        data.put("customerRemark", order.getCustomerRemark());
        data.put("bankName", order.getBankName());
        data.put("bankAccount", order.getBankAccount());
        data.put("taxNo", order.getTaxNo());
        // 仓库 / 经手
        data.put("warehouseName", order.getWarehouseName());
        data.put("handlerName", order.getHandlerName());
        data.put("deptName", order.getDeptName());
        // 收货
        data.put("receiverName", order.getReceiverName());
        data.put("receiverPhone", order.getReceiverPhone());
        data.put("shippingAddress", order.getShippingAddress());
        data.put("region", order.getRegion());
        // 金额与结算
        data.put("totalAmount", order.getTotalAmount());
        data.put("discountedAmount", order.getDiscountedAmount());
        data.put("orderAmount", order.getOrderAmount());
        // 预订金（这张单的核心：客户先付多少、还差多少）
        data.put("depositAmount", order.getDepositAmount());
        data.put("receivedDeposit", order.getReceivedDeposit());
        data.put("unreceivedDeposit", order.getUnreceivedDeposit());
        data.put("depositBalance", order.getDepositBalance());
        data.put("depositDeadline", order.getDepositDeadline());
        data.put("creditLimit", order.getCreditLimit());
        // 数量进度（预订 → 已订 → 已发）
        data.put("preOrderQuantity", order.getPreOrderQuantity());
        data.put("orderedQuantity", order.getOrderedQuantity());
        data.put("unOrderedQuantity", order.getUnOrderedQuantity());
        data.put("shippedQuantity", order.getShippedQuantity());
        data.put("unShippedQuantity", order.getUnShippedQuantity());
        data.put("totalWeight", order.getTotalWeight());
        data.put("totalVolume", order.getTotalVolume());
        // 备注与表尾自定义
        data.put("summary", order.getSummary());
        data.put("remark", order.getRemark());
        data.put("footerExtText1", order.getFooterExtText1());
        data.put("footerExtText2", order.getFooterExtText2());

        // ── 明细（行号可能为 null —— 打印时按顺序补上）──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (SalePreOrderItem item : items) {
            Map<String, Object> row = itemRow(item);
            if (row.get("lineNo") == null) {
                row.put("lineNo", fallbackLineNo);
            }
            fallbackLineNo++;
            rows.add(row);
        }
        data.put("items", rows);

        // ── 跨行汇总（实体存了就用实体的，它是权威值；为空才按明细算）──
        data.put("totalQuantity", order.getPreOrderQuantity() != null
                ? order.getPreOrderQuantity() : sum(items, SalePreOrderItem::getQuantity));
        data.put("totalLineAmount", sum(items, SalePreOrderItem::getAmount));
        data.put("totalDiscountedAmount", sum(items, SalePreOrderItem::getDiscountedAmount));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        SalePreOrder order = orderMapper.selectById(documentId);
        return order != null ? order.getOrderNo() : null;
    }

    private Map<String, Object> itemRow(SalePreOrderItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineNo", item.getLineNo());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("barcode", item.getBarcode());
        row.put("specification", item.getSpecification());
        row.put("model", item.getModel());
        row.put("origin", item.getOrigin());
        row.put("brand", item.getBrand());
        row.put("unit", item.getUnit());
        row.put("storageLocation", item.getLocation());
        row.put("availableStock", item.getAvailableStock());
        // 预订数量在这张实体上就叫 quantity
        row.put("quantity", item.getQuantity());
        row.put("orderedQuantity", item.getOrderedQuantity());
        row.put("unOrderedQuantity", item.getUnOrderedQuantity());
        row.put("shippedQuantity", item.getShippedQuantity());
        row.put("unShippedQuantity", item.getUnShippedQuantity());
        row.put("unitPrice", item.getUnitPrice());
        row.put("amount", item.getAmount());
        row.put("discountRate", item.getDiscountRate());
        row.put("discountedAmount", item.getDiscountedAmount());
        row.put("isGift", item.getGift());
        row.put("remark", item.getRemark());
        return row;
    }

    private BigDecimal sum(List<SalePreOrderItem> items,
                           Function<SalePreOrderItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalePreOrderItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
