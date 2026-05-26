package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockTransfer;
import cn.aiedge.erp.stock.entity.StockTransferItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface StockTransferService extends IService<StockTransfer> {

    StockTransfer getByTransferNo(String transferNo);

    Page<StockTransfer> pageList(String keyword, Long fromWarehouseId, Long toWarehouseId, Integer status, int pageNum, int pageSize);

    List<StockTransfer> listByFromWarehouseId(Long warehouseId);

    List<StockTransfer> listByToWarehouseId(Long warehouseId);

    String generateTransferNo();

    StockTransfer createTransfer(StockTransfer transfer, List<StockTransferItem> items);

    StockTransfer submitForApproval(Long transferId);

    StockTransfer approve(Long transferId, Long approverId, String note);

    StockTransfer reject(Long transferId, String reason);

    StockTransfer execute(Long transferId);

    StockTransfer cancel(Long transferId, String reason);

    List<StockTransferItem> getItems(Long transferId);

    StockTransferItem addItem(Long transferId, StockTransferItem item);

    StockTransferItem updateItem(Long itemId, StockTransferItem item);

    void removeItem(Long itemId);

    void calculateTotals(Long transferId);
}