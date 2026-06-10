package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductAttributeDef;
import cn.aiedge.erp.stock.entity.ProductAttributeOption;
import cn.aiedge.erp.stock.mapper.ProductAttributeDefMapper;
import cn.aiedge.erp.stock.mapper.ProductAttributeOptionMapper;
import cn.aiedge.erp.stock.service.ProductAttributeDefService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ProductAttributeDefServiceImpl extends ServiceImpl<ProductAttributeDefMapper, ProductAttributeDef>
        implements ProductAttributeDefService {

    private final ProductAttributeOptionMapper optionMapper;

    @Override
    public List<ProductAttributeOption> getOptions(Long attrDefId) {
        return optionMapper.selectList(new QueryWrapper<ProductAttributeOption>()
                .eq("attr_def_id", attrDefId)
                .eq("deleted", 0)
                .orderByAsc("sort_order"));
    }

    @Override
    public boolean saveOption(ProductAttributeOption option) {
        return optionMapper.insert(option) > 0;
    }

    @Override
    public boolean updateOption(ProductAttributeOption option) {
        return optionMapper.updateById(option) > 0;
    }

    @Override
    public boolean removeOption(Long id) {
        return optionMapper.deleteById(id) > 0;
    }
}
