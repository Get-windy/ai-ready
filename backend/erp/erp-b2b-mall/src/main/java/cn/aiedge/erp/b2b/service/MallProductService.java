package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;

import java.util.List;

public interface MallProductService {

    PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword);

    ProductDetailDTO getProductDetail(Long id);

    List getCategories();

    List<ProductListDTO> getRecommendations();

    List<ProductListDTO> getHotProducts();

    List getBanners();
}
