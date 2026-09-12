package cn.aiedge.wms.warehouse.service.impl;

import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.mapper.WarehouseMapper;
import cn.aiedge.wms.entity.WmsWarehouse;
import cn.aiedge.wms.entity.WmsLocation;
import cn.aiedge.wms.warehouse.dto.LocationGenerateDTO;
import cn.aiedge.wms.warehouse.dto.LocationQuery;
import cn.aiedge.wms.warehouse.mapper.WmsWarehouseMapper;
import cn.aiedge.wms.warehouse.mapper.WmsLocationMapper;
import cn.aiedge.wms.warehouse.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;
    private final WarehouseMapper erpWarehouseMapper;

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

    // ══════════════════════════════════════════════════════════════════════
    // 仓库规划 → 2.货位（对标 ql361）
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public Page<WmsLocation> pageLocationPlan(LocationQuery query) {
        Page<WmsLocation> page = new Page<>(
                query == null || query.getPageNum() < 1 ? 1 : query.getPageNum(),
                query == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        return locationMapper.selectPage(page, buildLocationWrapper(query));
    }

    @Override
    public List<WmsLocation> listLocationsForExport(LocationQuery query) {
        return locationMapper.selectList(buildLocationWrapper(query));
    }

    private LambdaQueryWrapper<WmsLocation> buildLocationWrapper(LocationQuery query) {
        LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getWarehouseId() != null && query.getWarehouseId() != 0L) {
                wrapper.eq(WmsLocation::getWarehouseId, query.getWarehouseId());
            } else if (StringUtils.hasText(query.getWarehouseKeyword())) {
                wrapper.like(WmsLocation::getWarehouseName, query.getWarehouseKeyword().trim());
            }
            if (StringUtils.hasText(query.getLocationCode())) {
                wrapper.like(WmsLocation::getLocationCode, query.getLocationCode().trim());
            }
            // 对标默认：不勾选「显示停用」时只展示启用货位
            if (!Boolean.TRUE.equals(query.getShowDisabled())) {
                wrapper.eq(WmsLocation::getIsEnabled, 1);
            }
        }
        wrapper.orderByDesc(WmsLocation::getId);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateLocations(LocationGenerateDTO dto) {
        if (dto == null || dto.getWarehouseId() == null) {
            throw new IllegalArgumentException("请选择所属仓库");
        }
        Warehouse erpWarehouse = erpWarehouseMapper.selectById(dto.getWarehouseId());
        if (erpWarehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        int channelCount = positive(dto.getChannelCount());
        int shelfCount = positive(dto.getShelfCount());
        int layerCount = positive(dto.getLayerCount());
        int columnCount = positive(dto.getColumnCount());
        String channelNo = StringUtils.hasText(dto.getChannelNo()) ? dto.getChannelNo().trim() : "A";
        String shelfNo = StringUtils.hasText(dto.getShelfNo()) ? dto.getShelfNo().trim() : "1";
        String columnNo = StringUtils.hasText(dto.getColumnNo()) ? dto.getColumnNo().trim() : "1";

        // 已存在的货位编号（同仓库内去重，避免重复生成报唯一冲突）
        Set<String> exists = locationMapper.selectList(new LambdaQueryWrapper<WmsLocation>()
                        .eq(WmsLocation::getWarehouseId, dto.getWarehouseId())
                        .select(WmsLocation::getLocationCode))
                .stream().map(WmsLocation::getLocationCode).filter(Objects::nonNull).collect(Collectors.toSet());

        int created = 0;
        for (int ci = 0; ci < channelCount; ci++) {
            String channel = incrementAlpha(channelNo, ci);
            for (int si = 0; si < shelfCount; si++) {
                String shelf = incrementNumeric(shelfNo, si);
                String shelfTrim = shelf.replaceFirst("^0+(?=\\d)", "");
                for (int li = 1; li <= layerCount; li++) {
                    for (int mi = 0; mi < columnCount; mi++) {
                        String column = incrementNumeric(columnNo, mi);
                        String code = channel + shelfTrim + "-" + li + column;
                        if (exists.contains(code)) {
                            continue;
                        }
                        WmsLocation location = new WmsLocation();
                        location.setWarehouseId(dto.getWarehouseId());
                        location.setWarehouseName(erpWarehouse.getWarehouseName());
                        location.setLocationCode(code);
                        location.setLocationName(code);
                        location.setLocationType(1);
                        location.setLocationLevel(4);
                        location.setStatus(1);
                        location.setIsPickable(1);
                        location.setIsReceivable(1);
                        location.setIsEnabled(1);
                        location.setIsBuiltin(0);
                        location.setSortOrder(created);
                        location.setRemark(dto.getRemark());
                        locationMapper.insert(location);
                        exists.add(code);
                        created++;
                    }
                }
            }
        }
        return created;
    }

    private int positive(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    /** 字母递增：A→B…Z→AA */
    private String incrementAlpha(String base, int step) {
        if (!StringUtils.hasText(base)) {
            return String.valueOf((char) ('A' + step));
        }
        char[] chars = base.toCharArray();
        int carry = step;
        for (int i = chars.length - 1; i >= 0 && carry > 0; i--) {
            int idx = (Character.toUpperCase(chars[i]) - 'A') + carry;
            chars[i] = (char) ('A' + (idx % 26));
            carry = idx / 26;
        }
        return new String(chars) + (carry > 0 ? "A".repeat(carry) : "");
    }

    /** 数字递增并保持原有位数（01→02） */
    private String incrementNumeric(String base, int step) {
        if (!StringUtils.hasText(base)) {
            return String.valueOf(step + 1);
        }
        try {
            int value = Integer.parseInt(base.trim());
            int width = base.trim().length();
            return String.format("%0" + width + "d", value + step);
        } catch (NumberFormatException e) {
            return base + step;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLocationEnabled(Long id, Integer isEnabled) {
        WmsLocation exist = locationMapper.selectById(id);
        if (exist == null) {
            throw new IllegalArgumentException("货位不存在");
        }
        WmsLocation update = new WmsLocation();
        update.setId(id);
        update.setIsEnabled(isEnabled == null ? 1 : isEnabled);
        return locationMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemoveLocations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<WmsLocation> targets = locationMapper.selectBatchIds(ids);
        List<Long> removable = targets.stream()
                .filter(l -> l.getIsBuiltin() == null || l.getIsBuiltin() != 1)
                .map(WmsLocation::getId)
                .collect(Collectors.toList());
        if (removable.isEmpty()) {
            return 0;
        }
        return locationMapper.deleteBatchIds(removable);
    }
}
