package cn.aiedge.erp.sale.analytics.service;

import cn.aiedge.erp.sale.analytics.dto.PromotionFunnelQueryDTO;

import java.util.Map;

/**
 * 推广分析（职员分享推广漏斗）报表服务
 */
public interface PromotionFunnelReportService {

    /**
     * 职员维度推广漏斗分页取数。
     *
     * @return {records, total, page, size, pages, summary}
     */
    Map<String, Object> page(PromotionFunnelQueryDTO query);
}
