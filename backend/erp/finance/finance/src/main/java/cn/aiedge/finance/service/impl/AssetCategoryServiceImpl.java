package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AssetCategory;
import cn.aiedge.finance.mapper.AssetCategoryMapper;
import cn.aiedge.finance.service.AssetCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetCategoryServiceImpl extends ServiceImpl<AssetCategoryMapper, AssetCategory> implements AssetCategoryService {
    
    @Override
    public List<AssetCategory> listAllEnabled(Long tenantId) {
        return baseMapper.listAllEnabled(tenantId);
    }
    
    @Override
    public List<AssetCategory> listByParentId(Long tenantId, Long parentId) {
        return baseMapper.listByParentId(tenantId, parentId);
    }
    
    @Override
    public AssetCategory getByCode(Long tenantId, String categoryCode) {
        return baseMapper.getByCode(tenantId, categoryCode);
    }
    
    @Override
    public Page<AssetCategory> pageList(Long tenantId, String categoryCode, String categoryName, Page<AssetCategory> page) {
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetCategory::getTenantId, tenantId)
               .eq(AssetCategory::getDeleted, 0);
        if (categoryCode != null && !categoryCode.isEmpty()) {
            wrapper.like(AssetCategory::getCategoryCode, categoryCode);
        }
        if (categoryName != null && !categoryName.isEmpty()) {
            wrapper.like(AssetCategory::getCategoryName, categoryName);
        }
        wrapper.orderByAsc(AssetCategory::getCategoryCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createCategory(AssetCategory category) {
        AssetCategory existing = this.getByCode(category.getTenantId(), category.getCategoryCode());
        if (existing != null) {
            throw new RuntimeException("分类编码已存在");
        }
        category.setEnabled(1);
        return this.save(category);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategory(AssetCategory category) {
        return this.updateById(category);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long tenantId, Long categoryId) {
        Integer childCount = baseMapper.countChildren(tenantId, categoryId);
        if (childCount > 0) {
            throw new RuntimeException("存在下级分类，不能删除");
        }
        return this.removeById(categoryId);
    }
    
    @Override
    public List<AssetCategory> buildTree(Long tenantId) {
        List<AssetCategory> allCategories = this.listAllEnabled(tenantId);
        Map<Long, AssetCategory> categoryMap = new HashMap<>();
        List<AssetCategory> rootCategories = new ArrayList<>();
        for (AssetCategory category : allCategories) {
            categoryMap.put(category.getId(), category);
        }
        for (AssetCategory category : allCategories) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                rootCategories.add(category);
            } else {
                AssetCategory parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(category);
                }
            }
        }
        return rootCategories;
    }
}