package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public List<Product> getProductList() {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }
}
