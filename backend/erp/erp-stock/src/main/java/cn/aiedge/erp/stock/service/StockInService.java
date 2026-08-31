package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockInItemVO;
import cn.aiedge.erp.stock.dto.StockInQuery;
import cn.aiedge.erp.stock.entity.StockIn;
import cn.aiedge.erp.stock.entity.StockInItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockInService extends IService<StockIn> {
    String generateNo();

    Page<StockIn> pageList(StockInQuery query);

    Page<StockInItemVO> pageDetail(StockInQuery query);

    StockIn getDetail(Long id);

    List<StockInItem> getItems(Long stockInId);

    StockIn createStockIn(StockIn stockIn, List<StockInItem> items);

    StockIn updateStockIn(Long id, StockIn stockIn, List<StockInItem> items);

    StockIn submitForApproval(Long id);

    StockIn approve(Long id, Long approverId, String note);

    StockIn reject(Long id, String reason);

    StockIn execute(Long id);

    StockIn cancel(Long id, String reason);
}
