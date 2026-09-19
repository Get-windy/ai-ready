package cn.aiedge.erp.purchase.analytics.service;

import cn.aiedge.erp.purchase.analytics.dto.PurchaseAnalysisQueryDTO;

import java.util.Map;

/**
 * 采购分析报表服务（分析 → 采销分析 → 采购分析，三维度 Tab）
 */
public interface PurchaseAnalysisReportService {

    /**
     * 采购分析分页取数（按时间 / 按商品 / 按供应商）。
     *
     * @return {records, total, page, size, pages, summary}
     */
    Map<String, Object> analysis(PurchaseAnalysisQueryDTO query);
}
