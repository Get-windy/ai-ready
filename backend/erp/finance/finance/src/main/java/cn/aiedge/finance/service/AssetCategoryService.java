package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AssetCategory;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AssetCategoryService extends IService<AssetCategory> {
    
    List<AssetCategory> listAllEnabled(Long tenantId);
    
    List<AssetCategory> listByParentId(Long tenantId, Long parentId);
    
    AssetCategory getByCode(Long tenantId, String categoryCode);
    
    Page<AssetCategory> pageList(Long tenantId, String categoryCode, String categoryName, Page<AssetCategory> page);
    
    boolean createCategory(AssetCategory category);
    
    boolean updateCategory(AssetCategory category);
    
    boolean deleteCategory(Long tenantId, Long categoryId);
    
    List<AssetCategory> buildTree(Long tenantId);
}