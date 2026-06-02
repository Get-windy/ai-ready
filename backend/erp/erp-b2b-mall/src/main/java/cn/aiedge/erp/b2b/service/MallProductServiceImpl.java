package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;
import cn.aiedge.erp.b2b.mapper.MallProductMapper;
import cn.aiedge.erp.b2b.model.MallProduct;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallProductServiceImpl implements MallProductService {

    private final MallProductMapper mallProductMapper;

    @Override
    public PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword) {
        log.info("查询商品列表: page={}, size={}, categoryId={}, keyword={}", page, size, categoryId, keyword);

        LambdaQueryWrapper<MallProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallProduct::getDeleted, false);
        wrapper.eq(MallProduct::getStatus, "ON_SHELF");

        if (categoryId != null && !categoryId.isEmpty()) {
            wrapper.eq(MallProduct::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MallProduct::getProductName, keyword);
        }

        wrapper.orderByDesc(MallProduct::getSalesCount);

        IPage<MallProduct> productPage = mallProductMapper.selectPage(new Page<>(page, size), wrapper);

        List<ProductListDTO> records = productPage.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

        PageResult<ProductListDTO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(productPage.getTotal());
        result.setPage((int) productPage.getCurrent());
        result.setSize((int) productPage.getSize());

        return result;
    }

    @Override
    public ProductDetailDTO getProductDetail(Long id) {
        log.info("获取商品详情: {}", id);
        MallProduct product = mallProductMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + id);
        }

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setImageUrl(product.getImageUrl());
        dto.setSalePrice(product.getSalePrice());
        dto.setMarketPrice(product.getMarketPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setSalesCount(product.getSalesCount());
        dto.setDescription(product.getDescription());
        dto.setCategoryName(product.getCategoryName());

        // Placeholder for additional data
        dto.setImages(new ArrayList<>());
        dto.setSpecs(new ArrayList<>());

        return dto;
    }

    @Override
    public List getCategories() {
        log.info("获取商品分类列表");
        // TODO: Implement actual category retrieval
        List<Map<String, Object>> categories = new ArrayList<>();

        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("id", "1");
        cat1.put("name", "电子产品");
        cat1.put("icon", "electronics");
        categories.add(cat1);

        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("id", "2");
        cat2.put("name", "办公用品");
        cat2.put("icon", "office");
        categories.add(cat2);

        Map<String, Object> cat3 = new HashMap<>();
        cat3.put("id", "3");
        cat3.put("name", "家居用品");
        cat3.put("icon", "home");
        categories.add(cat3);

        return categories;
    }

    @Override
    public List<ProductListDTO> getRecommendations() {
        log.info("获取推荐商品");
        LambdaQueryWrapper<MallProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallProduct::getDeleted, false);
        wrapper.eq(MallProduct::getStatus, "ON_SHELF");
        wrapper.orderByDesc(MallProduct::getSalesCount);
        wrapper.last("LIMIT 10");

        List<MallProduct> products = mallProductMapper.selectList(wrapper);
        return products.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductListDTO> getHotProducts() {
        log.info("获取热销商品");
        LambdaQueryWrapper<MallProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallProduct::getDeleted, false);
        wrapper.eq(MallProduct::getStatus, "ON_SHELF");
        wrapper.orderByDesc(MallProduct::getSalesCount);
        wrapper.last("LIMIT 10");

        List<MallProduct> products = mallProductMapper.selectList(wrapper);
        return products.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Override
    public List getBanners() {
        log.info("获取轮播图");
        // TODO: Implement actual banner retrieval
        List<Map<String, Object>> banners = new ArrayList<>();

        Map<String, Object> banner1 = new HashMap<>();
        banner1.put("id", "1");
        banner1.put("imageUrl", "https://example.com/banner1.jpg");
        banner1.put("linkUrl", "/products/1");
        banner1.put("title", "新品上市");
        banners.add(banner1);

        Map<String, Object> banner2 = new HashMap<>();
        banner2.put("id", "2");
        banner2.put("imageUrl", "https://example.com/banner2.jpg");
        banner2.put("linkUrl", "/products/2");
        banner2.put("title", "限时特惠");
        banners.add(banner2);

        return banners;
    }

    private ProductListDTO convertToListDTO(MallProduct product) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(product.getId());
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setImageUrl(product.getImageUrl());
        dto.setSalePrice(product.getSalePrice());
        dto.setMarketPrice(product.getMarketPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setSalesCount(product.getSalesCount());
        return dto;
    }
}
