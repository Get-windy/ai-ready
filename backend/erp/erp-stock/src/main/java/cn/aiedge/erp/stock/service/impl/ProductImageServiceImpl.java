package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.stock.dto.ProductImageMatchResultVO;
import cn.aiedge.erp.stock.dto.ProductImageRowVO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.entity.ProductImage;
import cn.aiedge.erp.stock.mapper.ProductCategoryMapper;
import cn.aiedge.erp.stock.mapper.ProductImageMapper;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.service.ProductImageService;
import cn.aiedge.erp.stock.service.ProductImageStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片管理服务实现（商品图片列表 + 图片空间）
 *
 * <p>P0 图片单一口径：商品图片与图片空间素材共用 erp_product_image 一张表，
 * product_id 为空即未匹配素材，非空即已关联商品的图片。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage>
        implements ProductImageService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductImageStorageService storageService;

    /** 图片访问地址前缀（controller 内 view 接口） */
    private static final String VIEW_PREFIX = "/api/erp/md/image/view/";

    // ─────────────────────────── 商品图片列表 ───────────────────────────

    @Override
    public IPage<ProductImageRowVO> getProductImagePage(Long tenantId, Long categoryId, String keyword,
                                                        String imageFilter, String spec, String model,
                                                        String origin, String brand, String status,
                                                        String sortField, String sortOrder,
                                                        Integer pageNum, Integer pageSize) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();

        if (categoryId != null && categoryId > 0) {
            List<Long> categoryIds = collectDescendantCategoryIds(categoryId);
            if (!categoryIds.isEmpty()) {
                wrapper.in("category_id", categoryIds);
            }
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("product_name", keyword)
                    .or().like("product_code", keyword)
                    .or().like("product_code_alias", keyword)
                    .or().like("barcode", keyword)
                    .or().like("spec", keyword));
        }
        if (StringUtils.hasText(spec)) {
            wrapper.like("spec", spec);
        }
        if (StringUtils.hasText(model)) {
            wrapper.like("model", model);
        }
        if (StringUtils.hasText(origin)) {
            wrapper.like("origin", origin);
        }
        if (StringUtils.hasText(brand)) {
            wrapper.like("brand", brand);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }

        // 图片筛选：按该商品是否已关联图片
        // ⚠️ inSql 内的 SQL 不会被多租户插件改写，必须显式带 tenant_id，
        //    否则会跨租户判断（当前靠雪花 ID 全局唯一侥幸正确），且无法命中 idx_epi_tenant。
        long tid = tenantId == null ? 0L : tenantId;
        String imgSubSql = "SELECT product_id FROM erp_product_image"
                + " WHERE deleted = 0 AND product_id IS NOT NULL AND tenant_id = " + tid;
        if ("HAS".equalsIgnoreCase(imageFilter)) {
            wrapper.inSql("id", imgSubSql);
        } else if ("NONE".equalsIgnoreCase(imageFilter)) {
            wrapper.notInSql("id", imgSubSql);
        }

        boolean asc = !"DESC".equalsIgnoreCase(sortOrder);
        if ("PRODUCT_NAME".equalsIgnoreCase(sortField)) {
            wrapper.orderBy(true, asc, "product_name");
        } else if ("CREATE_TIME".equalsIgnoreCase(sortField)) {
            wrapper.orderBy(true, asc, "create_time");
        } else {
            // 默认「按货号」
            wrapper.orderBy(true, asc, "product_code_alias");
        }
        wrapper.orderByAsc("id");

        Page<Product> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        IPage<Product> productPage = productMapper.selectPage(page, wrapper);

        List<Long> productIds = productPage.getRecords().stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, List<ProductImage>> imageMap = loadImagesByProductIds(productIds);

        Page<ProductImageRowVO> result = new Page<>(productPage.getCurrent(), productPage.getSize(),
                productPage.getTotal());
        result.setRecords(productPage.getRecords().stream()
                .map(p -> toRowVO(p, imageMap.getOrDefault(p.getId(), Collections.emptyList())))
                .collect(Collectors.toList()));
        return result;
    }

    private ProductImageRowVO toRowVO(Product p, List<ProductImage> images) {
        ProductImageRowVO vo = new ProductImageRowVO();
        vo.setProductId(p.getId());
        vo.setProductName(p.getProductName());
        vo.setProductCode(StringUtils.hasText(p.getProductCodeAlias()) ? p.getProductCodeAlias() : p.getProductCode());
        vo.setStatus(p.getStatus());
        vo.setSpec(p.getSpec());
        vo.setModel(p.getModel());
        vo.setOrigin(p.getOrigin());
        vo.setBrand(p.getBrand());
        vo.setCategoryId(p.getCategoryId());
        vo.setImages(images.stream().map(img -> {
            ProductImageRowVO.ImageItem item = new ProductImageRowVO.ImageItem();
            item.setId(img.getId());
            item.setImageName(img.getImageName());
            item.setImageUrl(viewUrl(img));
            item.setIsMain(img.getIsMain());
            item.setFileSize(img.getFileSize());
            return item;
        }).collect(Collectors.toList()));
        return vo;
    }

    private Map<Long, List<ProductImage>> loadImagesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductImage> images = this.list(new LambdaQueryWrapper<ProductImage>()
                .in(ProductImage::getProductId, productIds)
                .orderByDesc(ProductImage::getIsMain)
                .orderByAsc(ProductImage::getSortNo)
                .orderByAsc(ProductImage::getId));
        return images.stream().collect(Collectors.groupingBy(ProductImage::getProductId));
    }

    private List<Long> collectDescendantCategoryIds(Long rootId) {
        List<ProductCategory> all = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().select(ProductCategory::getId, ProductCategory::getParentId));
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (ProductCategory c : all) {
            Long pid = c.getParentId() == null ? 0L : c.getParentId();
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(c.getId());
        }
        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            result.add(id);
            List<Long> children = childrenMap.get(id);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return result;
    }

    // ─────────────────────────── 图片空间 ───────────────────────────

    @Override
    public IPage<ProductImage> getSpacePage(Long tenantId, String keyword, Integer onlyImage,
                                            Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ProductImage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ProductImage::getImageName, keyword)
                    .or().like(ProductImage::getMatchKey, keyword));
        }
        if (onlyImage != null && onlyImage == 1) {
            wrapper.likeRight(ProductImage::getFileType, "image/");
        }
        wrapper.orderByDesc(ProductImage::getId);
        Page<ProductImage> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 60 : pageSize);
        IPage<ProductImage> result = this.page(page, wrapper);
        result.getRecords().forEach(img -> img.setImageUrl(viewUrl(img)));
        return result;
    }

    // ─────────────────────────── 上传 / 绑定 / 搬移 ───────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductImage upload(MultipartFile file, Long productId, Integer isMain) throws IOException {
        Long tenantId = currentTenantId();
        String relativePath = storageService.save(file, tenantId);

        ProductImage image = new ProductImage();
        image.setTenantId(tenantId);
        image.setProductId(productId);
        image.setImageName(truncateName(file.getOriginalFilename()));
        image.setMatchKey(stripIndexSuffix(file.getOriginalFilename()));
        image.setFilePath(relativePath);
        image.setFileSize(file.getSize());
        image.setFileType(file.getContentType());
        image.setSource("UPLOAD");
        image.setSortNo(0);
        image.setDeleted(0);
        boolean asMain = productId != null && (isMain == null || isMain == 1);
        image.setIsMain(asMain ? 1 : 0);
        this.save(image);

        image.setImageUrl(viewUrl(image));
        if (asMain) {
            applyMainImage(productId, image.getId());
        }
        return image;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductImageMatchResultVO autoMatch(Long tenantId, String matchType) {
        boolean byCode = "CODE".equalsIgnoreCase(matchType);
        List<ProductImage> materials = this.list(new LambdaQueryWrapper<ProductImage>()
                .isNull(ProductImage::getProductId));
        ProductImageMatchResultVO result = new ProductImageMatchResultVO();
        result.setTotal(materials.size());
        if (materials.isEmpty()) {
            return result;
        }

        List<Product> products = productMapper.selectList(new QueryWrapper<Product>()
                .eq("deleted", 0)
                .select("id", "product_name", "product_code", "product_code_alias"));
        Map<String, List<Product>> index = products.stream()
                .filter(p -> StringUtils.hasText(byCode ? codeKey(p) : p.getProductName()))
                .collect(Collectors.groupingBy(p -> (byCode ? codeKey(p) : p.getProductName()).trim()));

        int matched = 0, ambiguous = 0, unmatched = 0;
        for (ProductImage material : materials) {
            String key = material.getMatchKey();
            if (!StringUtils.hasText(key)) {
                unmatched++;
                continue;
            }
            List<Product> hits = index.get(key.trim());
            if (hits == null || hits.isEmpty()) {
                unmatched++;
                continue;
            }
            if (hits.size() > 1) {
                ambiguous++;
                continue;
            }
            material.setProductId(hits.get(0).getId());
            material.setSource("AUTO_MATCH");
            this.updateById(material);
            matched++;
        }
        result.setMatched(matched);
        result.setAmbiguous(ambiguous);
        result.setUnmatched(unmatched);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bind(Long imageId, Long productId, Integer isMain) {
        ProductImage image = this.getById(imageId);
        if (image == null || productId == null) {
            return false;
        }
        Long fromProductId = image.getProductId();
        image.setProductId(productId);
        image.setSource("BIND");
        boolean asMain = isMain != null && isMain == 1;
        image.setIsMain(asMain ? 1 : 0);
        this.updateById(image);
        if (asMain) {
            applyMainImage(productId, imageId);
        }
        // 改绑到别的商品后，原商品的主图可能落空，需重算回退
        if (fromProductId != null && !fromProductId.equals(productId)) {
            refreshProductMainImage(fromProductId);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int move(List<Long> imageIds, Long productId) {
        if (imageIds == null || imageIds.isEmpty() || productId == null) {
            return 0;
        }
        Set<Long> fromProductIds = new LinkedHashSet<>();
        int count = 0;
        for (Long id : imageIds) {
            ProductImage image = this.getById(id);
            if (image == null) {
                continue;
            }
            Long fromProductId = image.getProductId();
            if (fromProductId != null && !fromProductId.equals(productId)) {
                fromProductIds.add(fromProductId);
            }
            image.setProductId(productId);
            image.setIsMain(0);
            image.setSource("MOVE");
            this.updateById(image);
            count++;
        }
        // 搬走图片的原商品需重算主图（图片已不属于它，image_url 不能继续指向它）
        fromProductIds.forEach(this::refreshProductMainImage);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteImages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<ProductImage> images = this.listByIds(ids);
        // ⚠️ 不能用 isMain 判断是否需要回退：图片被 move / bind 置为非主图后，
        //    其商品的 image_url 仍可能指向它，删除后前端 <img> 会持续 404。
        //    统一按「该图片归属过的商品」重算主图。
        Set<Long> affectedProductIds = new LinkedHashSet<>();
        int count = 0;
        for (ProductImage image : images) {
            if (image.getProductId() != null) {
                affectedProductIds.add(image.getProductId());
            }
            this.removeById(image.getId());
            storageService.delete(image.getFilePath());
            count++;
        }
        affectedProductIds.forEach(this::refreshProductMainImage);
        return count;
    }

    /**
     * 重算并回写商品主图：取该商品剩余图片中排序最前的一张，无图则清空。
     *
     * <p>删除 / 搬移 / 改绑图片后调用，保证 <code>erp_product.image_url</code> 不会
     * 残留指向已删除或已不属于该商品的图片（否则商品列表、图片管理页的
     * &lt;img&gt; 会请求到 404）。</p>
     */
    private void refreshProductMainImage(Long productId) {
        if (productId == null) {
            return;
        }
        List<ProductImage> rest = listByProductId(productId);
        ProductImage main = rest.isEmpty() ? null : rest.get(0);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getImageUrl, main == null ? null : viewUrl(main)));
        if (main != null && (main.getIsMain() == null || main.getIsMain() != 1)) {
            this.update(new LambdaUpdateWrapper<ProductImage>()
                    .eq(ProductImage::getId, main.getId())
                    .set(ProductImage::getIsMain, 1));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setMain(Long imageId) {
        ProductImage image = this.getById(imageId);
        if (image == null || image.getProductId() == null) {
            return false;
        }
        applyMainImage(image.getProductId(), imageId);
        return true;
    }

    @Override
    public byte[] readContent(Long imageId) throws IOException {
        ProductImage image = this.getById(imageId);
        if (image == null) {
            return null;
        }
        return storageService.read(image.getFilePath());
    }

    @Override
    public ProductImage getImage(Long imageId) {
        return this.getById(imageId);
    }

    @Override
    public List<ProductImage> listByProductId(Long productId) {
        if (productId == null) {
            return Collections.emptyList();
        }
        List<ProductImage> list = this.list(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .orderByDesc(ProductImage::getIsMain)
                .orderByAsc(ProductImage::getSortNo)
                .orderByAsc(ProductImage::getId));
        list.forEach(img -> img.setImageUrl(viewUrl(img)));
        return list;
    }

    // ─────────────────────────── 内部工具 ───────────────────────────

    /** 设置主图：同商品其它图片取消主图，并回写商品主图 URL */
    private void applyMainImage(Long productId, Long imageId) {
        this.update(new LambdaUpdateWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .set(ProductImage::getIsMain, 0));
        this.update(new LambdaUpdateWrapper<ProductImage>()
                .eq(ProductImage::getId, imageId)
                .set(ProductImage::getIsMain, 1));
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getImageUrl, VIEW_PREFIX + imageId));
    }

    private String viewUrl(ProductImage image) {
        return VIEW_PREFIX + image.getId();
    }

    private String codeKey(Product p) {
        return StringUtils.hasText(p.getProductCodeAlias()) ? p.getProductCodeAlias() : p.getProductCode();
    }

    /**
     * 文件名截断到 image_name 列上限（255，保留扩展名），避免超长文件名写库报错
     */
    static String truncateName(String name) {
        if (name == null || name.length() <= 255) {
            return name;
        }
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            ext = name.substring(dot);
        }
        int keep = Math.max(1, 255 - ext.length());
        return name.substring(0, Math.min(keep, name.length())) + ext;
    }

    /**
     * 去掉自动匹配命名规则的序号后缀：sp001-1 / sp001-2 → sp001
     */
    static String stripIndexSuffix(String name) {
        if (!StringUtils.hasText(name)) {
            return name;
        }
        String base = name;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base.replaceFirst("-\\d+$", "");
    }

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
