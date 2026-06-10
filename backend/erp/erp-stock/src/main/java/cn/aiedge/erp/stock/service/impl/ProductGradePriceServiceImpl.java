package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductGradePrice;
import cn.aiedge.erp.stock.mapper.ProductGradePriceMapper;
import cn.aiedge.erp.stock.service.ProductGradePriceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品等级价格Service实现
 */
@Service
@RequiredArgsConstructor
public class ProductGradePriceServiceImpl extends ServiceImpl<ProductGradePriceMapper, ProductGradePrice>
        implements ProductGradePriceService {

    private final ProductGradePriceMapper gradePriceMapper;

    @Override
    public List<ProductGradePrice> getByProductId(Long productId) {
        return gradePriceMapper.selectByProductIdWithGrade(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(Long productId, List<ProductGradePrice> priceList) {
        // 删除旧价格
        remove(new LambdaQueryWrapper<ProductGradePrice>()
                .eq(ProductGradePrice::getProductId, productId));
        // 批量插入新价格
        if (priceList != null && !priceList.isEmpty()) {
            for (ProductGradePrice price : priceList) {
                price.setId(null);
                price.setProductId(productId);
                if (price.getIsActive() == null) price.setIsActive(1);
            }
            saveBatch(priceList);
        }
        return true;
    }
}
