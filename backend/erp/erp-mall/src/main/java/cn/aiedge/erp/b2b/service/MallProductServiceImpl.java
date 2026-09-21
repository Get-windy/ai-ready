package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dao.ErpProductMall;
import cn.aiedge.erp.b2b.dao.ErpProductMallMapper;
import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;
import cn.aiedge.erp.b2b.mapper.ShopBannerMapper;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.support.MallGuestAccess;
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
    private final MallGuestAccess guestAccess;

    /**
     * 当前访问的是哪家店（会话租户优先，其次 {@code X-Tenant-Id} 头）。
     *
     * <p>⚠️ 原实现是「取不到会话租户就回落 0」——游客因此恒查 {@code tenant_id = 0}，
     * 结果是**空商城**（不是跨租户泄露，但也用不了）。现在改为取不到就交给
     * {@link #requireShop()} 明确拒绝，不再用 0 冒充一个租户。</p>
     */
    private Long getTenantId() {
        Long tenantId = guestAccess.currentShopTenantId();
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析当前店铺并做**游客准入校验**，返回该店铺配置。
     *
     * <p>B2B 租户商城模型下，游客必须能知道「我在逛哪家店」，否则任何查询都无意义：
     * 要么带 {@code X-Tenant-Id}，要么先登录。取不到就明确报错，
     * 而不是退化成「查全部租户」或「查 tenant_id=0」。</p>
     *
     * <p>准入口径见 {@link MallGuestAccess#guestMayBrowse}：店铺
     * {@code allowGuest = ALLOW} 才允许游客；无配置行（未开通商城）按 fail-closed 拒绝。</p>
     */
    private ShopConfig requireShop() {
        Long tenantId = guestAccess.currentShopTenantId();
        if (tenantId == null) {
            throw BusinessException.badRequest("无法确定店铺：请携带 X-Tenant-Id 请求头，或先登录");
        }
        ShopConfig config = guestAccess.shopConfig(tenantId);
        if (!guestAccess.guestMayBrowse(config)) {
            throw BusinessException.forbidden("该店铺未开放游客访问，请先登录");
        }
        return config;
    }

    /**
     * 按店铺开关决定是否对当前调用者隐藏价格。
     *
     * <p>对**已登录用户不做处理**：买家价格由客户等级另算，不归本开关管。</p>
     */
    private void applyPriceVisibility(ShopConfig config, List<ProductListDTO> dtos) {
        if (guestAccess.priceVisible(config)) {
            return;
        }
        dtos.forEach(d -> {
            d.setSalePrice(null);
            d.setMarketPrice(null);
        });
    }

    @Override
    public PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword) {
        log.info("查询商品列表: page={}, size={}, categoryId={}, keyword={}", page, size, categoryId, keyword);

        ShopConfig shopConfig = requireShop();

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
        applyPriceVisibility(shopConfig, records);

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

        ShopConfig shopConfig = requireShop();

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

        applyPriceVisibility(shopConfig, List.of(dto));

        return dto;
    }

    @Override
    public List<Map<String, Object>> getCategories() {
        log.info("获取商品分类列表");
        requireShop();

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
        ShopConfig shopConfig = requireShop();
        LambdaQueryWrapper<ErpProductMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProductMall::getDeleted, 0);
        wrapper.eq(ErpProductMall::getStatus, "ON_SHELF");
        wrapper.eq(ErpProductMall::getTenantId, getTenantId());
        wrapper.orderByDesc(ErpProductMall::getSalesCount);
        wrapper.last("LIMIT 10");

        List<ErpProductMall> products = erpProductMallMapper.selectList(wrapper);
        List<ProductListDTO> dtos = products.stream().map(this::convertToListDTO).collect(Collectors.toList());
        applyPriceVisibility(shopConfig, dtos);
        return dtos;
    }

    @Override
    public List<ProductListDTO> getHotProducts() {
        log.info("获取热销商品");
        ShopConfig shopConfig = requireShop();
        LambdaQueryWrapper<ErpProductMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProductMall::getDeleted, 0);
        wrapper.eq(ErpProductMall::getStatus, "ON_SHELF");
        wrapper.eq(ErpProductMall::getTenantId, getTenantId());
        wrapper.orderByDesc(ErpProductMall::getSalesCount);
        wrapper.last("LIMIT 10");

        List<ErpProductMall> products = erpProductMallMapper.selectList(wrapper);
        List<ProductListDTO> dtos = products.stream().map(this::convertToListDTO).collect(Collectors.toList());
        applyPriceVisibility(shopConfig, dtos);
        return dtos;
    }

    @Override
    public List<Map<String, Object>> getBanners() {
        log.info("获取轮播图");
        requireShop();
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
