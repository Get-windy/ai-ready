package cn.aiedge.wms.warehouse.service;

import cn.aiedge.wms.entity.WmsWarehouse;
import cn.aiedge.wms.entity.WmsLocation;
import cn.aiedge.wms.warehouse.dto.LocationGenerateDTO;
import cn.aiedge.wms.warehouse.dto.LocationQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;

public interface WarehouseService {
    // 仓库
    boolean saveWarehouse(WmsWarehouse warehouse);
    boolean updateWarehouse(WmsWarehouse warehouse);
    WmsWarehouse getWarehouseById(Long id);
    Page<WmsWarehouse> pageWarehouse(Page<WmsWarehouse> page, WmsWarehouse query);
    boolean removeWarehouse(Long id);
    // 货位
    boolean saveLocation(WmsLocation location);
    boolean updateLocation(WmsLocation location);
    WmsLocation getLocationById(Long id);
    Page<WmsLocation> pageLocation(Page<WmsLocation> page, WmsLocation query);
    List<WmsLocation> listByWarehouseId(Long warehouseId);
    boolean removeLocation(Long id);
    // 上架策略推荐
    List<WmsLocation> recommendLocations(Long warehouseId, Long productId, BigDecimal quantity);

    // ── 仓库规划 → 2.货位（对标 ql361） ──
    /** 货位列表分页（仓库 / 货位编号 / 显示停用） */
    Page<WmsLocation> pageLocationPlan(LocationQuery query);

    /** 导出用货位全量列表 */
    List<WmsLocation> listLocationsForExport(LocationQuery query);

    /** 批量生成货位，返回生成条数 */
    int generateLocations(LocationGenerateDTO dto);

    /** 启用/停用货位 */
    boolean updateLocationEnabled(Long id, Integer isEnabled);

    /** 批量删除货位，返回删除条数 */
    int batchRemoveLocations(List<Long> ids);
}
