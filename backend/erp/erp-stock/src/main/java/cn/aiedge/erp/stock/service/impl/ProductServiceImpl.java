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
import java.util.List;
import java.util.Map;
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

        // 关键字搜索(编码/名称/规格/条码)
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("p.product_code", keyword)
                    .or()
                    .like("p.product_name", keyword)
                    .or()
                    .like("p.spec", keyword)
                    .or()
                    .like("p.barcode", keyword));
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
        return result;
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

        // 2) 查询基本单位
        LambdaQueryWrapper<ProductUnit> unitWrapper = new LambdaQueryWrapper<>();
        unitWrapper.in(ProductUnit::getProductId, productIds);
        unitWrapper.eq(ProductUnit::getIsBaseUnit, 1);
        unitWrapper.eq(ProductUnit::getDeleted, 0);
        unitWrapper.select(ProductUnit::getId, ProductUnit::getProductId);
        List<ProductUnit> baseUnits = productUnitMapper.selectList(unitWrapper);

        List<Long> needFallback = new ArrayList<>();
        Map<Long, Long> productIdToBaseUnitId = new HashMap<>();
        for (ProductUnit bu : baseUnits) {
            productIdToBaseUnitId.put(bu.getProductId(), bu.getId());
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

        // 4) 设置到每个 Product 上
        for (Product product : records) {
            Map<String, BigDecimal> m = priceMap.get(product.getId());
            product.setGradePriceMap(m != null ? m : Collections.<String, BigDecimal>emptyMap());
        }
    }

    @Override
    public Product getProductDetail(Long id) {
        return baseMapper.selectProductDetail(id);
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
        // 验证分类是否存在
        if (product.getCategoryId() != null && product.getCategoryId() > 0) {
            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
            if (category == null || category.getDeleted() != 0) {
                throw new RuntimeException("所属分类不存在或已删除");
            }
        }
        return save(product);
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
        return this.list(wrapper);
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
        return removeByIds(ids);
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
