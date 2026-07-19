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
                                   String brand, String industryCategory,
                                   String createTimeStart, String createTimeEnd,
                                   Integer useCoupon, Integer isStandardProduct,
                                   String productType, Integer mallShelfStatus,
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

    /**
     * 导出产品列表(不含分页)
     *
     * @param categoryId 分类ID(可选)
     * @param keyword    关键字(可选)
     * @param status     状态(可选)
     * @param brand      品牌(可选)
     * @param industryCategory 行业类别(可选)
     * @param createTimeStart  创建日期起始(可选)
     * @param createTimeEnd    创建日期截止(可选)
     * @param useCoupon        使用优惠券(可选)
     * @param isStandardProduct 是否标品(可选)
     * @return 产品列表
     */
    List<Product> exportList(Long categoryId, String keyword, String status,
                              String brand, String industryCategory,
                              String createTimeStart, String createTimeEnd,
                              Integer useCoupon, Integer isStandardProduct);

    /**
     * 批量更新商品状态
     */
    boolean batchUpdateStatus(List<Long> ids, String status);

    /**
     * 批量删除商品
     */
    boolean batchDelete(List<Long> ids);

    /**
     * 获取不重复的行业类别列表
     */
    List<String> getDistinctIndustryCategories();

    /**
     * 获取不重复的品牌列表
     */
    List<String> getDistinctBrands();
}
