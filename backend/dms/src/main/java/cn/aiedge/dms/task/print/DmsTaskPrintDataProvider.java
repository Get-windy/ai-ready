package cn.aiedge.dms.task.print;

import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.entity.DmsTaskItem;
import cn.aiedge.dms.task.mapper.DmsTaskItemMapper;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
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
 * 配送任务单打印数据装配器（pageCode = dispatch-task）。
 *
 * <p>字段名就是 {@code DmsTask / DmsTaskItem} 的属性名。这张单上要打的东西很杂
 * （客户/收货地址、骑手/车辆、金额与代收、箱数/退货、各节点时间），
 * 所以按业务分组平铺：收货信息 → 运力信息 → 数量金额 → 节点时间。
 *
 * <p>注意明细的数量/金额字段在这张实体上叫 {@code quantity / amount}，
 * 规格叫 {@code spec}（不是 specification）——跟各自实体走。</p>
 */
@Component
@RequiredArgsConstructor
public class DmsTaskPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="dispatch-task">} 一致 */
    public static final String PAGE_CODE = "dispatch-task";

    private final DmsTaskMapper taskMapper;
    private final DmsTaskItemMapper itemMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        DmsTask task = taskMapper.selectById(documentId);
        if (task == null) {
            return null;
        }
        List<DmsTaskItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<DmsTaskItem>()
                        .eq(DmsTaskItem::getTaskId, documentId)
                        .orderByAsc(DmsTaskItem::getLineNo));
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // ── 单头 ──
        data.put("taskNo", task.getTaskNo());
        data.put("orderNo", task.getOrderNo());
        data.put("orderType", task.getOrderType());
        data.put("dispatchType", task.getDispatchType());
        data.put("status", task.getStatus());
        data.put("priority", task.getPriority());
        data.put("deliveryDate", task.getDeliveryDate());
        data.put("sourceBillNo", task.getSourceBillNo());
        data.put("sourceAddress", task.getSourceAddress());
        data.put("creatorName", task.getCreatorName());
        data.put("createTime", task.getCreateTime());
        data.put("remark", task.getRemark());
        // ── 收货信息 ──
        data.put("customerName", task.getCustomerName());
        data.put("customerPhone", task.getCustomerPhone());
        data.put("customerAddress", task.getCustomerAddress());
        // ── 运力信息 ──
        data.put("riderName", task.getRiderName());
        data.put("riderPhone", task.getRiderPhone());
        data.put("deliverymanName", task.getDeliverymanName());
        data.put("vehicleName", task.getVehicleName());
        data.put("routeArea", task.getRouteArea());
        // ── 数量 / 金额 ──
        data.put("totalItems", task.getTotalItems());
        data.put("totalQuantity", task.getTotalQuantity());
        data.put("totalWeight", task.getTotalWeight());
        data.put("totalVolume", task.getTotalVolume());
        data.put("goodsAmount", task.getGoodsAmount());
        data.put("deliveryFee", task.getDeliveryFee());
        data.put("collectOnDelivery", task.getCollectOnDelivery());
        data.put("depositAmount", task.getDepositAmount());
        data.put("returnOrderCount", task.getReturnOrderCount());
        data.put("returnQuantity", task.getReturnQuantity());
        data.put("returnAmount", task.getReturnAmount());
        data.put("boxQuantity", task.getBoxQuantity());
        data.put("orderCount", task.getOrderCount());
        data.put("estimatedDistance", task.getEstimatedDistance());
        data.put("distanceKm", task.getDistanceKm());
        // ── 节点时间 ──
        data.put("deadlineTime", task.getDeadlineTime());
        data.put("dispatchTime", task.getDispatchTime());
        data.put("pickupTime", task.getPickupTime());
        data.put("deliveryTime", task.getDeliveryTime());
        data.put("completedTime", task.getCompletedTime());

        // ── 明细（行号为空时按顺序补）──
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        int fallbackLineNo = 1;
        for (DmsTaskItem item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineNo", item.getLineNo() != null ? item.getLineNo() : fallbackLineNo);
            row.put("productCode", item.getProductCode());
            row.put("productName", item.getProductName());
            row.put("spec", item.getSpec());
            row.put("unit", item.getUnit());
            row.put("quantity", item.getQuantity());
            row.put("unitPrice", item.getUnitPrice());
            row.put("amount", item.getAmount());
            row.put("weight", item.getWeight());
            row.put("volume", item.getVolume());
            row.put("remark", item.getRemark());
            rows.add(row);
            fallbackLineNo++;
        }
        data.put("items", rows);

        // ── 跨行汇总 ──
        data.put("totalLineAmount", sum(items, DmsTaskItem::getAmount));
        data.put("totalLineQuantity", sum(items, DmsTaskItem::getQuantity));
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        DmsTask task = taskMapper.selectById(documentId);
        return task != null ? task.getTaskNo() : null;
    }

    private BigDecimal sum(List<DmsTaskItem> items, Function<DmsTaskItem, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (DmsTaskItem item : items) {
            BigDecimal value = getter.apply(item);
            if (Objects.nonNull(value)) {
                total = total.add(value);
            }
        }
        return total;
    }
}
