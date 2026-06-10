package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockBom;
import cn.aiedge.erp.stock.entity.StockBomItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockBomService extends IService<StockBom> {

    Page<StockBom> pageList(String keyword, Long productId, Integer status, int pageNum, int pageSize);

    StockBom createBom(StockBom bom, List<StockBomItem> items);

    StockBom updateBom(Long id, StockBom bom, List<StockBomItem> items);

    List<StockBomItem> getItems(Long bomId);

    StockBom enableBom(Long id);

    StockBom disableBom(Long id);
}
