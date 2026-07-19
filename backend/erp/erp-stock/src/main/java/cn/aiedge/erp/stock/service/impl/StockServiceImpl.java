package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.controller.initial.InitialStockDTO;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存管理ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Autowired
    private StockMapper stockMapper;

    /**
     * 默认库存预警阈值（从配置文件读取）
     */
    @Value("${stock.alert.low-stock-threshold:10}")
    private Integer defaultLowStockThreshold;

    @Override
    public Stock getStockDetail(Long productId, Long warehouseId) {
        QueryWrapper<Stock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId)
                .eq("warehouse_id", warehouseId)
                .eq("deleted", 0);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean increaseStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null) {
            // 如果库存记录不存在，创建新的库存记录
            stock = new Stock();
            stock.setProductId(productId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(quantity);
            stock.setAvailableQuantity(quantity);
            stock.setFrozenQuantity(BigDecimal.ZERO);
            return this.save(stock);
        } else {
            // 更新现有库存
            BigDecimal newQuantity = stock.getQuantity().add(quantity);
            BigDecimal newAvailableQuantity = stock.getAvailableQuantity().add(quantity);
            stock.setQuantity(newQuantity);
            stock.setAvailableQuantity(newAvailableQuantity);
            return this.updateById(stock);
        }
    }

    @Override
    public boolean decreaseStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null || stock.getAvailableQuantity().compareTo(quantity) < 0) {
            return false; // 库存不足
        }

        BigDecimal newQuantity = stock.getQuantity().subtract(quantity);
        BigDecimal newAvailableQuantity = stock.getAvailableQuantity().subtract(quantity);
        stock.setQuantity(newQuantity);
        stock.setAvailableQuantity(newAvailableQuantity);
        return this.updateById(stock);
    }

    @Override
    public boolean freezeStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null || stock.getAvailableQuantity().compareTo(quantity) < 0) {
            return false; // 可用库存不足
        }

        BigDecimal newAvailableQuantity = stock.getAvailableQuantity().subtract(quantity);
        BigDecimal newFrozenQuantity = stock.getFrozenQuantity().add(quantity);
        stock.setAvailableQuantity(newAvailableQuantity);
        stock.setFrozenQuantity(newFrozenQuantity);
        return this.updateById(stock);
    }

    @Override
    public boolean unfreezeStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null || stock.getFrozenQuantity().compareTo(quantity) < 0) {
            return false; // 冻结库存不足
        }

        BigDecimal newAvailableQuantity = stock.getAvailableQuantity().add(quantity);
        BigDecimal newFrozenQuantity = stock.getFrozenQuantity().subtract(quantity);
        stock.setAvailableQuantity(newAvailableQuantity);
        stock.setFrozenQuantity(newFrozenQuantity);
        return this.updateById(stock);
    }

    @Override
    public boolean checkStock(Long productId, Long warehouseId, java.math.BigDecimal actualQuantity) {
        if (actualQuantity == null || actualQuantity.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null) {
            stock = new Stock();
            stock.setProductId(productId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(actualQuantity);
            stock.setAvailableQuantity(actualQuantity);
            stock.setFrozenQuantity(BigDecimal.ZERO);
            return this.save(stock);
        } else {
            stock.setQuantity(actualQuantity);
            stock.setAvailableQuantity(actualQuantity.subtract(stock.getFrozenQuantity()));
            return this.updateById(stock);
        }
    }

    @Override
    public List<Stock> checkStockAlert() {
        // 使用默认配置的预警阈值
        Integer alertThreshold = defaultLowStockThreshold;

        // 查询库存数量低于预警阈值的商品
        QueryWrapper<Stock> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("available_quantity", alertThreshold)
                .eq("deleted", 0)
                .orderByAsc("available_quantity");

        return this.list(queryWrapper);
    }

    @Override
    public Stock getStockByProductId(Long productId) {
        QueryWrapper<Stock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId)
                .eq("deleted", 0)
                .orderByDesc("create_time")
                .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public void saveInitialStock(List<InitialStockDTO> initialStockList) {
        // 清除现有期初库存数据
        QueryWrapper<Stock> clearWrapper = new QueryWrapper<>();
        clearWrapper.eq("is_initial", 1);
        this.remove(clearWrapper);

        // 批量保存期初库存数据
        for (InitialStockDTO dto : initialStockList) {
            Stock stock = new Stock();
            stock.setProductCode(dto.getProductCode());
            stock.setProductName(dto.getProductName());
            stock.setWarehouseName(dto.getWarehouseName());
            stock.setQuantity(dto.getQuantity());
            stock.setUnitPrice(dto.getUnitPrice());
            stock.setIsInitial(1); // 标记为期初库存

            // 设置其他必要字段的默认值
            stock.setAvailableQuantity(dto.getQuantity()); // 期初数量全部为可用数量
            stock.setFrozenQuantity(BigDecimal.ZERO); // 期初无冻结库存
            stock.setDeleted(0); // 未删除状态
            stock.setTenantId(0L); // 租户ID

            this.save(stock);
        }
    }

    @Override
    public void updateInitialStock(InitialStockDTO initialStockDTO) {
        // 更新指定的期初库存数据
        Stock existingStock = this.getById(initialStockDTO.getId());
        if (existingStock != null && existingStock.getIsInitial() != null && existingStock.getIsInitial() == 1) {
            existingStock.setProductCode(initialStockDTO.getProductCode());
            existingStock.setProductName(initialStockDTO.getProductName());
            existingStock.setWarehouseName(initialStockDTO.getWarehouseName());
            existingStock.setQuantity(initialStockDTO.getQuantity());
            existingStock.setUnitPrice(initialStockDTO.getUnitPrice());

            // 同步更新可用数量
            existingStock.setAvailableQuantity(initialStockDTO.getQuantity());

            this.updateById(existingStock);
        }
    }
}