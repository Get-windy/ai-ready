package cn.aiedge.erp.pricing.service.impl;

import cn.aiedge.erp.pricing.entity.ProductGradePrice;
import cn.aiedge.erp.pricing.mapper.ProductGradePriceMapper;
import cn.aiedge.erp.pricing.service.IProductGradePriceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductGradePriceServiceImpl extends ServiceImpl<ProductGradePriceMapper, ProductGradePrice> implements IProductGradePriceService {

    @Override
    public List<ProductGradePrice> getByProductId(Long productId) {
        return list(new LambdaQueryWrapper<ProductGradePrice>()
                .eq(ProductGradePrice::getProductId, productId)
                .eq(ProductGradePrice::getDeleted, 0)
                .orderByAsc(ProductGradePrice::getGradeCode));
    }

    @Override
    @Transactional
    public void batchSave(Long productId, List<ProductGradePrice> priceList) {
        // 删除该产品的所有等级价格
        remove(new LambdaQueryWrapper<ProductGradePrice>()
                .eq(ProductGradePrice::getProductId, productId));

        // 批量插入新价格
        if (priceList != null && !priceList.isEmpty()) {
            priceList.forEach(price -> {
                price.setProductId(productId);
                if (price.getIsActive() == null) price.setIsActive(1);
            });
            saveBatch(priceList);
        }
    }
}