package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockDamageItemVO;
import cn.aiedge.erp.stock.dto.StockDamageQuery;
import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockDamageService extends IService<StockDamage> {
    String generateNo();
    Page<StockDamage> pageList(StockDamageQuery query);
    Page<StockDamageItemVO> pageDetail(StockDamageQuery query);
    StockDamage getDetail(Long id);
    List<StockDamageItem> getItems(Long damageId);
    StockDamage createStockDamage(StockDamage damage, List<StockDamageItem> items);
    StockDamage updateStockDamage(Long id, StockDamage damage, List<StockDamageItem> items);
    StockDamage execute(Long id);
    StockDamage cancel(Long id, String reason);
}
