package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockOutItemVO;
import cn.aiedge.erp.stock.dto.StockOutQuery;
import cn.aiedge.erp.stock.entity.StockOut;
import cn.aiedge.erp.stock.entity.StockOutItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockOutService extends IService<StockOut> {
    String generateNo();

    Page<StockOut> pageList(StockOutQuery query);

    Page<StockOutItemVO> pageDetail(StockOutQuery query);

    StockOut getDetail(Long id);

    List<StockOutItem> getItems(Long stockOutId);

    StockOut createStockOut(StockOut stockOut, List<StockOutItem> items);

    StockOut updateStockOut(Long id, StockOut stockOut, List<StockOutItem> items);

    StockOut submitForApproval(Long id);

    StockOut approve(Long id, Long approverId, String note);

    StockOut reject(Long id, String reason);

    StockOut execute(Long id);

    StockOut cancel(Long id, String reason);
}
