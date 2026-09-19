package cn.aiedge.erp.sale.preorder.analytics.service;

import cn.aiedge.erp.sale.preorder.analytics.dto.PreOrderAnalysisQueryDTO;

import java.util.Map;

/**
 * 预订货查询汇总报表服务（按商品 / 按客户）
 */
public interface PreOrderAnalysisReportService {

    /**
     * 预订货汇总分页取数。
     *
     * @return {records, total, page, size, pages, summary}
     */
    Map<String, Object> analysis(PreOrderAnalysisQueryDTO query);
}
