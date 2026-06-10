package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockDamageService extends IService<StockDamage> {
    Page<StockDamage> pageList(String keyword, Long warehouseId, Integer status, Integer damageCause, int pageNum, int pageSize);
    StockDamage createDamage(StockDamage damage, List<StockDamageItem> items);
    StockDamage submitForApproval(Long id);
    StockDamage approve(Long id, Long approverId, String note);
    StockDamage reject(Long id, String reason);
    StockDamage execute(Long id);
    StockDamage cancel(Long id, String reason);
    List<StockDamageItem> getItems(Long damageId);
}
