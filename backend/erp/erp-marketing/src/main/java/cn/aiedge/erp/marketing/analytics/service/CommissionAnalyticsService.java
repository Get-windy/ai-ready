package cn.aiedge.erp.marketing.analytics.service;

import cn.aiedge.erp.marketing.analytics.dto.CommissionAnalyticsQuery;

import java.util.List;
import java.util.Map;

/**
 * 业绩提成中心（分析 → 提成分析 → 业绩提成中心，菜单 80443）· 六视图结算工作台。
 *
 * <p>视图：配送员 / 每月提成（年度矩阵）、提成构成、方案汇总提成、业绩概览、业绩明细。</p>
 */
public interface CommissionAnalyticsService {

    /** 配送员 / 每月提成：人员 × 12 月矩阵（含结算顺序约束：某月未结算则其后月份不显示） */
    Map<String, Object> riderMatrix(CommissionAnalyticsQuery query);

    /** 提成构成：配送员 × 提成方案（7 列） */
    Map<String, Object> composition(CommissionAnalyticsQuery query);

    /** 方案汇总提成：按提成方案汇总（5 列） */
    Map<String, Object> planSummary(CommissionAnalyticsQuery query);

    /** 业绩概览：配送员维度配送业绩指标（21 列） */
    Map<String, Object> performanceOverview(CommissionAnalyticsQuery query);

    /** 业绩明细：配送员 × 商品级业绩明细（21 列） */
    Map<String, Object> performanceDetail(CommissionAnalyticsQuery query);

    /** 批量结算：把指定年月的 DRAFT/CONFIRMED 提成置为 PAID（结算后锁定） */
    Map<String, Object> settle(Integer year, Integer month, List<String> riderNames, String operator);

    /** 提成方案列表（工具栏「提成方案」只读入口） */
    List<Map<String, Object>> planList();
}
