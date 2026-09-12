package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.WarehouseCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 仓库分类Service
 *
 * @author AI-Ready Team
 * @since 11.156.0
 */
public interface WarehouseCategoryService extends IService<WarehouseCategory> {

    /** 分类树（含仓库数量统计） */
    List<WarehouseCategory> getCategoryTree();

    /** 新增分类（自动计算层级） */
    WarehouseCategory createCategory(WarehouseCategory category);

    /** 修改分类 */
    boolean updateCategory(WarehouseCategory category);

    /** 删除分类（校验子分类与仓库引用） */
    boolean removeCategory(Long id);
}
