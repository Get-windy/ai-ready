package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockTakeItemVO;
import cn.aiedge.erp.stock.dto.StockTakeQuery;
import cn.aiedge.erp.stock.dto.StockTakeUncheckedVO;
import cn.aiedge.erp.stock.entity.StockTake;
import cn.aiedge.erp.stock.entity.StockTakeItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockTakeService extends IService<StockTake> {
    String generateNo();

    Page<StockTake> pageList(StockTakeQuery query);

    Page<StockTakeItemVO> pageDetail(StockTakeQuery query);

    StockTake getDetail(Long id);

    List<StockTakeItem> getItems(Long stockTakeId);

    StockTake createStockTake(StockTake stockTake, List<StockTakeItem> items);

    StockTake updateStockTake(Long id, StockTake stockTake, List<StockTakeItem> items);

    /**
     * 盘点处理：按盈亏生成报损单(盘亏)/报溢单(盘盈)并回写库存
     */
    StockTake process(Long id);

    /**
     * 未盘商品查询：按仓库列出库存商品，排除已进入盘点单明细的商品
     */
    List<StockTakeUncheckedVO> uncheckedProducts(StockTakeQuery query);
}
