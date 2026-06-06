package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Warehouse;
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
}
