package cn.aiedge.erp.stock.analytics.service;

import cn.aiedge.erp.stock.analytics.dto.InventoryAnalysisQueryDTO;

import java.util.Map;

/**
 * 进销存分析报表服务（按商品 / 仓库调拨分析 / 商品调拨分析）
 */
public interface InventoryAnalysisReportService {

    /**
     * 进销存分析分页取数。
     *
     * @return {records, total, page, size, pages, summary}
     */
    Map<String, Object> analysis(InventoryAnalysisQueryDTO query);
}
