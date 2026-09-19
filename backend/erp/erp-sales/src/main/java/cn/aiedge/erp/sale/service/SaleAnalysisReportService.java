package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.SaleAnalysisReportQueryDTO;

import java.util.Map;

/**
 * 销售分析组报表服务（销售业绩 / 销售分析 / 销售履约分析 / 销售欠款分析）
 *
 * <p>四个页面共用同一套「订货 → 销售 → 退货 → 回款」取数引擎，按传入 {@code tab} 切换到对应聚合维度。</p>
 */
public interface SaleAnalysisReportService {

    /**
     * 销售业绩（漏斗口径）：按时间 / 按职员
     */
    Map<String, Object> salesPerformance(SaleAnalysisReportQueryDTO query);

    /**
     * 销售分析（量本利口径）：按时间 / 商品 / 品牌 / 客户 / 区域 / 仓库 / 职员 / 来源
     */
    Map<String, Object> salesAnalysis(SaleAnalysisReportQueryDTO query);

    /**
     * 销售履约分析（发货履约 + 五金额口径）：按单据 / 按客户
     */
    Map<String, Object> salesFulfillment(SaleAnalysisReportQueryDTO query);

    /**
     * 销售欠款分析（欠款滚动 + 超期账龄）：按职员 / 按客户 / 按区域
     */
    Map<String, Object> salesDebt(SaleAnalysisReportQueryDTO query);
}
