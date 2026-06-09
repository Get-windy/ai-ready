package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.StockReplenishment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StockReplenishmentService extends IService<StockReplenishment> {

    /**
     * 分页查询补货建议
     */
    Page<StockReplenishment> pageList(String keyword, String priority, String status, int pageNum, int pageSize);

    /**
     * 生成补货建议
     */
    List<StockReplenishment> generateSuggestions();

    /**
     * 根据补货建议创建采购订单
     */
    StockReplenishment createOrder(Long suggestionId, Long supplierId);

    /**
     * 忽略补货建议
     */
    StockReplenishment ignoreSuggestion(Long id, String reason);
}
