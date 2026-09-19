package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.controller.initial.InitialStockDTO;
import cn.aiedge.erp.stock.entity.Stock;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
     * 库存入库回写（记账）：增加库存并按核定成本单价做移动加权平均。
     * 入参 stock 承载本次入库信息：productId/warehouseId/quantity/unitPrice(核定成本)/batchNo/
     * productionDate/validityDate/productCode/productName/unit。
     * 业界实践：盘盈/溢余入库按核定成本单价入账；若同批次已有库存，采用移动加权平均更新单价。
     *
     * @param stock 本次入库移动载体
     * @return 操作结果
     */
    boolean recordStockIn(Stock stock);

    /**
     * 库存出库回写（记账）：按批次扣减库存，可用量不足则失败。
     * 入参 stock 承载：productId/warehouseId/quantity/batchNo。
     *
     * @param stock 本次出库移动载体
     * @return 操作结果
     */
    boolean recordStockOut(Stock stock);

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
    boolean checkStock(Long productId, Long warehouseId, java.math.BigDecimal actualQuantity);

    /**
     * 库存预警检查
     *
     * @return 预警列表
     */
    List<Stock> checkStockAlert();

    /**
     * 根据产品ID查询库存汇总
     *
     * @param productId 产品ID
     * @return 库存汇总
     */
    Stock getStockByProductId(Long productId);

    /**
     * 保存期初库存数据
     *
     * @param initialStockList 期初库存数据列表
     */
    void saveInitialStock(List<InitialStockDTO> initialStockList);

    /**
     * 更新期初库存数据
     *
     * @param initialStockDTO 期初库存数据
     */
    void updateInitialStock(InitialStockDTO initialStockDTO);

    /**
     * 删除单条期初库存记录
     * <p>
     * ⚠️ 只允许删除 {@code is_initial = 1} 的期初行：本页是「erp_stock 中期初行的唯一维护入口」，
     * 日常库存行（is_initial = 0）不属于本页管辖，误删会破坏日常库存台账。
     *
     * @param id 期初库存记录ID
     * @return true=删除成功；false=记录不存在或不是期初行
     */
    boolean deleteInitialStock(Long id);
}
