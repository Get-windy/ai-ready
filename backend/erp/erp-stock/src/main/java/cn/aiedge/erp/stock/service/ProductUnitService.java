package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductUnit;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductUnitService extends IService<ProductUnit> {
    List<ProductUnit> getByProductId(Long productId);
}
