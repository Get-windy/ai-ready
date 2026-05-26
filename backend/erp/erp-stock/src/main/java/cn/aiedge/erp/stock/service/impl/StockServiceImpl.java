package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.StockService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存管理ServiceImpl
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

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

    /**
     * 库存预警检查
     * 
     * @return 预警列表 - 只返回库存数量低于预警阈值的商品
     */
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
}