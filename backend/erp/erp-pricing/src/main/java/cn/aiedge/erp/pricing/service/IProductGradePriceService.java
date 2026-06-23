package cn.aiedge.erp.pricing.service;

import cn.aiedge.erp.pricing.entity.ProductGradePrice;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface IProductGradePriceService extends IService<ProductGradePrice> {
    List<ProductGradePrice> getByProductId(Long productId);

    void batchSave(Long productId, List<ProductGradePrice> priceList);
}