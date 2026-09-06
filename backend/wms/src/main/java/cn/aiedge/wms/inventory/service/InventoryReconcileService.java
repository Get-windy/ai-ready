package cn.aiedge.wms.inventory.service;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.inventory.mapper.WmsInventoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存双账收敛（P0-2）· 初始化建档 + 对账。
 * <p>目的：把 ERP 仓库级总账（erp_stock）的存量在 WMS 库位级明细分账（wms_inventory）建档，
 * 并提供对账接口检测两表差异，逐步消除"双账割裂"。</p>
 * <p>注意：仅用于期初初始化/对账，不改变业务过账路径。初始化幂等（已有则跳过），对账只读。</p>
 */
@Service
public class InventoryReconcileService {

    @Resource
    private WmsInventoryMapper wmsInventoryMapper;
    @Resource
    private StockService stockService;

    /**
     * 把 erp_stock 存量（可用库存>0 的账）在 wms_inventory 建档（locationId=null 代表"期初/未指定库位"）。
     * 幂等：若该 (productId, warehouseId, batchNo) 在 wms 已有任何行则跳过，不覆盖 WMS 既有库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> initFromErp() {
        List<Stock> stocks = stockService.list();
        int created = 0, skipped = 0;
        for (Stock s : stocks) {
            if (s.getProductId() == null || s.getWarehouseId() == null) continue;
            if (s.getDeleted() != null && s.getDeleted() == 1) continue;
            BigDecimal avail = s.getAvailableQuantity() != null ? s.getAvailableQuantity() : BigDecimal.ZERO;
            if (avail.compareTo(BigDecimal.ZERO) <= 0) { skipped++; continue; }
            String batch = s.getBatchNo() == null ? "" : s.getBatchNo();
            // 已存在该 (product,warehouse,batch) 任何 wms 行 → 跳过（不覆盖、不重复）
            List<WmsInventory> exists = wmsInventoryMapper.selectList(new QueryWrapper<WmsInventory>()
                    .eq("product_id", s.getProductId())
                    .eq("warehouse_id", s.getWarehouseId())
                    .and(w -> w.isNull("batch_no").or().eq("batch_no", batch)));
            if (!exists.isEmpty()) { skipped++; continue; }
            WmsInventory inv = new WmsInventory();
            inv.setProductId(s.getProductId());
            inv.setProductCode(s.getProductCode());
            inv.setProductName(s.getProductName());
            inv.setProductUnit(s.getUnit());
            inv.setWarehouseId(s.getWarehouseId());
            inv.setWarehouseName(s.getWarehouseName());
            inv.setLocationId(null);
            inv.setLocationCode(null);
            inv.setBatchNo(s.getBatchNo());
            inv.setQuantity(s.getQuantity() != null ? s.getQuantity() : avail);
            inv.setAvailableQuantity(avail);
            inv.setFrozenQuantity(s.getFrozenQuantity() != null ? s.getFrozenQuantity() : BigDecimal.ZERO);
            inv.setUnitCost(s.getUnitPrice());
            inv.setProductionDate(s.getProductionDate());
            inv.setValidityDate(s.getValidityDate());
            wmsInventoryMapper.insert(inv);
            created++;
        }
        Map<String, Object> r = new HashMap<>();
        r.put("created", created);
        r.put("skipped", skipped);
        return r;
    }

    /**
     * 对账：按 (productId, warehouseId, batchNo) 聚合比较 erp_stock.available_quantity 与 wms_inventory(可用量合计)。
     * 仅返回差异（erp 与 wms 可用量不一致）的行。只读。
     */
    public List<Map<String, Object>> reconcile() {
        Map<String, BigDecimal> erp = new HashMap<>();
        for (Stock s : stockService.list()) {
            if (s.getProductId() == null || s.getWarehouseId() == null) continue;
            if (s.getDeleted() != null && s.getDeleted() == 1) continue;
            String k = key(s.getProductId(), s.getWarehouseId(), s.getBatchNo());
            erp.merge(k, nz(s.getAvailableQuantity()), BigDecimal::add);
        }
        Map<String, BigDecimal> wms = new HashMap<>();
        for (WmsInventory i : wmsInventoryMapper.selectList(null)) {
            if (i.getProductId() == null || i.getWarehouseId() == null) continue;
            String k = key(i.getProductId(), i.getWarehouseId(), i.getBatchNo());
            wms.merge(k, nz(i.getAvailableQuantity()), BigDecimal::add);
        }
        List<Map<String, Object>> diffs = new ArrayList<>();
        java.util.Set<String> keys = new java.util.HashSet<>(erp.keySet());
        keys.addAll(wms.keySet());
        for (String k : keys) {
            BigDecimal e = nz(erp.get(k));
            BigDecimal w = nz(wms.get(k));
            if (e.compareTo(w) != 0) {
                Map<String, Object> row = new HashMap<>();
                row.put("key", k);
                row.put("erpAvailable", e);
                row.put("wmsAvailable", w);
                row.put("diff", e.subtract(w));
                diffs.add(row);
            }
        }
        return diffs;
    }

    private String key(Long productId, Long warehouseId, String batchNo) {
        return productId + "|" + warehouseId + "|" + (batchNo == null ? "" : batchNo);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
