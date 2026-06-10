package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockOverflow;
import cn.aiedge.erp.stock.entity.StockOverflowItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockOverflowService extends IService<StockOverflow> {
    Page<StockOverflow> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize);
    StockOverflow createOverflow(StockOverflow overflow, List<StockOverflowItem> items);
    StockOverflow submitForApproval(Long id);
    StockOverflow approve(Long id, Long approverId, String note);
    StockOverflow reject(Long id, String reason);
    StockOverflow execute(Long id);
    StockOverflow cancel(Long id, String reason);
    List<StockOverflowItem> getItems(Long overflowId);
}
