package cn.aiedge.wms.inventory.service;

import cn.aiedge.wms.inventory.service.InventoryReconcileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 库存双账收敛（P0-2）控制器：期初建档 + 对账。
 * <p>仅提供运维/迁移辅助接口，不参与业务过账。可独立验证。</p>
 */
@RestController
@RequestMapping("/api/wms/inventory")
public class InventoryReconcileController {

    private final InventoryReconcileService reconcileService;

    public InventoryReconcileController(InventoryReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    /**
     * 期初建档：把 erp_stock 存量写入 wms_inventory（幂等）。返回 created/skipped 统计。
     */
    @PostMapping("/init-from-erp")
    public Map<String, Object> initFromErp() {
        return reconcileService.initFromErp();
    }

    /**
     * 对账：比较 erp_stock 与 wms_inventory 按 (product,warehouse,batch) 聚合的可用量差异。
     */
    @GetMapping("/reconcile")
    public List<Map<String, Object>> reconcile() {
        return reconcileService.reconcile();
    }
}
