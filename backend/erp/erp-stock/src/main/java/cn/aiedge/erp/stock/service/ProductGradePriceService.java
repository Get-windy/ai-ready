package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductGradePrice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 产品等级价格Service接口
 */
public interface ProductGradePriceService extends IService<ProductGradePrice> {

    /**
     * 获取某产品的等级价格列表
     */
    List<ProductGradePrice> getByProductId(Long productId);

    /**
     * 批量保存某产品的等级价格(全量覆盖)
     */
    boolean batchSave(Long productId, List<ProductGradePrice> priceList);
}
