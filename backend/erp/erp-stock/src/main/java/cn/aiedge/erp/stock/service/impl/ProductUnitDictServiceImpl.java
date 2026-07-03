package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductUnitDict;
import cn.aiedge.erp.stock.mapper.ProductUnitDictMapper;
import cn.aiedge.erp.stock.service.ProductUnitDictService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品单位字典ServiceImpl
 */
@Slf4j
@Service
public class ProductUnitDictServiceImpl extends ServiceImpl<ProductUnitDictMapper, ProductUnitDict>
        implements ProductUnitDictService {

    @Override
    public IPage<ProductUnitDict> getPage(Long tenantId, String keyword, int pageNum, int pageSize) {
        Page<ProductUnitDict> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectPage(page, tenantId, keyword);
    }

    @Override
    public List<ProductUnitDict> getByTenantId(Long tenantId) {
        return baseMapper.selectByTenantId(tenantId);
    }
}
