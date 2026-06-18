package cn.aiedge.wms.warehouse.service;

import cn.aiedge.wms.entity.WmsWarehouse;
import cn.aiedge.wms.entity.WmsLocation;
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
}
