package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.mapper.ProductUnitMapper;
import cn.aiedge.erp.stock.service.ProductUnitService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class ProductUnitServiceImpl extends ServiceImpl<ProductUnitMapper, ProductUnit>
        implements ProductUnitService {

    @Override
    public List<ProductUnit> getByProductId(Long productId) {
        return lambdaQuery()
                .eq(ProductUnit::getProductId, productId)
                .eq(ProductUnit::getDeleted, 0)
                .orderByAsc(ProductUnit::getSortOrder)
                .list();
    }
}
