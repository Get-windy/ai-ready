package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.TradeAnalysisDTO;

/**
 * 商城交易分析服务
 */
public interface MallTradeAnalysisService {

    /**
     * 交易分析: 按日分组(订单数/GMV/客单价) + 汇总 + 支付状态分布
     *
     * @param startDate 开始日期 (yyyy-MM-dd, 可空)
     * @param endDate   结束日期 (yyyy-MM-dd, 可空)
     */
    TradeAnalysisDTO tradeAnalysis(String startDate, String endDate);
}
