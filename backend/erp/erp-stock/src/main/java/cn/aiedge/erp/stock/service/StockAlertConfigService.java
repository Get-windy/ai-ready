package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockAlertConfig;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockAlertConfigService extends IService<StockAlertConfig> {

    StockAlertConfig getByProductAndWarehouse(Long productId, Long warehouseId);

    Page<StockAlertConfig> pageList(String keyword, Long warehouseId, Boolean active, int pageNum, int pageSize);

    List<StockAlertConfig> listByWarehouse(Long warehouseId);

    List<StockAlertConfig> listAllActive();

    StockAlertConfig createConfig(StockAlertConfig config);

    StockAlertConfig updateConfig(Long configId, StockAlertConfig config);

    void activateConfig(Long configId);

    void deactivateConfig(Long configId);

    List<StockAlertConfig> checkAlerts();

    Integer countActive(Long tenantId);
}