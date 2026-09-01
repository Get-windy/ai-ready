package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockCostAdjustItemVO;
import cn.aiedge.erp.stock.dto.StockCostAdjustQuery;
import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockCostAdjustService extends IService<StockCostAdjust> {

    String generateNo();

    Page<StockCostAdjust> pageList(StockCostAdjustQuery query);

    Page<StockCostAdjustItemVO> pageDetail(StockCostAdjustQuery query);

    StockCostAdjust getDetail(Long id);

    StockCostAdjust createAdjust(StockCostAdjust adjust, List<StockCostAdjustItem> items);

    StockCostAdjust updateAdjust(Long id, StockCostAdjust adjust, List<StockCostAdjustItem> items);

    StockCostAdjust submitForApproval(Long id);

    StockCostAdjust approve(Long id, Long approverId, String note);

    StockCostAdjust reject(Long id, String reason);

    StockCostAdjust execute(Long id);

    StockCostAdjust cancel(Long id, String reason);

    List<StockCostAdjustItem> getItems(Long adjustId);
}
