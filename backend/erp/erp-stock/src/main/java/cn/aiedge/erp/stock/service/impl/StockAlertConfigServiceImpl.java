package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockAlertBatchItemDTO;
import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.StockAlertConfig;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.mapper.StockAlertConfigMapper;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.StockAlertConfigService;
import cn.aiedge.erp.stock.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertConfigServiceImpl extends ServiceImpl<StockAlertConfigMapper, StockAlertConfig> implements StockAlertConfigService {

    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final SysConfigService sysConfigService;

    /** 比较口径配置key，存在 sys_project_config（group=stock_alert） */
    private static final String COMPARISON_KEY = "stock.alert.comparison";
    private static final String COMPARISON_DEFAULT = "BOOK_WAIT_DELIVER";
    private static final String COMPARISON_GROUP = "stock_alert";

    @Override
    public StockAlertConfig getByProductAndWarehouse(Long productId, Long warehouseId) {
        return baseMapper.selectByProductAndWarehouse(productId, warehouseId);
    }

    @Override
    public Page<StockAlertConfig> pageList(String keyword, Long warehouseId, Boolean active, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockAlertConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockAlertConfig::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(StockAlertConfig::getProductCode, keyword)
                    .or().like(StockAlertConfig::getProductName, keyword));
        }
        if (warehouseId != null) {
            wrapper.eq(StockAlertConfig::getWarehouseId, warehouseId);
        }
        if (active != null) {
            wrapper.eq(StockAlertConfig::getActive, active);
        }
        wrapper.orderByDesc(StockAlertConfig::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<StockAlertConfig> listByWarehouse(Long warehouseId) {
        return baseMapper.selectByWarehouse(warehouseId);
    }

    @Override
    public List<StockAlertConfig> listAllActive() {
        return baseMapper.selectAllActive();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAlertConfig createConfig(StockAlertConfig config) {
        config.setActive(true);
        save(config);
        return getById(config.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockAlertConfig updateConfig(Long configId, StockAlertConfig config) {
        StockAlertConfig existing = getById(configId);
        if (existing == null) {
            throw BusinessException.notFound("预警配置不存在");
        }
        config.setId(configId);
        updateById(config);
        return getById(configId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateConfig(Long configId) {
        StockAlertConfig config = getById(configId);
        if (config == null) {
            throw BusinessException.notFound("预警配置不存在");
        }
        config.setActive(true);
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateConfig(Long configId) {
        StockAlertConfig config = getById(configId);
        if (config == null) {
            throw BusinessException.notFound("预警配置不存在");
        }
        config.setActive(false);
        updateById(config);
    }

    @Override
    public List<StockAlertConfig> checkAlerts() {
        List<StockAlertConfig> activeConfigs = listAllActive();
        List<StockAlertConfig> alertConfigs = new ArrayList<>();
        for (StockAlertConfig config : activeConfigs) {
            log.info("检查库存预警: 产品={}, 仓库={}", config.getProductCode(), config.getWarehouseName());
            alertConfigs.add(config);
        }
        return alertConfigs;
    }

    @Override
    public Integer countActive(Long tenantId) {
        return baseMapper.countActive(tenantId);
    }

    // ═══════════════ 预警设置页（库存预警固定值设置） ═══════════════

    @Override
    public IPage<StockAlertQueryVO> configItemsPage(Long warehouseId, String keyword, String brand,
                                                    Long categoryId, int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return baseMapper.configItemsPage(new Page<>(pageNum, pageSize), tenantId, warehouseId, keyword, brand, categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetThreshold(Long warehouseId, List<Long> productIds, BigDecimal minStock, BigDecimal maxStock) {
        if (warehouseId == null) {
            throw BusinessException.badRequest("请先选择仓库");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw BusinessException.badRequest("请先选择商品");
        }
        Warehouse warehouse = warehouseService.getById(warehouseId);
        if (warehouse == null) {
            throw BusinessException.notFound("仓库不存在");
        }
        for (Long productId : productIds) {
            upsertThreshold(productId, warehouseId, warehouse.getWarehouseName(), minStock, maxStock, true);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetItems(List<StockAlertBatchItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw BusinessException.badRequest("保存数据不能为空");
        }
        Map<Long, String> warehouseNameCache = new HashMap<>();
        for (StockAlertBatchItemDTO item : items) {
            if (item.getProductId() == null) {
                continue;
            }
            if (item.getWarehouseId() == null) {
                throw BusinessException.badRequest("存在未选择仓库的商品，请重新选择仓库后再保存");
            }
            String warehouseName = warehouseNameCache.computeIfAbsent(item.getWarehouseId(), wid -> {
                Warehouse w = warehouseService.getById(wid);
                return w == null ? null : w.getWarehouseName();
            });
            upsertThreshold(item.getProductId(), item.getWarehouseId(), warehouseName,
                    item.getMinStock(), item.getMaxStock(),
                    item.getActive() == null ? Boolean.TRUE : item.getActive());
        }
    }

    private void upsertThreshold(Long productId, Long warehouseId, String warehouseName,
                                 BigDecimal minStock, BigDecimal maxStock, Boolean active) {
        Product product = productService.getById(productId);
        if (product == null) {
            return;
        }
        StockAlertConfig existing = lambdaQuery()
                .eq(StockAlertConfig::getProductId, productId)
                .eq(StockAlertConfig::getWarehouseId, warehouseId)
                .eq(StockAlertConfig::getDeleted, 0)
                .one();
        StockAlertConfig cfg;
        if (existing != null) {
            cfg = existing;
        } else {
            cfg = new StockAlertConfig();
            cfg.setProductId(productId);
            cfg.setProductCode(product.getProductCode());
            cfg.setProductName(product.getProductName());
            cfg.setWarehouseId(warehouseId);
            cfg.setWarehouseName(warehouseName);
            cfg.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
            cfg.setActive(Boolean.TRUE.equals(active));
        }
        cfg.setMinStock(minStock);
        cfg.setMaxStock(maxStock);
        cfg.setEnableLowStockAlert(true);
        cfg.setEnableOverStockAlert(true);
        saveOrUpdate(cfg);
    }

    @Override
    public Map<String, Object> getComparison() {
        String value = sysConfigService.getValue(COMPARISON_KEY, COMPARISON_DEFAULT);
        Map<String, Object> result = new HashMap<>();
        result.put("key", COMPARISON_KEY);
        result.put("value", value);
        result.put("label", comparisonLabel(value));
        return result;
    }

    @Override
    public void setComparison(String value) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest("比较口径不能为空");
        }
        sysConfigService.setValue(COMPARISON_KEY, value, "string", COMPARISON_GROUP, "库存预警比较口径");
    }

    private String comparisonLabel(String value) {
        switch (value) {
            case "BOOK_QTY":
                return "账面库存";
            case "BOOK_WAIT_DELIVER":
                return "账面库存-待发货";
            case "BOOK_WAIT_DELIVER_PURCHASE":
                return "账面库存-待发货-待收货";
            default:
                return value;
        }
    }
}
