package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.StockAlertConfig;
import cn.aiedge.erp.stock.mapper.StockAlertConfigMapper;
import cn.aiedge.erp.stock.service.StockAlertConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertConfigServiceImpl extends ServiceImpl<StockAlertConfigMapper, StockAlertConfig> implements StockAlertConfigService {

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
            throw new RuntimeException("预警配置不存在");
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
            throw new RuntimeException("预警配置不存在");
        }
        config.setActive(true);
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateConfig(Long configId) {
        StockAlertConfig config = getById(configId);
        if (config == null) {
            throw new RuntimeException("预警配置不存在");
        }
        config.setActive(false);
        updateById(config);
    }

    @Override
    public List<StockAlertConfig> checkAlerts() {
        List<StockAlertConfig> activeConfigs = listAllActive();
        List<StockAlertConfig> alertConfigs = new java.util.ArrayList<>();
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
}