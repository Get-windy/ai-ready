package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.ProductLocationQuery;
import cn.aiedge.erp.stock.dto.ProductLocationSetDTO;
import cn.aiedge.erp.stock.dto.ProductLocationVO;
import cn.aiedge.erp.stock.entity.ProductLocation;
import cn.aiedge.erp.stock.mapper.ProductLocationMapper;
import cn.aiedge.erp.stock.service.ProductLocationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品货位设置 ServiceImpl
 * <p>
 * 货位主数据为全局唯一口径 wms_location（本类只读其 location_code 做校验与快照），
 * 严禁另建货位表；仓库口径统一 erp_warehouse.id。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ProductLocationServiceImpl extends ServiceImpl<ProductLocationMapper, ProductLocation>
        implements ProductLocationService {

    @Override
    public IPage<ProductLocationVO> page(ProductLocationQuery query) {
        normalize(query);
        Page<ProductLocationVO> page = new Page<>(
                query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1,
                query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20);
        return baseMapper.selectProductLocationPage(page,
                MyBatisPlusConfig.getCurrentTenantIdValue(), query.getWarehouseId(), query.getCategoryId(),
                query.getKeyword(), query.getBrand(), query.getLocationCode(),
                legalBarcodeStatus(query.getHasBarcodeStatus()), legalShelfStatus(query.getShelfStatus()),
                query.getShowStop(), Boolean.TRUE.equals(query.getOnlyUnsettedGoods()),
                Boolean.TRUE.equals(query.getOnlyStockGoods()));
    }

    @Override
    public List<ProductLocationVO> list(ProductLocationQuery query) {
        normalize(query);
        return baseMapper.selectProductLocationList(
                MyBatisPlusConfig.getCurrentTenantIdValue(), query.getWarehouseId(), query.getCategoryId(),
                query.getKeyword(), query.getBrand(), query.getLocationCode(),
                legalBarcodeStatus(query.getHasBarcodeStatus()), legalShelfStatus(query.getShelfStatus()),
                query.getShowStop(), Boolean.TRUE.equals(query.getOnlyUnsettedGoods()),
                Boolean.TRUE.equals(query.getOnlyStockGoods()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setLocation(ProductLocationSetDTO dto) {
        requireWarehouse(dto.getWarehouseId());
        if (dto.getLocationId() == null) {
            throw new IllegalArgumentException("请选择货位");
        }
        List<Long> productIds = distinctProductIds(dto.getProductIds());
        // 货位必须真实存在且属于所选仓库（跨模块只读 wms_location，不复制货位属性）
        String locationCode = baseMapper.selectLocationCode(dto.getLocationId(), dto.getWarehouseId());
        if (locationCode == null) {
            throw new IllegalArgumentException("货位不存在或不属于所选仓库");
        }

        Map<Long, ProductLocation> existMap = new HashMap<>();
        for (ProductLocation exist : this.list(new LambdaQueryWrapper<ProductLocation>()
                .eq(ProductLocation::getWarehouseId, dto.getWarehouseId())
                .in(ProductLocation::getProductId, productIds))) {
            existMap.put(exist.getProductId(), exist);
        }

        List<ProductLocation> toSave = new ArrayList<>();
        for (Long productId : productIds) {
            ProductLocation bind = existMap.get(productId);
            if (bind == null) {
                bind = new ProductLocation()
                        .setWarehouseId(dto.getWarehouseId())
                        .setProductId(productId);
            }
            bind.setLocationId(dto.getLocationId())
                    .setLocationCode(locationCode)
                    .setRemark(trimToNull(dto.getRemark()));
            toSave.add(bind);
        }
        this.saveOrUpdateBatch(toSave);
        log.info("商品推荐货位设置: warehouseId={}, locationId={}, productCount={}",
                dto.getWarehouseId(), dto.getLocationId(), toSave.size());
        return toSave.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeLocation(ProductLocationSetDTO dto) {
        requireWarehouse(dto.getWarehouseId());
        List<Long> productIds = distinctProductIds(dto.getProductIds());
        boolean ok = this.remove(new LambdaQueryWrapper<ProductLocation>()
                .eq(ProductLocation::getWarehouseId, dto.getWarehouseId())
                .in(ProductLocation::getProductId, productIds));
        log.info("商品推荐货位移除: warehouseId={}, productCount={}", dto.getWarehouseId(), productIds.size());
        return ok ? productIds.size() : 0;
    }

    private void requireWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("请选择仓库");
        }
    }

    /** 空串归一为 null —— 否则 LIKE '%%' 会把「未设置货位」的行过滤掉 */
    private void normalize(ProductLocationQuery query) {
        // 仓库未选 = 全部仓库（不按仓库过滤，绑定按商品聚合）
        query.setWarehouseId(query.getWarehouseId() != null && query.getWarehouseId() > 0 ? query.getWarehouseId() : null);
        query.setKeyword(trimToNull(query.getKeyword()));
        query.setBrand(trimToNull(query.getBrand()));
        query.setLocationCode(trimToNull(query.getLocationCode()));
        query.setCategoryId(query.getCategoryId() != null && query.getCategoryId() > 0 ? query.getCategoryId() : null);
        // -1 表示「全部」，按未筛选处理
        query.setHasBarcodeStatus(legalBarcodeStatus(query.getHasBarcodeStatus()));
        query.setShelfStatus(legalShelfStatus(query.getShelfStatus()));
        query.setShowStop(legalBarcodeStatus(query.getShowStop()));
    }

    private Integer legalBarcodeStatus(Integer value) {
        return value == null || value < 0 ? null : value;
    }

    private Integer legalShelfStatus(Integer value) {
        return value == null || value < 0 ? null : value;
    }

    private List<Long> distinctProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个商品");
        }
        Map<Long, Boolean> distinct = new LinkedHashMap<>();
        for (Long id : productIds) {
            if (id != null) {
                distinct.put(id, Boolean.TRUE);
            }
        }
        if (distinct.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个商品");
        }
        return new ArrayList<>(distinct.keySet());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
