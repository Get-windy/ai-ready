package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductBrand;
import cn.aiedge.erp.stock.mapper.ProductBrandMapper;
import cn.aiedge.erp.stock.service.ProductBrandService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品品牌ServiceImpl
 */
@Slf4j
@Service
public class ProductBrandServiceImpl extends ServiceImpl<ProductBrandMapper, ProductBrand>
        implements ProductBrandService {

    @Override
    public IPage<ProductBrand> getPage(Long tenantId, String keyword, int pageNum, int pageSize) {
        Page<ProductBrand> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectPage(page, tenantId, keyword);
    }

    @Override
    public List<ProductBrand> getByTenantId(Long tenantId) {
        return baseMapper.selectByTenantId(tenantId);
    }
}
