package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductAttributeValue;
import cn.aiedge.erp.stock.mapper.ProductAttributeValueMapper;
import cn.aiedge.erp.stock.service.ProductAttributeValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductAttributeValueServiceImpl extends ServiceImpl<ProductAttributeValueMapper, ProductAttributeValue>
        implements ProductAttributeValueService {

    @Override
    public List<ProductAttributeValue> getByProductId(Long productId) {
        return baseMapper.selectWithAttrDef(new QueryWrapper<ProductAttributeValue>()
                .eq("pav.product_id", productId)
                .eq("pav.deleted", 0)
                .orderByAsc("pav.sort_order"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(Long productId, List<ProductAttributeValue> values) {
        // 删除旧属性值
        baseMapper.delete(new QueryWrapper<ProductAttributeValue>()
                .eq("product_id", productId));
        // 批量插入新值
        if (values.isEmpty()) return true;
        values.forEach(v -> {
            v.setId(null);
            v.setProductId(productId);
        });
        return saveBatch(values);
    }
}
