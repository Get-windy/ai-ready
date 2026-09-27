package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;

import java.util.List;
import java.util.Map;

public interface MallProductService {

    /**
     * 商品列表。
     *
     * @param tagCode 商品标签编码（{@code erp_product.mall_tags} 逗号分隔的槽位码，
     *                如 {@code TAG_1}）——用于**分类页最顶部的商品标签 Tab**；
     *                传空表示不过滤。命中的是"包含"关系，不是相等。
     */
    PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword, String tagCode);

    ProductDetailDTO getProductDetail(Long id);

    List<Map<String, Object>> getCategories();

    List<ProductListDTO> getRecommendations();

    List<ProductListDTO> getHotProducts();

    List<Map<String, Object>> getBanners();
}
