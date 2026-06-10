package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockCostAdjustService extends IService<StockCostAdjust> {

    Page<StockCostAdjust> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize);

    StockCostAdjust createAdjust(StockCostAdjust adjust, List<StockCostAdjustItem> items);

    StockCostAdjust submitForApproval(Long id);

    StockCostAdjust approve(Long id, Long approverId, String note);

    StockCostAdjust reject(Long id, String reason);

    StockCostAdjust execute(Long id);

    StockCostAdjust cancel(Long id, String reason);

    List<StockCostAdjustItem> getItems(Long adjustId);
}
