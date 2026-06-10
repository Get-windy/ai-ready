package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductBarcode;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductBarcodeService extends IService<ProductBarcode> {
    List<ProductBarcode> getByProductId(Long productId);
}
