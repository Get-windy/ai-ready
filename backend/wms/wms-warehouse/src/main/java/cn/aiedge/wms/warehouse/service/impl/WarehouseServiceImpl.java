package cn.aiedge.wms.warehouse.service.impl;

import cn.aiedge.wms.entity.WmsWarehouse;
import cn.aiedge.wms.entity.WmsLocation;
import cn.aiedge.wms.warehouse.mapper.WmsWarehouseMapper;
import cn.aiedge.wms.warehouse.mapper.WmsLocationMapper;
import cn.aiedge.wms.warehouse.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWarehouse(WmsWarehouse warehouse) {
        return warehouseMapper.insert(warehouse) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWarehouse(WmsWarehouse warehouse) {
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Override
    public WmsWarehouse getWarehouseById(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Override
    public Page<WmsWarehouse> pageWarehouse(Page<WmsWarehouse> page, WmsWarehouse query) {
        LambdaQueryWrapper<WmsWarehouse> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsWarehouse::getId, query.getId());
            }
            if (query.getWarehouseCode() != null) {
                wrapper.like(WmsWarehouse::getWarehouseCode, query.getWarehouseCode());
            }
            if (query.getWarehouseName() != null) {
                wrapper.like(WmsWarehouse::getWarehouseName, query.getWarehouseName());
            }
            if (query.getWarehouseType() != null) {
                wrapper.eq(WmsWarehouse::getWarehouseType, query.getWarehouseType());
            }
            if (query.getIsWmsEnabled() != null) {
                wrapper.eq(WmsWarehouse::getIsWmsEnabled, query.getIsWmsEnabled());
            }
        }
        wrapper.orderByDesc(WmsWarehouse::getId);
        return warehouseMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeWarehouse(Long id) {
        return warehouseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveLocation(WmsLocation location) {
        return locationMapper.insert(location) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLocation(WmsLocation location) {
        return locationMapper.updateById(location) > 0;
    }

    @Override
    public WmsLocation getLocationById(Long id) {
        return locationMapper.selectById(id);
    }

    @Override
    public Page<WmsLocation> pageLocation(Page<WmsLocation> page, WmsLocation query) {
        LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsLocation::getId, query.getId());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsLocation::getWarehouseId, query.getWarehouseId());
            }
            if (query.getLocationCode() != null) {
                wrapper.like(WmsLocation::getLocationCode, query.getLocationCode());
            }
            if (query.getLocationName() != null) {
                wrapper.like(WmsLocation::getLocationName, query.getLocationName());
            }
            if (query.getLocationType() != null) {
                wrapper.eq(WmsLocation::getLocationType, query.getLocationType());
            }
            if (query.getLocationLevel() != null) {
                wrapper.eq(WmsLocation::getLocationLevel, query.getLocationLevel());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsLocation::getStatus, query.getStatus());
            }
            if (query.getIsPickable() != null) {
                wrapper.eq(WmsLocation::getIsPickable, query.getIsPickable());
            }
            if (query.getIsReceivable() != null) {
                wrapper.eq(WmsLocation::getIsReceivable, query.getIsReceivable());
            }
        }
        wrapper.orderByDesc(WmsLocation::getId);
        return locationMapper.selectPage(page, wrapper);
    }

    @Override
    public List<WmsLocation> listByWarehouseId(Long warehouseId) {
        LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsLocation::getWarehouseId, warehouseId);
        wrapper.orderByAsc(WmsLocation::getSortOrder);
        return locationMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeLocation(Long id) {
        return locationMapper.deleteById(id) > 0;
    }

    @Override
    public List<WmsLocation> recommendLocations(Long warehouseId, Long productId, BigDecimal quantity) {
        // 推荐策略：查询仓库下空闲的、可拣货的存储位，按已用容量升序排列
        LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsLocation::getWarehouseId, warehouseId);
        wrapper.eq(WmsLocation::getStatus, 1); // 空闲
        wrapper.eq(WmsLocation::getLocationType, 1); // 存储位
        wrapper.eq(WmsLocation::getIsPickable, 1); // 可拣货
        wrapper.orderByAsc(WmsLocation::getUsedCapacity);
        return locationMapper.selectList(wrapper);
    }
}
