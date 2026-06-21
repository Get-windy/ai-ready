package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductRecommend;
import cn.aiedge.erp.stock.mapper.ProductRecommendMapper;
import cn.aiedge.erp.stock.service.ProductRecommendService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 推荐商品ServiceImpl
 */
@Slf4j
@Service
public class ProductRecommendServiceImpl extends ServiceImpl<ProductRecommendMapper, ProductRecommend> implements ProductRecommendService {

    @Override
    public List<ProductRecommend> getByProductId(Long productId) {
        return baseMapper.selectByProductId(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(Long productId, List<ProductRecommend> recommends) {
        // 先删除该商品的所有推荐
        QueryWrapper<ProductRecommend> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        baseMapper.delete(wrapper);

        if (recommends == null || recommends.isEmpty()) {
            return true;
        }

        // 批量插入新的推荐
        for (int i = 0; i < recommends.size(); i++) {
            ProductRecommend r = recommends.get(i);
            r.setProductId(productId);
            r.setSortOrder(r.getSortOrder() != null ? r.getSortOrder() : (i + 1));
            r.setDeleted(0);
        }
        return saveBatch(recommends);
    }
}
