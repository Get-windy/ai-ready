package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;

import java.util.List;
import java.util.Map;

public interface MallProductService {

    PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword);

    ProductDetailDTO getProductDetail(Long id);

    List<Map<String, Object>> getCategories();

    List<ProductListDTO> getRecommendations();

    List<ProductListDTO> getHotProducts();

    List<Map<String, Object>> getBanners();
}
