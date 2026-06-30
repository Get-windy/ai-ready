package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dao.ErpProductMall;
import cn.aiedge.erp.b2b.dao.ErpProductMallMapper;
import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;
import cn.aiedge.erp.b2b.mapper.ShopBannerMapper;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商城商品服务
 * 数据源为 v_mall_product 视图（来自 erp_product + erp_stock）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallProductServiceImpl implements MallProductService {

    private final ErpProductMallMapper erpProductMallMapper;
    private final ShopBannerMapper shopBannerMapper;

    /** 获取当前登录用户的租户ID */
    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    @Override
    public PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword) {
        log.info("查询商品列表: page={}, size={}, categoryId={}, keyword={}", page, size, categoryId, keyword);

        LambdaQueryWrapper<ErpProductMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProductMall::getDeleted, 0);
        wrapper.eq(ErpProductMall::getStatus, "ON_SHELF");
        wrapper.eq(ErpProductMall::getTenantId, getTenantId());

        if (categoryId != null && !categoryId.isEmpty()) {
            wrapper.eq(ErpProductMall::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ErpProductMall::getProductName, keyword);
        }

        wrapper.orderByDesc(ErpProductMall::getSalesCount, ErpProductMall::getCreateTime);

        IPage<ErpProductMall> productPage = erpProductMallMapper.selectPage(new Page<>(page, size), wrapper);

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
        ErpProductMall product = erpProductMallMapper.selectById(id);
        if (product == null) {
            throw BusinessException.notFound("商品不存在: " + id);
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

        dto.setImages(new ArrayList<>());
        dto.setSpecs(new ArrayList<>());

        return dto;
    }

    @Override
    public List<Map<String, Object>> getCategories() {
        log.info("获取商品分类列表");
        // 从 v_mall_product 视图中提取所有存在的分类
        List<ErpProductMall> products = erpProductMallMapper.selectList(
                new LambdaQueryWrapper<ErpProductMall>()
                        .eq(ErpProductMall::getDeleted, 0)
                        .eq(ErpProductMall::getTenantId, getTenantId())
                        .isNotNull(ErpProductMall::getCategoryId)
                        .select(ErpProductMall::getCategoryId, ErpProductMall::getCategoryName)
                        .groupBy(ErpProductMall::getCategoryId, ErpProductMall::getCategoryName)
        );

        return products.stream().map(p -> {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", p.getCategoryId());
            cat.put("name", p.getCategoryName() != null ? p.getCategoryName() : p.getCategoryId());
            cat.put("icon", "default");
            return cat;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProductListDTO> getRecommendations() {
        log.info("获取推荐商品");
        LambdaQueryWrapper<ErpProductMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProductMall::getDeleted, 0);
        wrapper.eq(ErpProductMall::getStatus, "ON_SHELF");
        wrapper.eq(ErpProductMall::getTenantId, getTenantId());
        wrapper.orderByDesc(ErpProductMall::getSalesCount);
        wrapper.last("LIMIT 10");

        List<ErpProductMall> products = erpProductMallMapper.selectList(wrapper);
        return products.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductListDTO> getHotProducts() {
        log.info("获取热销商品");
        LambdaQueryWrapper<ErpProductMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProductMall::getDeleted, 0);
        wrapper.eq(ErpProductMall::getStatus, "ON_SHELF");
        wrapper.eq(ErpProductMall::getTenantId, getTenantId());
        wrapper.orderByDesc(ErpProductMall::getSalesCount);
        wrapper.last("LIMIT 10");

        List<ErpProductMall> products = erpProductMallMapper.selectList(wrapper);
        return products.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getBanners() {
        log.info("获取轮播图");
        List<ShopBanner> banners = shopBannerMapper.selectList(
                new LambdaQueryWrapper<ShopBanner>()
                        .eq(ShopBanner::getStatus, 1)
                        .eq(ShopBanner::getTenantId, getTenantId())
                        .orderByAsc(ShopBanner::getSortOrder)
        );
        return banners.stream().map(banner -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", String.valueOf(banner.getId()));
            map.put("imageUrl", banner.getImageUrl());
            map.put("linkUrl", banner.getLinkUrl());
            map.put("linkType", banner.getLinkType());
            map.put("linkValue", banner.getLinkValue());
            map.put("title", banner.getTitle());
            return map;
        }).collect(Collectors.toList());
    }

    private ProductListDTO convertToListDTO(ErpProductMall product) {
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
