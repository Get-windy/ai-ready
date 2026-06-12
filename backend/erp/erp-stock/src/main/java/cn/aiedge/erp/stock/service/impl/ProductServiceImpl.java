package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public List<Product> getProductList() {
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .eq("deleted", 0)
                .orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    public IPage<Product> getProductPage(Long categoryId, String keyword, String status,
                                          Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);

        QueryWrapper<Product> wrapper = new QueryWrapper<Product>();

        // 分类筛选
        if (categoryId != null && categoryId > 0) {
            wrapper.eq("p.category_id", categoryId);
        }

        // 关键字搜索(编码/名称/规格)
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("p.product_code", keyword)
                    .or()
                    .like("p.product_name", keyword)
                    .or()
                    .like("p.spec", keyword));
        }

        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq("p.status", status);
        }

        return baseMapper.selectProductPage(page, wrapper);
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
    public List<Product> exportList(Long categoryId, String keyword, String status) {
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .eq("deleted", 0);

        if (categoryId != null && categoryId > 0) {
            wrapper.eq("category_id", categoryId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("product_code", keyword)
                    .or()
                    .like("product_name", keyword)
                    .or()
                    .like("spec", keyword));
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("create_time");
        return this.list(wrapper);
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
}
