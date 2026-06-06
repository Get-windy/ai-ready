package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Product;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 产品Service接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ProductService extends IService<Product> {

    /**
     * 获取产品列表
     */
    List<Product> getProductList();
}
