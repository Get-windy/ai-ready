package cn.aiedge.wms.warehouse.listener;

import cn.aiedge.common.event.WarehouseChangedEvent;
import cn.aiedge.wms.entity.WmsWarehouse;
import cn.aiedge.wms.warehouse.mapper.WmsWarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仓库主数据同步（ERP 轨 → WMS 扩展表）。
 *
 * <p>监听 {@link WarehouseChangedEvent}：仓库规划页（写 {@code erp_warehouse}）发生新增/改名/停用/删除时，
 * 把变更同步到 {@code wms_warehouse} —— 后者才是全站业务单据「仓库」下拉的数据源
 * （{@code GET /wms/warehouse/list-all}）。没有这条同步链时，规划页新建的仓库在下拉里选不到。</p>
 *
 * <p><b>id 口径：</b>{@code wms_warehouse.id} 与 {@code warehouse_id} 都取 {@code erp_warehouse.id}，
 * 与现有种子数据（WH001~WH003）一致，避免业务表出现两套仓库 id 体系。</p>
 *
 * <p><b>字段映射说明：</b>{@code is_wms_enabled} 是「是否纳入 WMS 管理」，
 * 与 ERP 侧的 {@code status}（启用/停用）语义并不完全等价；
 * 此处按「停用则不纳入 WMS」的保守映射处理，其余 WMS 专属属性（类型/容量等）保持既有值不动。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSyncListener {

    private final WmsWarehouseMapper wmsWarehouseMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onWarehouseChanged(WarehouseChangedEvent ev) {
        if (ev == null || ev.getWarehouseId() == null || ev.getAction() == null) {
            return;
        }
        Long id = ev.getWarehouseId();
        WmsWarehouse exist = wmsWarehouseMapper.selectById(id);

        switch (ev.getAction()) {
            case CREATED -> {
                if (exist != null) {
                    log.warn("[仓库同步] 新建事件但 WMS 扩展行已存在，按更新处理: warehouseId={}", id);
                    applyBasic(exist, ev);
                    wmsWarehouseMapper.updateById(exist);
                    return;
                }
                WmsWarehouse created = new WmsWarehouse();
                created.setId(id);
                created.setWarehouseId(id);
                created.setWarehouseCode(ev.getWarehouseCode());
                created.setWarehouseName(ev.getWarehouseName());
                created.setWarehouseType(1);
                created.setIsWmsEnabled(ev.getStatus() == null || ev.getStatus() == 1 ? 1 : 0);
                created.setTenantId(ev.getTenantId());
                wmsWarehouseMapper.insert(created);
                log.info("[仓库同步] 已为 ERP 新建仓库补建 WMS 扩展行: warehouseId={}, code={}",
                        id, ev.getWarehouseCode());
            }
            case UPDATED -> {
                if (exist == null) {
                    log.warn("[仓库同步] 更新事件但 WMS 扩展行不存在，跳过（下次新建时会补建）: warehouseId={}", id);
                    return;
                }
                applyBasic(exist, ev);
                wmsWarehouseMapper.updateById(exist);
                log.info("[仓库同步] 已同步仓库编号/名称: warehouseId={}, code={}", id, ev.getWarehouseCode());
            }
            case STATUS_CHANGED -> {
                if (exist == null) {
                    return;
                }
                exist.setIsWmsEnabled(ev.getStatus() != null && ev.getStatus() == 1 ? 1 : 0);
                wmsWarehouseMapper.updateById(exist);
                log.info("[仓库同步] 已同步仓库启停: warehouseId={}, isWmsEnabled={}", id, exist.getIsWmsEnabled());
            }
            case DELETED -> {
                if (exist == null) {
                    return;
                }
                wmsWarehouseMapper.deleteById(id);
                log.info("[仓库同步] 已删除 WMS 扩展行: warehouseId={}", id);
            }
            default -> log.warn("[仓库同步] 未知动作，忽略: {}", ev.getAction());
        }
    }

    private void applyBasic(WmsWarehouse target, WarehouseChangedEvent ev) {
        if (ev.getWarehouseCode() != null) {
            target.setWarehouseCode(ev.getWarehouseCode());
        }
        if (ev.getWarehouseName() != null) {
            target.setWarehouseName(ev.getWarehouseName());
        }
    }
}
