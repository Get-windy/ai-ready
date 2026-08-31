package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockTransferCreateDTO;
import cn.aiedge.erp.stock.dto.StockTransferItemVO;
import cn.aiedge.erp.stock.dto.StockTransferQuery;
import cn.aiedge.erp.stock.entity.StockTransfer;
import cn.aiedge.erp.stock.entity.StockTransferItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockTransferService extends IService<StockTransfer> {

    StockTransfer getByTransferNo(String transferNo);

    Page<StockTransfer> pageList(StockTransferQuery query);

    Page<StockTransferItemVO> pageDetail(StockTransferQuery query);

    StockTransfer getDetail(Long id);

    Page<StockTransfer> pageList(String keyword, Long fromWarehouseId, Long toWarehouseId, Integer status, int pageNum, int pageSize);

    List<StockTransfer> exportList(StockTransferQuery query);

    List<StockTransfer> listByFromWarehouseId(Long warehouseId);

    List<StockTransfer> listByToWarehouseId(Long warehouseId);

    String generateTransferNo();

    StockTransfer createTransfer(StockTransferCreateDTO dto);

    StockTransfer updateTransfer(Long transferId, StockTransferCreateDTO dto);

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
