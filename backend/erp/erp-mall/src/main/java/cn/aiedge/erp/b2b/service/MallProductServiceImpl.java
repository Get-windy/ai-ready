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
import java.util.Comparator;
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
     * <p>2026-09-26：实现已上提到 {@link MallGuestAccess#requireShop()} —— 店铺配置下发、
     * 商品列表、标签等 C 端只读接口必须用**同一套**准入判定，各写一份迟早出现
     * "商品进不去但店铺配置能看到"这类不一致。此处保留薄包装，避免本类多处调用点改写。</p>
     */
    private ShopConfig requireShop() {
        return guestAccess.requireShop();
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
    public PageResult<ProductListDTO> listProducts(int page, int size, String categoryId, String keyword, String tagCode) {
        log.info("查询商品列表: page={}, size={}, categoryId={}, keyword={}, tagCode={}",
                page, size, categoryId, keyword, tagCode);

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
        // 商品标签（分类页最顶部的标签 Tab）：erp_product.mall_tags 是**逗号分隔的槽位码**，
        // 故用 like 做"包含"匹配（不能 eq，否则一个商品挂多标签时查不到）。
        // ⚠️ 前置条件：tagCode 必须来自本店启用的标签（erp_mall_tag），不能是任意串 ——
        // 否则用户可凭此探测任意文本；由 /v1/mall/tags 下发、前端只回传已知码。
        if (tagCode != null && !tagCode.isEmpty()) {
            wrapper.like(ErpProductMall::getProductTag, tagCode);
        }

        wrapper.orderByDesc(ErpProductMall::getSalesCount, ErpProductMall::getCreateTime);

        IPage<ErpProductMall> productPage = erpProductMallMapper.selectPage(new Page<>(page, size), wrapper);

        List<ProductListDTO> records = productPage.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
        applyPriceVisibility(shopConfig, records);
        fillCategoryNames(records);

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

        // ① 找出**在售商品实际挂了的**分类 id（商城里只该出现有货的分类）
        Long tenantId = getTenantId();
        List<ErpProductMall> products = erpProductMallMapper.selectList(
                new LambdaQueryWrapper<ErpProductMall>()
                        .eq(ErpProductMall::getDeleted, 0)
                        .eq(ErpProductMall::getStatus, "ON_SHELF")
                        .eq(ErpProductMall::getTenantId, tenantId)
                        .isNotNull(ErpProductMall::getCategoryId)
                        .select(ErpProductMall::getCategoryId)
                        .groupBy(ErpProductMall::getCategoryId)
        );
        List<String> ids = products.stream()
                .map(ErpProductMall::getCategoryId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // ② 名称回**分类树**取（权威来源），而不是用商品表上的冗余列 —— 见 Mapper 注释：
        //    商品上的 mall_category_name/category 实测为空，直接读会退化成显示裸 id。
        List<Map<String, Object>> cats = erpProductMallMapper.selectCategoryNames(tenantId, ids);
        return cats.stream()
                .sorted(Comparator.comparing(c -> {
                    Object o = c.get("sort_order");
                    return o instanceof Number ? ((Number) o).intValue() : 0;
                }))
                .map(c -> {
                    Map<String, Object> cat = new LinkedHashMap<>();
                    cat.put("id", c.get("category_id"));
                    cat.put("name", c.get("category_name"));
                    // 分类树的 icon 列当前无数据；非 URL 时前端自动落到文字占位
                    cat.put("icon", "default");
                    cat.put("level", c.get("category_level"));
                    cat.put("parentId", c.get("parent_id"));
                    return cat;
                })
                .collect(Collectors.toList());
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
        fillCategoryNames(dtos);
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
        fillCategoryNames(dtos);
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

    /**
     * 批量把分类名补进列表 DTO（**一次查询**，不是每条商品查一次）。
     *
     * <p>视图里的 category_name 来自商品表上的冗余列，实测为空；权威名称在
     * {@code erp_product_category}。列表页每屏 20 条，不批量处理就是 20 次单查。</p>
     */
    private void fillCategoryNames(List<ProductListDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<String> ids = dtos.stream()
                .map(ProductListDTO::getCategoryId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<String, String> nameById = new HashMap<>();
        for (Map<String, Object> row : erpProductMallMapper.selectCategoryNames(getTenantId(), ids)) {
            Object id = row.get("category_id");
            Object name = row.get("category_name");
            if (id != null && name != null) {
                nameById.put(id.toString(), name.toString());
            }
        }
        for (ProductListDTO dto : dtos) {
            String name = nameById.get(dto.getCategoryId());
            if (name != null) {
                dto.setCategoryName(name);
            }
        }
    }

    /** 取文本首行并截断（列表响应里不该出现整段详情；null 安全） */
    private static String firstLine(String text, int maxLen) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String line = text.strip().split("\\R", 2)[0].strip();
        if (line.isEmpty()) {
            return null;
        }
        return line.length() > maxLen ? line.substring(0, maxLen) : line;
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
        // 2026-09-26：补齐 C 端商品卡所需字段（视图列已存在，不增加查询）
        dto.setCategoryId(product.getCategoryId());
        dto.setCategoryName(product.getCategoryName());
        dto.setIndustryCategory(product.getIndustryCategory());
        dto.setSpecification(product.getSpecification());
        dto.setUnitName(product.getUnitName());
        dto.setProductTag(product.getProductTag());
        // 卖点条只取首行并截断，避免把整段图文详情塞进列表响应
        dto.setDescription(firstLine(product.getDescription(), 30));
        dto.setMinOrderQuantity(product.getMinOrderQuantity());
        dto.setProductType(product.getProductType());
        return dto;
    }
}
