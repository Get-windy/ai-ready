package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockAssemble;
import cn.aiedge.erp.stock.entity.StockAssembleItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockAssembleService extends IService<StockAssemble> {

    Page<StockAssemble> pageList(String keyword, Long warehouseId, Integer status, int pageNum, int pageSize);

    StockAssemble createAssemble(StockAssemble assemble, List<StockAssembleItem> items);

    StockAssemble getItems(Long assembleId);

    List<StockAssembleItem> getItemList(Long assembleId);

    StockAssemble submitForApproval(Long id);

    StockAssemble approve(Long id, Long approverId, String note);

    StockAssemble reject(Long id, String reason);

    StockAssemble execute(Long id);

    StockAssemble cancel(Long id, String reason);
}
