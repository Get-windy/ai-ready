package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductBarcode;
import cn.aiedge.erp.stock.mapper.ProductBarcodeMapper;
import cn.aiedge.erp.stock.service.ProductBarcodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class ProductBarcodeServiceImpl extends ServiceImpl<ProductBarcodeMapper, ProductBarcode>
        implements ProductBarcodeService {

    @Override
    public List<ProductBarcode> getByProductId(Long productId) {
        return lambdaQuery()
                .eq(ProductBarcode::getProductId, productId)
                .eq(ProductBarcode::getDeleted, 0)
                .list();
    }
}
