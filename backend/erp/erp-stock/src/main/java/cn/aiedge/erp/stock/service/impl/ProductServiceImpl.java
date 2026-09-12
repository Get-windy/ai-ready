package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.entity.ProductGradePrice;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.mapper.ProductCategoryMapper;
import cn.aiedge.erp.stock.mapper.ProductGradePriceMapper;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.mapper.ProductUnitMapper;
import cn.aiedge.erp.stock.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产品ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductGradePriceMapper productGradePriceMapper;
    private final ProductUnitMapper productUnitMapper;
    private final cn.aiedge.erp.stock.service.ProductShieldService productShieldService;
    private final cn.aiedge.erp.stock.mapper.ProductImageMapper productImageMapper;
    private final cn.aiedge.erp.stock.service.ProductImageStorageService productImageStorageService;

    @Override
    public List<Product> getProductList() {
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .eq("deleted", 0)
                .orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    public IPage<Product> getProductPage(Long categoryId, String keyword, String status,
                                          String brand, String industryCategory,
                                          String createTimeStart, String createTimeEnd,
                                          Integer useCoupon, Integer isStandardProduct,
                                          String productType, Integer mallShelfStatus,
                                          Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);

        QueryWrapper<Product> wrapper = new QueryWrapper<Product>();

        // 分类筛选（包含所有子分类）
        if (categoryId != null && categoryId > 0) {
            List<Long> categoryIds = collectDescendantCategoryIds(categoryId);
            if (categoryIds.size() == 1) {
                wrapper.eq("p.category_id", categoryIds.get(0));
            } else {
                wrapper.in("p.category_id", categoryIds);
            }
        }

        // 关键字搜索(单框多字段：商品名称/货号/条码/规格/型号，对标查询区口径)
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("p.product_name", keyword)
                    .or()
                    .like("p.product_code_alias", keyword)
                    .or()
                    .like("p.product_code", keyword)
                    .or()
                    .like("p.barcode", keyword)
                    .or()
                    .like("p.spec", keyword)
                    .or()
                    .like("p.model", keyword));
        }

        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq("p.status", status);
        }

        // 品牌筛选
        if (StringUtils.hasText(brand)) {
            wrapper.like("p.brand", brand);
        }

        // 行业类别筛选
        if (StringUtils.hasText(industryCategory)) {
            wrapper.eq("p.industry_category", industryCategory);
        }

        // 创建日期范围筛选
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge("p.create_time", parseLocalDateToStartOfDay(createTimeStart));
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le("p.create_time", parseLocalDateToEndOfDay(createTimeEnd));
        }

        // 使用优惠券筛选
        if (useCoupon != null) {
            wrapper.eq("p.use_coupon", useCoupon);
        }

        // 是否标品筛选
        if (isStandardProduct != null) {
            wrapper.eq("p.is_standard_product", isStandardProduct);
        }

        // 产品类型筛选
        if (StringUtils.hasText(productType)) {
            wrapper.eq("p.product_type", productType);
        }

        // 商城上架状态筛选
        if (mallShelfStatus != null) {
            wrapper.eq("p.mall_shelf_status", mallShelfStatus);
        }

        IPage<Product> result = baseMapper.selectProductPage(page, wrapper);
        fillGradePriceMap(result);
        fillConversionRelation(result.getRecords());
        fillProductImage(result.getRecords());
        return result;
    }

    /** 图片视图URL前缀（与图片管理模块 ProductImageServiceImpl.VIEW_PREFIX 保持一致） */
    private static final String IMAGE_VIEW_PREFIX = "/api/erp/md/image/view/";

    /**
     * 修正商品图片列：image_url 指向的图片若已被删除/不存在，则回退到该商品当前有效的主图（其次任意有效图），
     * 都没有则置空。避免前端 img 直接 404（图片管理里删除/更换图片不会回写 product.image_url）。
     */
    private void fillProductImage(List<Product> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 1) 当前 image_url 指向的图片（需同时满足：记录未软删 + 物理文件存在）
        Map<Long, Long> productToImageId = new HashMap<>();
        List<Long> imageIds = new ArrayList<>();
        for (Product p : records) {
            Long imageId = parseViewImageId(p.getImageUrl());
            if (imageId != null) {
                productToImageId.put(p.getId(), imageId);
                imageIds.add(imageId);
            }
        }
        Set<Long> usableImageIds = new HashSet<>();
        if (!imageIds.isEmpty()) {
            for (cn.aiedge.erp.stock.entity.ProductImage img : loadImagesByIds(imageIds)) {
                if (productImageStorageService.exists(img.getFilePath())) {
                    usableImageIds.add(img.getId());
                }
            }
        }

        // 2) 需要回退的商品（url 为空 / 图片已软删 / 文件已被清理）
        List<Long> needFallback = new ArrayList<>();
        for (Product p : records) {
            Long imageId = productToImageId.get(p.getId());
            if (imageId == null || !usableImageIds.contains(imageId)) {
                needFallback.add(p.getId());
            }
        }

        // 3) 回退候选：优先主图，其次按排序取第一个物理文件存在的图片
        Map<Long, Long> fallbackImageId = new HashMap<>();
        if (!needFallback.isEmpty()) {
            Map<Long, List<cn.aiedge.erp.stock.entity.ProductImage>> byProduct = new HashMap<>();
            for (cn.aiedge.erp.stock.entity.ProductImage img : loadImagesByProductIds(needFallback)) {
                byProduct.computeIfAbsent(img.getProductId(), k -> new ArrayList<>()).add(img);
            }
            for (Map.Entry<Long, List<cn.aiedge.erp.stock.entity.ProductImage>> e : byProduct.entrySet()) {
                for (cn.aiedge.erp.stock.entity.ProductImage img : e.getValue()) {
                    if (productImageStorageService.exists(img.getFilePath())) {
                        fallbackImageId.put(e.getKey(), img.getId());
                        break;
                    }
                }
            }
        }

        // 4) 写回：无效引用一律置空或替换为可用图，保证列表里不会出现必然 404 的地址
        for (Product p : records) {
            Long imageId = productToImageId.get(p.getId());
            if (imageId != null && usableImageIds.contains(imageId)) {
                continue;
            }
            Long fallbackId = fallbackImageId.get(p.getId());
            p.setImageUrl(fallbackId == null ? null : IMAGE_VIEW_PREFIX + fallbackId);
        }
    }

    private List<cn.aiedge.erp.stock.entity.ProductImage> loadImagesByIds(List<Long> imageIds) {
        LambdaQueryWrapper<cn.aiedge.erp.stock.entity.ProductImage> w = new LambdaQueryWrapper<>();
        w.in(cn.aiedge.erp.stock.entity.ProductImage::getId, imageIds);
        w.eq(cn.aiedge.erp.stock.entity.ProductImage::getDeleted, 0);
        return productImageMapper.selectList(w);
    }

    private List<cn.aiedge.erp.stock.entity.ProductImage> loadImagesByProductIds(List<Long> productIds) {
        LambdaQueryWrapper<cn.aiedge.erp.stock.entity.ProductImage> fw = new LambdaQueryWrapper<>();
        fw.in(cn.aiedge.erp.stock.entity.ProductImage::getProductId, productIds);
        fw.eq(cn.aiedge.erp.stock.entity.ProductImage::getDeleted, 0);
        fw.orderByDesc(cn.aiedge.erp.stock.entity.ProductImage::getIsMain);
        fw.orderByAsc(cn.aiedge.erp.stock.entity.ProductImage::getSortNo);
        fw.orderByDesc(cn.aiedge.erp.stock.entity.ProductImage::getId);
        return productImageMapper.selectList(fw);
    }

    /** 从 /api/erp/md/image/view/{id} 形式的地址中解析图片ID，非该形式返回 null */
    private static Long parseViewImageId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains(IMAGE_VIEW_PREFIX)) {
            return null;
        }
        String tail = imageUrl.substring(imageUrl.indexOf(IMAGE_VIEW_PREFIX) + IMAGE_VIEW_PREFIX.length());
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }
        int q = tail.indexOf('?');
        if (q >= 0) {
            tail = tail.substring(0, q);
        }
        try {
            return Long.valueOf(tail.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 批量填充单位换算关系描述（如 "1箱=12袋；1中包=6袋"），列表「换算关系」列展示用。
     */
    private void fillConversionRelation(List<Product> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> productIds = new ArrayList<>();
        for (Product p : records) {
            productIds.add(p.getId());
        }
        LambdaQueryWrapper<ProductUnit> unitWrapper = new LambdaQueryWrapper<>();
        unitWrapper.in(ProductUnit::getProductId, productIds);
        unitWrapper.eq(ProductUnit::getDeleted, 0);
        unitWrapper.orderByAsc(ProductUnit::getSortOrder);
        List<ProductUnit> units = productUnitMapper.selectList(unitWrapper);

        Map<Long, List<ProductUnit>> byProduct = new HashMap<>();
        for (ProductUnit u : units) {
            byProduct.computeIfAbsent(u.getProductId(), k -> new ArrayList<>()).add(u);
        }
        for (Product p : records) {
            List<ProductUnit> list = byProduct.get(p.getId());
            if (list == null || list.isEmpty()) {
                p.setConversionRelation(null);
                if (p.getUnit() == null) {
                    p.setUnit(null);
                }
                continue;
            }
            String baseName = null;
            for (ProductUnit u : list) {
                if (u.getIsBaseUnit() != null && u.getIsBaseUnit() == 1) {
                    baseName = u.getUnitName();
                    break;
                }
            }
            if (baseName == null) {
                baseName = list.get(0).getUnitName();
            }
            if (p.getUnit() == null || p.getUnit().isEmpty()) {
                p.setUnit(baseName);
            }
            StringBuilder sb = new StringBuilder();
            for (ProductUnit u : list) {
                if (u.getIsBaseUnit() != null && u.getIsBaseUnit() == 1) {
                    continue;
                }
                if (u.getUnitName() == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("；");
                }
                BigDecimal rate = u.getConversionRate() != null ? u.getConversionRate() : BigDecimal.ONE;
                sb.append("1").append(u.getUnitName()).append("=")
                        .append(rate.stripTrailingZeros().toPlainString()).append(baseName);
            }
            p.setConversionRelation(sb.length() > 0 ? sb.toString() : "1" + baseName);
        }
    }

    /**
     * 为分页结果批量填充等级价格。
     * 优先取产品级别价格（unitId IS NULL），若没有则取基本单位（isBaseUnit=1）的价格。
     */
    private void fillGradePriceMap(IPage<Product> pageResult) {
        List<Product> records = pageResult.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> productIds = new ArrayList<>();
        for (Product p : records) {
            productIds.add(p.getId());
        }

        // 1) 查询产品级别等级价格（unitId IS NULL）
        LambdaQueryWrapper<ProductGradePrice> levelWrapper = new LambdaQueryWrapper<>();
        levelWrapper.in(ProductGradePrice::getProductId, productIds);
        levelWrapper.eq(ProductGradePrice::getDeleted, 0);
        levelWrapper.eq(ProductGradePrice::getIsActive, 1);
        levelWrapper.isNull(ProductGradePrice::getUnitId);
        List<ProductGradePrice> productLevelPrices = productGradePriceMapper.selectList(levelWrapper);

        Map<Long, Map<String, BigDecimal>> priceMap = new HashMap<>();
        for (ProductGradePrice gp : productLevelPrices) {
            priceMap.computeIfAbsent(gp.getProductId(), k -> new HashMap<>())
                    .putIfAbsent(gp.getGradeCode(),
                            gp.getGradePrice() != null ? gp.getGradePrice() : BigDecimal.ZERO);
        }

        // 2) 查询基本单位（含单位标准等级价列，作为最终回退口径）
        LambdaQueryWrapper<ProductUnit> unitWrapper = new LambdaQueryWrapper<>();
        unitWrapper.in(ProductUnit::getProductId, productIds);
        unitWrapper.eq(ProductUnit::getIsBaseUnit, 1);
        unitWrapper.eq(ProductUnit::getDeleted, 0);
        unitWrapper.select(ProductUnit::getId, ProductUnit::getProductId,
                ProductUnit::getGradePrice1, ProductUnit::getGradePrice2,
                ProductUnit::getGradePrice3, ProductUnit::getGradePrice4,
                ProductUnit::getGradePrice5, ProductUnit::getGradePrice6,
                ProductUnit::getGradePrice7, ProductUnit::getGradePrice8);
        List<ProductUnit> baseUnits = productUnitMapper.selectList(unitWrapper);

        List<Long> needFallback = new ArrayList<>();
        Map<Long, Long> productIdToBaseUnitId = new HashMap<>();
        Map<Long, ProductUnit> productIdToBaseUnit = new HashMap<>();
        for (ProductUnit bu : baseUnits) {
            productIdToBaseUnitId.put(bu.getProductId(), bu.getId());
            productIdToBaseUnit.put(bu.getProductId(), bu);
            if (!priceMap.containsKey(bu.getProductId())) {
                needFallback.add(bu.getProductId());
            }
        }

        // 3) 回退：查询基本单位的等级价格
        if (!needFallback.isEmpty()) {
            List<Long> relevantUnitIds = new ArrayList<>();
            for (Long pid : needFallback) {
                Long uid = productIdToBaseUnitId.get(pid);
                if (uid != null) {
                    relevantUnitIds.add(uid);
                }
            }

            if (!relevantUnitIds.isEmpty()) {
                LambdaQueryWrapper<ProductGradePrice> buWrapper = new LambdaQueryWrapper<>();
                buWrapper.in(ProductGradePrice::getUnitId, relevantUnitIds);
                buWrapper.eq(ProductGradePrice::getDeleted, 0);
                buWrapper.eq(ProductGradePrice::getIsActive, 1);
                List<ProductGradePrice> baseUnitPrices = productGradePriceMapper.selectList(buWrapper);

                Map<Long, Long> unitIdToProductId = new HashMap<>();
                for (Map.Entry<Long, Long> entry : productIdToBaseUnitId.entrySet()) {
                    unitIdToProductId.put(entry.getValue(), entry.getKey());
                }

                for (ProductGradePrice gp : baseUnitPrices) {
                    Long pid = unitIdToProductId.get(gp.getUnitId());
                    if (pid != null) {
                        Map<String, BigDecimal> m = priceMap.get(pid);
                        if (m == null) {
                            m = new HashMap<>();
                            priceMap.put(pid, m);
                        }
                        m.putIfAbsent(gp.getGradeCode(),
                                gp.getGradePrice() != null ? gp.getGradePrice() : BigDecimal.ZERO);
                    }
                }
            }
        }

        // 4) 最终回退：基本单位表的单位标准等级价列（商品表单「商品单位」维护）
        for (Product product : records) {
            Map<String, BigDecimal> m = priceMap.get(product.getId());
            if (m == null || m.isEmpty()) {
                ProductUnit bu = productIdToBaseUnit.get(product.getId());
                if (bu != null) {
                    m = baseUnitGradePrices(bu);
                }
            }
            product.setGradePriceMap(m != null ? m : Collections.<String, BigDecimal>emptyMap());
        }
    }

    /** 从基本单位的 grade_price_1..8 列构造等级价 Map（key=GRADE_1..8） */
    private Map<String, BigDecimal> baseUnitGradePrices(ProductUnit unit) {
        BigDecimal[] prices = new BigDecimal[]{
                unit.getGradePrice1(), unit.getGradePrice2(), unit.getGradePrice3(), unit.getGradePrice4(),
                unit.getGradePrice5(), unit.getGradePrice6(), unit.getGradePrice7(), unit.getGradePrice8()
        };
        Map<String, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] != null) {
                map.put("GRADE_" + (i + 1), prices[i]);
            }
        }
        return map;
    }

    @Override
    public Product getProductDetail(Long id) {
        Product product = baseMapper.selectProductDetail(id);
        if (product != null) {
            fillProductImage(java.util.Collections.singletonList(product));
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus("ENABLED");
        }
        if (product.getProductType() == null) {
            product.setProductType("SINGLE");
        }
        // 货号为空时按对标规则自动生成 SP + 时间戳36进制大写
        if (!StringUtils.hasText(product.getProductCodeAlias())) {
            product.setProductCodeAlias("SP" + Long.toString(System.currentTimeMillis(), 36).toUpperCase());
        }
        // product_code 为库表 NOT NULL 列：未传时取货号，并保证唯一（重复则追加序号）
        if (!StringUtils.hasText(product.getProductCode())) {
            product.setProductCode(product.getProductCodeAlias());
        }
        product.setProductCode(ensureUniqueProductCode(product.getProductCode()));
        // 验证分类是否存在
        if (product.getCategoryId() != null && product.getCategoryId() > 0) {
            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
            if (category == null || category.getDeleted() != 0) {
                throw new RuntimeException("所属分类不存在或已删除");
            }
        }
        return save(product);
    }

    /** 商品编码唯一化：已存在时追加 -2、-3 … */
    private String ensureUniqueProductCode(String code) {
        if (!StringUtils.hasText(code)) {
            return code;
        }
        String candidate = code;
        int seq = 1;
        while (this.count(new QueryWrapper<Product>().eq("product_code", candidate).eq("deleted", 0)) > 0) {
            seq++;
            candidate = code + "-" + seq;
        }
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProduct(Product product) {
        return updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProductStatus(Long id, String status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        return updateById(product);
    }

    @Override
    public List<Product> exportList(Long categoryId, String keyword, String status,
                                      String brand, String industryCategory,
                                      String createTimeStart, String createTimeEnd,
                                      Integer useCoupon, Integer isStandardProduct) {
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .eq("deleted", 0);

        if (categoryId != null && categoryId > 0) {
            List<Long> categoryIds = collectDescendantCategoryIds(categoryId);
            wrapper.in("category_id", categoryIds);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("product_code", keyword)
                    .or()
                    .like("product_name", keyword)
                    .or()
                    .like("spec", keyword)
                    .or()
                    .like("barcode", keyword));
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }

        if (StringUtils.hasText(brand)) {
            wrapper.like("brand", brand);
        }

        if (StringUtils.hasText(industryCategory)) {
            wrapper.eq("industry_category", industryCategory);
        }

        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge("create_time", parseLocalDateToStartOfDay(createTimeStart));
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le("create_time", parseLocalDateToEndOfDay(createTimeEnd));
        }

        if (useCoupon != null) {
            wrapper.eq("use_coupon", useCoupon);
        }

        if (isStandardProduct != null) {
            wrapper.eq("is_standard_product", isStandardProduct);
        }

        wrapper.orderByDesc("create_time");
        List<Product> rows = this.list(wrapper);
        fillProductImage(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<Product> batch = ids.stream().map(id -> {
            Product p = new Product();
            p.setId(id);
            p.setStatus(status);
            return p;
        }).collect(Collectors.toList());
        return updateBatchById(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean ok = removeByIds(ids);
        // 级联软删商品授权（屏蔽客户）关系，避免商品授权子标签出现孤儿行
        if (ok) {
            productShieldService.cancelByProductIds(ids);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        boolean ok = super.removeById(id);
        if (ok && id != null) {
            productShieldService.cancelByProductIds(
                    Collections.singletonList(Long.valueOf(String.valueOf(id))));
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdatePrices(List<BatchPriceUpdateDTO.PriceItem> items) {
        if (items == null || items.isEmpty()) {
            log.warn("[批量价格] 更新列表为空，跳过");
            return true;
        }
        List<Product> batch = new ArrayList<>(items.size());
        for (BatchPriceUpdateDTO.PriceItem item : items) {
            if (item.getId() == null) {
                log.warn("[批量价格] 跳过无效项: id为null");
                continue;
            }
            Product product = new Product();
            product.setId(item.getId());
            if (item.getCostPrice() != null) product.setCostPrice(item.getCostPrice());
            if (item.getStandardPrice() != null) product.setStandardPrice(item.getStandardPrice());
            if (item.getWholesalePrice() != null) product.setWholesalePrice(item.getWholesalePrice());
            batch.add(product);
        }
        if (batch.isEmpty()) {
            log.warn("[批量价格] 无有效项需更新");
            return true;
        }
        log.info("[批量价格] 批量更新 {} 条记录", batch.size());
        return updateBatchById(batch);
    }

    /**
     * 递归收集分类及其所有子孙分类的ID列表
     */
    private List<Long> collectDescendantCategoryIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        List<ProductCategory> children = productCategoryMapper.selectList(
                new QueryWrapper<ProductCategory>()
                        .eq("parent_id", parentId)
                        .eq("deleted", 0)
                        .select("id")
        );
        for (ProductCategory child : children) {
            ids.addAll(collectDescendantCategoryIds(child.getId()));
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchMoveCategory(List<Long> ids, Long categoryId) {
        if (ids == null || ids.isEmpty() || categoryId == null) {
            return 0;
        }
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null || Integer.valueOf(1).equals(category.getDeleted())) {
            throw new RuntimeException("目标分类不存在或已删除");
        }
        List<Product> batch = new ArrayList<>();
        for (Long id : ids) {
            Product p = new Product();
            p.setId(id);
            p.setCategoryId(categoryId);
            p.setCategory(category.getCategoryName());
            batch.add(p);
        }
        updateBatchById(batch);
        return batch.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateFields(List<Long> ids, Map<String, Object> fields) {
        if (ids == null || ids.isEmpty() || fields == null || fields.isEmpty()) {
            return 0;
        }
        List<Product> batch = new ArrayList<>();
        for (Long id : ids) {
            Product p = new Product();
            p.setId(id);
            if (fields.containsKey("brand")) {
                p.setBrand(String.valueOf(fields.get("brand")));
            }
            if (fields.containsKey("industryCategory")) {
                p.setIndustryCategory(String.valueOf(fields.get("industryCategory")));
            }
            if (fields.containsKey("categoryId")) {
                p.setCategoryId(Long.valueOf(String.valueOf(fields.get("categoryId"))));
            }
            if (fields.containsKey("isStandardProduct")) {
                p.setIsStandardProduct(Integer.valueOf(String.valueOf(fields.get("isStandardProduct"))));
            }
            if (fields.containsKey("useCoupon")) {
                p.setUseCoupon(Integer.valueOf(String.valueOf(fields.get("useCoupon"))));
            }
            batch.add(p);
        }
        updateBatchById(batch);
        return batch.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateShelfStatus(List<Long> ids, Integer mallShelfStatus) {
        if (ids == null || ids.isEmpty() || mallShelfStatus == null) {
            return 0;
        }
        List<Product> batch = new ArrayList<>();
        for (Long id : ids) {
            Product p = new Product();
            p.setId(id);
            p.setMallShelfStatus(mallShelfStatus);
            batch.add(p);
        }
        updateBatchById(batch);
        return batch.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setMallSortType(String sortType) {
        Product update = new Product();
        update.setMallSortType(sortType == null || sortType.isEmpty() ? "DEFAULT" : sortType);
        boolean ok = this.lambdaUpdate()
                .eq(Product::getDeleted, 0)
                .set(Product::getMallSortType, update.getMallSortType())
                .update();
        return ok ? 1 : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFromExcel(org.springframework.web.multipart.MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int count = 0;
        int skipped = 0;
        if (file == null || file.isEmpty()) {
            result.put("count", 0);
            result.put("skipped", 0);
            result.put("errors", java.util.Collections.singletonList("请选择要导入的文件"));
            return result;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (name.endsWith(".csv")) {
                return importFromCsv(file, errors);
            }
            try (org.apache.poi.ss.usermodel.Workbook workbook =
                         new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    result.put("count", 0);
                    result.put("skipped", 0);
                    result.put("errors", java.util.Collections.singletonList("Excel 中没有可读工作表"));
                    return result;
                }
                org.apache.poi.ss.usermodel.Row headRow = sheet.getRow(sheet.getFirstRowNum());
                if (headRow == null) {
                    result.put("count", 0);
                    result.put("skipped", 0);
                    result.put("errors", java.util.Collections.singletonList("Excel 表头为空"));
                    return result;
                }
                Map<String, Integer> headIndex = new HashMap<>();
                for (int i = 0; i < headRow.getLastCellNum(); i++) {
                    String h = cellText(headRow.getCell(i));
                    if (!h.isEmpty()) {
                        headIndex.put(h.replace("*", "").trim(), i);
                    }
                }
                if (!headIndex.containsKey("商品名称")) {
                    result.put("count", 0);
                    result.put("skipped", 0);
                    result.put("errors", java.util.Collections.singletonList("表头缺少必填列：商品名称"));
                    return result;
                }
                for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    String productName = cellText(row.getCell(headIndex.get("商品名称"))).trim();
                    if (productName.isEmpty()) {
                        continue;
                    }
                    try {
                        LambdaQueryWrapper<Product> dup = new LambdaQueryWrapper<>();
                        dup.eq(Product::getProductName, productName);
                        dup.eq(Product::getDeleted, 0);
                        if (this.count(dup) > 0) {
                            skipped++;
                            continue;
                        }
                        createImportedProduct(row, headIndex, productName);
                        count++;
                    } catch (Exception ex) {
                        errors.add("第" + (r + 1) + "行：" + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[商品导入] 失败", e);
            errors.add("解析失败：" + e.getMessage());
        }
        result.put("count", count);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    /** 按表头构建一行商品 + 基本单位 */
    private void createImportedProduct(org.apache.poi.ss.usermodel.Row row,
                                       Map<String, Integer> headIndex, String productName) {
        Product product = new Product();
        product.setProductName(productName);
        product.setProductCodeAlias(pick(row, headIndex, "货号"));
        product.setSpec(pick(row, headIndex, "规格"));
        product.setModel(pick(row, headIndex, "型号"));
        product.setOrigin(pick(row, headIndex, "产地"));
        product.setBrand(pick(row, headIndex, "品牌"));
        product.setBarcode(pick(row, headIndex, "条码"));
        String unitName = pick(row, headIndex, "单位");
        product.setUnit(unitName.isEmpty() ? "件" : unitName);
        String industry = pick(row, headIndex, "所属行业类别");
        product.setIndustryCategory(industry.isEmpty() ? "其他" : industry);
        product.setRetailPrice(decimalOf(pick(row, headIndex, "零售价")));
        product.setWholesalePrice(decimalOf(pick(row, headIndex, "批发价")));
        product.setPurchasePrice(decimalOf(pick(row, headIndex, "预设进价")));
        product.setRemark(pick(row, headIndex, "备注"));
        String shelfLife = pick(row, headIndex, "保质期天数");
        if (!shelfLife.isEmpty()) {
            try {
                product.setShelfLifeDays((int) Double.parseDouble(shelfLife));
            } catch (NumberFormatException ignore) {
                // 非数字忽略
            }
        }
        if (product.getProductCodeAlias() == null || product.getProductCodeAlias().isEmpty()) {
            product.setProductCodeAlias("SP" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        }
        product.setProductCode(product.getProductCodeAlias());
        product.setProductType("SINGLE");
        product.setStatus("ENABLED");
        product.setMallShelfStatus(0);
        product.setIsStandardProduct(1);
        this.save(product);

        ProductUnit baseUnit = new ProductUnit();
        baseUnit.setProductId(product.getId());
        baseUnit.setUnitName(product.getUnit());
        baseUnit.setIsBaseUnit(1);
        baseUnit.setConversionRate(BigDecimal.ONE);
        baseUnit.setBarcode(product.getBarcode());
        baseUnit.setSortOrder(0);
        baseUnit.setPresetPurchasePrice(product.getPurchasePrice());
        baseUnit.setWholesalePrice(product.getWholesalePrice());
        baseUnit.setRetailPrice(product.getRetailPrice());
        productUnitMapper.insert(baseUnit);

        Product patch = new Product();
        patch.setId(product.getId());
        patch.setDefaultSalesUnitId(baseUnit.getId());
        patch.setDefaultPurchaseUnitId(baseUnit.getId());
        patch.setDefaultStockUnitId(baseUnit.getId());
        this.updateById(patch);
    }

    /** CSV 导入（UTF-8，首行表头，逗号分隔） */
    private Map<String, Object> importFromCsv(org.springframework.web.multipart.MultipartFile file, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        int count = 0;
        int skipped = 0;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String headLine = reader.readLine();
            if (headLine == null) {
                result.put("count", 0);
                result.put("skipped", 0);
                result.put("errors", java.util.Collections.singletonList("CSV 内容为空"));
                return result;
            }
            String[] heads = headLine.split(",");
            Map<String, Integer> headIndex = new HashMap<>();
            for (int i = 0; i < heads.length; i++) {
                headIndex.put(heads[i].replace("*", "").replace("\uFEFF", "").trim(), i);
            }
            if (!headIndex.containsKey("商品名称")) {
                result.put("count", 0);
                result.put("skipped", 0);
                result.put("errors", java.util.Collections.singletonList("表头缺少必填列：商品名称"));
                return result;
            }
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] cells = line.split(",", -1);
                String productName = csvPick(cells, headIndex, "商品名称");
                if (productName.isEmpty()) {
                    continue;
                }
                try {
                    LambdaQueryWrapper<Product> dup = new LambdaQueryWrapper<>();
                    dup.eq(Product::getProductName, productName);
                    dup.eq(Product::getDeleted, 0);
                    if (this.count(dup) > 0) {
                        skipped++;
                        continue;
                    }
                    Product product = new Product();
                    product.setProductName(productName);
                    product.setProductCodeAlias(csvPick(cells, headIndex, "货号"));
                    product.setSpec(csvPick(cells, headIndex, "规格"));
                    product.setModel(csvPick(cells, headIndex, "型号"));
                    product.setOrigin(csvPick(cells, headIndex, "产地"));
                    product.setBrand(csvPick(cells, headIndex, "品牌"));
                    product.setBarcode(csvPick(cells, headIndex, "条码"));
                    String unitName = csvPick(cells, headIndex, "单位");
                    product.setUnit(unitName.isEmpty() ? "件" : unitName);
                    String industry = csvPick(cells, headIndex, "所属行业类别");
                    product.setIndustryCategory(industry.isEmpty() ? "其他" : industry);
                    product.setRetailPrice(decimalOf(csvPick(cells, headIndex, "零售价")));
                    product.setWholesalePrice(decimalOf(csvPick(cells, headIndex, "批发价")));
                    product.setPurchasePrice(decimalOf(csvPick(cells, headIndex, "预设进价")));
                    product.setRemark(csvPick(cells, headIndex, "备注"));
                    String shelfLifeCsv = csvPick(cells, headIndex, "保质期天数");
                    if (!shelfLifeCsv.isEmpty()) {
                        try {
                            product.setShelfLifeDays((int) Double.parseDouble(shelfLifeCsv));
                        } catch (NumberFormatException ignore) {
                            // 非数字忽略
                        }
                    }
                    if (!StringUtils.hasText(product.getProductCodeAlias())) {
                        product.setProductCodeAlias("SP" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
                    }
                    product.setProductCode(ensureUniqueProductCode(product.getProductCodeAlias()));
                    product.setProductType("SINGLE");
                    product.setStatus("ENABLED");
                    product.setMallShelfStatus(0);
                    product.setIsStandardProduct(1);
                    this.save(product);

                    ProductUnit baseUnit = new ProductUnit();
                    baseUnit.setProductId(product.getId());
                    baseUnit.setUnitName(product.getUnit());
                    baseUnit.setIsBaseUnit(1);
                    baseUnit.setConversionRate(BigDecimal.ONE);
                    baseUnit.setSortOrder(0);
                    baseUnit.setBarcode(product.getBarcode());
                    baseUnit.setPresetPurchasePrice(product.getPurchasePrice());
                    baseUnit.setWholesalePrice(product.getWholesalePrice());
                    baseUnit.setRetailPrice(product.getRetailPrice());
                    productUnitMapper.insert(baseUnit);

                    Product patch = new Product();
                    patch.setId(product.getId());
                    patch.setDefaultSalesUnitId(baseUnit.getId());
                    patch.setDefaultPurchaseUnitId(baseUnit.getId());
                    patch.setDefaultStockUnitId(baseUnit.getId());
                    this.updateById(patch);
                    count++;
                } catch (Exception ex) {
                    errors.add("第" + lineNo + "行：" + ex.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("解析失败：" + e.getMessage());
        }
        result.put("count", count);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    private static String csvPick(String[] cells, Map<String, Integer> headIndex, String head) {
        Integer idx = headIndex.get(head);
        if (idx == null || idx >= cells.length) {
            return "";
        }
        return cells[idx] == null ? "" : cells[idx].trim();
    }

    private static String pick(org.apache.poi.ss.usermodel.Row row, Map<String, Integer> headIndex, String head) {
        Integer idx = headIndex.get(head);
        if (idx == null) {
            return "";
        }
        return cellText(row.getCell(idx)).trim();
    }

    private static String cellText(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private static BigDecimal decimalOf(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> getDistinctIndustryCategories() {
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .select("DISTINCT industry_category")
                .eq("deleted", 0)
                .isNotNull("industry_category")
                .ne("industry_category", "")
                .orderByAsc("industry_category");
        List<Product> list = this.list(wrapper);
        return list.stream().map(Product::getIndustryCategory).collect(Collectors.toList());
    }

    @Override
    public List<String> getDistinctBrands() {
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .select("DISTINCT brand")
                .eq("deleted", 0)
                .isNotNull("brand")
                .ne("brand", "")
                .orderByAsc("brand");
        List<Product> list = this.list(wrapper);
        return list.stream().map(Product::getBrand).collect(Collectors.toList());
    }

    private String parseLocalDateToStartOfDay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString).atStartOfDay().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString + ". Expected format: yyyy-MM-dd");
        }
    }

    private String parseLocalDateToEndOfDay(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString).atTime(23, 59, 59).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString + ". Expected format: yyyy-MM-dd");
        }
    }
}
