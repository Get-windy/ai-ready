package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.mapper.ProductCategoryMapper;
import cn.aiedge.erp.stock.service.ProductCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品分类Service实现
 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public List<ProductCategory> getCategoryTree() {
        // 获取所有分类,在内存中构建树
        List<ProductCategory> all = productCategoryMapper.selectAllOrdered();
        return buildTree(all, 0L);
    }

    @Override
    public List<ProductCategory> getChildren(Long parentId) {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, parentId)
                .eq(ProductCategory::getDeleted, 0)
                .orderByAsc(ProductCategory::getSortOrder);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createCategory(ProductCategory category) {
        // 自动计算层级
        if (category.getParentId() != null && category.getParentId() > 0) {
            ProductCategory parent = getById(category.getParentId());
            if (parent != null) {
                category.setCategoryLevel(parent.getCategoryLevel() + 1);
            } else {
                category.setCategoryLevel(1);
            }
        } else {
            category.setParentId(0L);
            category.setCategoryLevel(1);
        }
        if (category.getStatus() == null) category.setStatus(1);
        return save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategory(ProductCategory category) {
        // 如果修改了父级,重算层级
        ProductCategory old = getById(category.getId());
        if (old != null && category.getParentId() != null
                && !old.getParentId().equals(category.getParentId())) {
            if (category.getParentId() > 0) {
                ProductCategory newParent = getById(category.getParentId());
                category.setCategoryLevel(newParent != null ? newParent.getCategoryLevel() + 1 : 1);
            } else {
                category.setParentId(0L);
                category.setCategoryLevel(1);
            }
        }
        return updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long id) {
        // 检查是否有子分类
        long childCount = count(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id)
                .eq(ProductCategory::getDeleted, 0));
        if (childCount > 0) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }
        // 检查是否有产品引用
        Integer productCount = productCategoryMapper.countProductByCategory(id);
        if (productCount != null && productCount > 0) {
            throw new RuntimeException("该分类下存在" + productCount + "个产品，无法删除");
        }
        return removeById(id);
    }

    // ── 私有方法 ──

    /**
     * 构建分类树
     */
    private List<ProductCategory> buildTree(List<ProductCategory> all, Long parentId) {
        List<ProductCategory> children = all.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .collect(Collectors.toList());

        for (ProductCategory category : children) {
            List<ProductCategory> subs = buildTree(all, category.getId());
            category.setChildren(subs);
            // 统计产品数量
            Integer count = productCategoryMapper.countProductByCategory(category.getId());
            category.setProductCount(count != null ? count : 0);
        }
        return children;
    }
}
