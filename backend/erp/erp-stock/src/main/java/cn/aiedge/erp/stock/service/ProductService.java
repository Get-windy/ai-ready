package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Product;
import com.baomidou.mybatisplus.core.metadata.IPage;
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

    /**
     * 分页查询产品(含分类、等级名称)
     */
    IPage<Product> getProductPage(Long categoryId, String keyword, String status,
                                   Integer pageNum, Integer pageSize);

    /**
     * 获取产品详情(含等级价格)
     */
    Product getProductDetail(Long id);

    /**
     * 新增产品
     */
    boolean createProduct(Product product);

    /**
     * 更新产品
     */
    boolean updateProduct(Product product);

    /**
     * 更新产品状态
     */
    boolean updateProductStatus(Long id, String status);

    /**
     * 批量更新产品价格
     *
     * @param items 价格更新列表
     * @return 是否成功
     */
    boolean batchUpdatePrices(List<cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO.PriceItem> items);
}
