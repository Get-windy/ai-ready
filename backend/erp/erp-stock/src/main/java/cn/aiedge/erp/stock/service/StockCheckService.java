package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockCheck;
import cn.aiedge.erp.stock.entity.StockCheckItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface StockCheckService extends IService<StockCheck> {

    StockCheck getByCheckNo(String checkNo);

    Page<StockCheck> pageList(String keyword, Long warehouseId, Integer status, Integer checkType, int pageNum, int pageSize);

    List<StockCheck> exportList(String keyword, Long warehouseId, Integer status, Integer checkType);

    List<StockCheck> listByWarehouseId(Long warehouseId);

    String generateCheckNo();

    StockCheck createCheck(StockCheck check);

    StockCheck createCheckWithItems(Long warehouseId);

    StockCheck updateCheck(Long checkId, StockCheck check);

    StockCheck submitForApproval(Long checkId);

    StockCheck approve(Long checkId, Long approverId, String note);

    StockCheck reject(Long checkId, String reason);

    StockCheck startCheck(Long checkId);

    StockCheckItem checkItem(Long itemId, BigDecimal actualQuantity, String note);

    StockCheck completeCheck(Long checkId);

    StockCheck adjust(Long checkId);

    StockCheck cancel(Long checkId, String reason);

    List<StockCheckItem> getItems(Long checkId);

    List<StockCheckItem> getDiffItems(Long checkId);

    void calculateTotals(Long checkId);

    Integer getDiffItemCount(Long checkId);
}