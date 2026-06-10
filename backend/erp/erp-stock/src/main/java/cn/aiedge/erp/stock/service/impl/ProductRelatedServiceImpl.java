package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductRelated;
import cn.aiedge.erp.stock.mapper.ProductRelatedMapper;
import cn.aiedge.erp.stock.service.ProductRelatedService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class ProductRelatedServiceImpl extends ServiceImpl<ProductRelatedMapper, ProductRelated>
        implements ProductRelatedService {

    @Override
    public List<ProductRelated> getByProductId(Long productId) {
        return baseMapper.selectWithProduct(new QueryWrapper<ProductRelated>()
                .eq("pr.product_id", productId)
                .eq("pr.deleted", 0)
                .orderByAsc("pr.sort_order"));
    }
}
