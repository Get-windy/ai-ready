package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductAttributeDef;
import cn.aiedge.erp.stock.entity.ProductAttributeOption;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductAttributeDefService extends IService<ProductAttributeDef> {
    List<ProductAttributeOption> getOptions(Long attrDefId);
    boolean saveOption(ProductAttributeOption option);
    boolean updateOption(ProductAttributeOption option);
    boolean removeOption(Long id);
}
