package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockAlertBatchItemDTO;
import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import cn.aiedge.erp.stock.entity.StockAlertConfig;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    /** 预警设置：商品清单 + 仓库下的上下限配置分页查询 */
    IPage<StockAlertQueryVO> configItemsPage(Long warehouseId, String keyword, String brand, Long categoryId, int pageNum, int pageSize);

    /** 预警设置：批量设置上下限（对选中商品约定仓库统一赋值，upsert） */
    void batchSetThreshold(Long warehouseId, List<Long> productIds, BigDecimal minStock, BigDecimal maxStock);

    /** 预警设置：按行批量保存上下限（行内编辑保存，逐条 upsert） */
    void batchSetItems(List<StockAlertBatchItemDTO> items);

    /** 预警设置：获取比较口径配置 */
    Map<String, Object> getComparison();

    /** 预警设置：保存比较口径配置 */
    void setComparison(String value);
}