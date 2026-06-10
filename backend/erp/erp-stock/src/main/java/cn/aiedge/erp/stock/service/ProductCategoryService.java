package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 产品分类Service接口
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * 获取分类树结构
     */
    List<ProductCategory> getCategoryTree();

    /**
     * 获取子分类列表
     */
    List<ProductCategory> getChildren(Long parentId);

    /**
     * 新增分类
     */
    boolean createCategory(ProductCategory category);

    /**
     * 更新分类
     */
    boolean updateCategory(ProductCategory category);

    /**
     * 删除分类(检查是否有子节点和产品引用)
     */
    boolean deleteCategory(Long id);
}
