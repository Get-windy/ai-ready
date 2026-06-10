package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductAttributeValue;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductAttributeValueService extends IService<ProductAttributeValue> {
    List<ProductAttributeValue> getByProductId(Long productId);
    boolean batchSave(Long productId, List<ProductAttributeValue> values);
}
