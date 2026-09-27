package cn.aiedge.erp.purchase.print;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.UserService;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.entity.PurchaseOrderPartnerSnapshot;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderPartnerSnapshotMapper;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 采购订单打印数据装配器（pageCode = purchase）。
 *
 * <p><b>为什么不能只摊平实体</b>：{@code erp_purchase_order} 上供应商/仓库/经手人都只存 ID，
 * 没有名称快照列。所以这里必须补三处：
 * <ul>
 *   <li>供应商与开票信息 → {@code erp_purchase_order_partner_snapshot}（下单时落的快照，最准）；</li>
 *   <li>仓库名 → erp-stock 的 {@code WarehouseService}；</li>
 *   <li>经手人姓名 → core-base 的 {@code UserService}（取 realName，空则退 nickname）。</li>
 * </ul>
 * 查不到的名一律留空，**不打印 ID** —— 单据上出现一串雪花号比空着更糟。
 *
 * <p>金额口径：本单金额用 {@code billAmount}（{@code bill_amount}）。
 * 注意 {@code totalAmount} 是统计沿用的旧列，本模块业务写入不设置它，别拿它当单据金额。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="purchase">}、sys_print_template.page_code 一致 */
    public static final String PAGE_CODE = "purchase";

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final PurchaseOrderPartnerSnapshotMapper partnerSnapshotMapper;
    private final WarehouseService warehouseService;
    private final UserService userService;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        PurchaseOrder order = orderMapper.selectById(documentId);
        if (order == null) {
            return null;
        }
        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(documentId);
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单据头 ──
        data.put("orderNo", order.getOrderNo());
        data.put("orderDate", order.getOrderDate());
        data.put("status", order.getStatus());
        data.put("approvalStatus", order.getApprovalStatus());
        data.put("purchaseType", order.getPurchaseType());
        data.put("expectedReceiveTime", order.getExpectedReceiveTime());
        data.put("sourceBillNo", order.getSourceBillNo());
        data.put("remark", order.getRemark());
        data.put("createTime", order.getCreateTime());
        // 金额：本单金额 = billAmount（totalAmount 是统计旧列，不当作单据金额）
        data.put("productAmount", order.getProductAmount());
        data.put("discountAmount", order.getDiscountAmount());
        data.put("billAmount", order.getBillAmount());
        data.put("settledAmount", order.getSettledAmount());
        data.put("receivedAmount", order.getReceivedAmount());
        data.put("totalQuantity", order.getTotalQuantity());

        // ── 供应商（快照；没有快照就退回 ID 之外的空值）──
        PurchaseOrderPartnerSnapshot snapshot = partnerSnapshotMapper.selectOne(
                new LambdaQueryWrapper<PurchaseOrderPartnerSnapshot>()
                        .eq(PurchaseOrderPartnerSnapshot::getOrderId, documentId)
                        .last("limit 1"));
        if (snapshot != null) {
            data.put("supplierName", snapshot.getSupplierName());
            data.put("supplierCode", snapshot.getSupplierCode());
            data.put("contactName", snapshot.getContactName());
            data.put("contactPhone", snapshot.getContactPhone());
            data.put("contactAddress", snapshot.getContactAddress());
            data.put("bankName", snapshot.getBankName());
            data.put("bankAccount", snapshot.getBankAccount());
            data.put("taxNo", snapshot.getTaxNo());
            data.put("supplierRemark", snapshot.getSupplierRemark());
        }
        // ── 仓库 / 经手人（实体上只有 ID，这里补名称）──
        data.put("warehouseName", warehouseName(order.getWarehouseId()));
        data.put("purchaserName", userName(order.getPurchaserId()));

        // ── 明细 ──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (PurchaseOrderItem item : items) {
            Map<String, Object> row = itemRow(item);
            if (row.get("lineNo") == null) {
                row.put("lineNo", fallbackLineNo);
            }
            fallbackLineNo++;
            rows.add(row);
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalLineAmount", sum(items, PurchaseOrderItem::getAmount));
        data.put("totalReceivedQuantity", sum(items, PurchaseOrderItem::getReceivedQuantityDetail));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        PurchaseOrder order = orderMapper.selectById(documentId);
        return order != null ? order.getOrderNo() : null;
    }

    private Map<String, Object> itemRow(PurchaseOrderItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineNo", item.getLineNo());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("specification", item.getSpecification());
        row.put("model", item.getModel());
        row.put("brand", item.getBrand());
        row.put("origin", item.getOrigin());
        row.put("unit", item.getUnit());
        row.put("location", item.getLocation());
        row.put("batchCode", item.getBatchCode());
        row.put("productionDate", item.getProductionDate());
        row.put("expiryDate", item.getExpiryDate());
        row.put("shelfLife", item.getShelfLife());
        row.put("quantity", item.getQuantity());
        row.put("unitPrice", item.getUnitPrice());
        row.put("amount", item.getAmount());
        row.put("discountRate", item.getDiscountRate());
        row.put("discountedAmount", item.getDiscountedAmount());
        row.put("receivedQuantityDetail", item.getReceivedQuantityDetail());
        row.put("gift", item.getGift());
        row.put("remark", item.getRemark());
        return row;
    }

    /** 仓库名；无 ID 或查不到返回 null（不打印 ID） */
    private String warehouseName(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        try {
            Warehouse warehouse = warehouseService.getById(warehouseId);
            return warehouse != null ? warehouse.getWarehouseName() : null;
        } catch (Exception e) {
            log.warn("打印采购订单：取仓库名失败 warehouseId={} - {}", warehouseId, e.getMessage());
            return null;
        }
    }

    /** 经手人姓名；取 realName，空则退 nickname */
    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser user = userService.getById(userId);
            if (user == null) {
                return null;
            }
            return user.getRealName() != null && !user.getRealName().isBlank()
                    ? user.getRealName() : user.getNickname();
        } catch (Exception e) {
            log.warn("打印采购订单：取经手人姓名失败 userId={} - {}", userId, e.getMessage());
            return null;
        }
    }

    private BigDecimal sum(List<PurchaseOrderItem> items,
                           Function<PurchaseOrderItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
