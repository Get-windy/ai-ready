package cn.aiedge.common.event;

/**
 * 仓库主数据变更事件（进程内）。
 *
 * <p><b>背景：</b>仓储相关的主数据分布在两张表 ——</p>
 * <ul>
 *   <li>{@code erp_warehouse}：仓库规划页（资料 → 仓库管理 → 仓库规划）维护的主数据，
 *       携带地址/联系人/上级仓库/分类等通用属性；</li>
 *   <li>{@code wms_warehouse}：WMS 侧扩展表，{@code warehouse_id} 指向 {@code erp_warehouse.id}，
 *       承载 {@code is_wms_enabled} / {@code warehouse_type} / 容量等 WMS 专属属性。</li>
 * </ul>
 *
 * <p><b>要解决的问题：</b>全站业务单据的「仓库」下拉统一取自
 * {@code GET /wms/warehouse/list-all}（见前端 {@code api/options.ts:getWarehouses()}），
 * 即读的是 {@code wms_warehouse}；而新增仓库的入口是仓库规划页（写 {@code erp_warehouse}）。
 * 两者此前没有任何同步机制 ⇒ 在规划页新建的仓库不会出现在业务下拉里。
 * 本事件用于把 ERP 侧的主数据变更同步到 WMS 扩展表。</p>
 *
 * <p><b>id 对齐约定：</b>同步时令 {@code wms_warehouse.id = erp_warehouse.id}
 * 且 {@code warehouse_id} 同值 —— 现有种子数据（WH001~WH003）已是该口径，
 * 保持对齐可避免业务表里存的仓库 id 出现两套体系。</p>
 */
public class WarehouseChangedEvent {

    public enum Action {
        /** 新建 */
        CREATED,
        /** 基本信息（编号/名称）变更 */
        UPDATED,
        /** 启用/停用切换 */
        STATUS_CHANGED,
        /** 删除 */
        DELETED
    }

    private final Action action;
    private final Long warehouseId;
    private final String warehouseCode;
    private final String warehouseName;
    private final Integer status;
    private final Long tenantId;

    public WarehouseChangedEvent(Action action, Long warehouseId, String warehouseCode,
                                 String warehouseName, Integer status, Long tenantId) {
        this.action = action;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.status = status;
        this.tenantId = tenantId;
    }

    public Action getAction() { return action; }

    public Long getWarehouseId() { return warehouseId; }

    public String getWarehouseCode() { return warehouseCode; }

    public String getWarehouseName() { return warehouseName; }

    public Integer getStatus() { return status; }

    public Long getTenantId() { return tenantId; }
}
