package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockSplit;
import cn.aiedge.erp.stock.entity.StockSplitItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockSplitService extends IService<StockSplit> {

    Page<StockSplit> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize);

    StockSplit createSplit(StockSplit split, List<StockSplitItem> items);

    StockSplit getItems(Long splitId);

    List<StockSplitItem> getItemList(Long splitId);

    StockSplit submitForApproval(Long id);

    StockSplit approve(Long id, Long approverId, String note);

    StockSplit reject(Long id, String reason);

    StockSplit execute(Long id);

    StockSplit cancel(Long id, String reason);
}
