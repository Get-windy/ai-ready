package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Stock;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 库存管理Service接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface StockService extends IService<Stock> {

    /**
     * 查询库存详情
     * 
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @return 库存详情
     */
    Stock getStockDetail(Long productId, Long warehouseId);

    /**
     * 库存增加
     * 
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @param quantity 增加数量
     * @return 操作结果
     */
    boolean increaseStock(Long productId, Long warehouseId, java.math.BigDecimal quantity);

    /**
     * 库存减少
     * 
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @param quantity 减少数量
     * @return 操作结果
     */
    boolean decreaseStock(Long productId, Long warehouseId, java.math.BigDecimal quantity);

    /**
     * 库存冻结
     * 
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @param quantity 冻结数量
     * @return 操作结果
     */
    boolean freezeStock(Long productId, Long warehouseId, java.math.BigDecimal quantity);

    /**
     * 库存解冻
     * 
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @param quantity 解冻数量
     * @return 操作结果
     */
    boolean unfreezeStock(Long productId, Long warehouseId, java.math.BigDecimal quantity);

    /**
     * 库存盘点
     * 
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @param actualQuantity 实际数量
     * @return 操作结果
     */
    boolean盘点Stock(Long productId, Long warehouseId, java.math.BigDecimal actualQuantity);

    /**
     * 库存预警检查
     * 
     * @return 预警列表
     */
    java.util.List<Stock> checkStockAlert();
}
