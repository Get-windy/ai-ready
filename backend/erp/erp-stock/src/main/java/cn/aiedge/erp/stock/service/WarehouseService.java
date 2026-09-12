package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.WarehouseQuery;
import cn.aiedge.erp.stock.entity.Warehouse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 仓库Service接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface WarehouseService extends IService<Warehouse> {

    /**
     * 获取仓库列表
     */
    List<Warehouse> getWarehouseList();

    /**
     * 仓库规划分页查询（对标：筛选条件 / 显示停用 / 显示层次结构）
     */
    Page<Warehouse> pageWarehouse(WarehouseQuery query);

    /**
     * 导出用全量列表（不分页）
     */
    List<Warehouse> listForExport(WarehouseQuery query);

    /**
     * 仓库详情
     */
    Warehouse getWarehouseDetail(Long id);

    /**
     * 新增仓库
     */
    Warehouse createWarehouse(Warehouse warehouse);

    /**
     * 修改仓库
     */
    boolean updateWarehouse(Warehouse warehouse);

    /**
     * 启用/停用
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 删除仓库（存在库存引用时拒绝）
     */
    boolean removeWarehouse(Long id);

    /**
     * 生成下一个仓库编号（ck + 3 位序号）
     */
    String nextCode();
}
