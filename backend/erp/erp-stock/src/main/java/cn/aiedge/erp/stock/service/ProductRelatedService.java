package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductRelated;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductRelatedService extends IService<ProductRelated> {
    List<ProductRelated> getByProductId(Long productId);
}
