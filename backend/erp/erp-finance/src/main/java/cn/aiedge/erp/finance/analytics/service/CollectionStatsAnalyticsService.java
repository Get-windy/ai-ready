package cn.aiedge.erp.finance.analytics.service;

import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;

import java.util.Map;

/**
 * 回款统计（分析 → 提成分析 → 回款统计，菜单 80442）。
 *
 * <p>对标两视图 Tab：按职员 / 按部门。三口径归集：收款金额（erp_receipt）
 * + 预收款金额（erp_pre_receipt）+ 预订货收款金额（erp_sale_pre_order.received_deposit）= 回款总金额。</p>
 */
public interface CollectionStatsAnalyticsService {

    /** 分页查询（tab=staff|dept），返回 {records,total,page,size,summary} */
    Map<String, Object> page(AnalyticsQuery query);

    /** 行级「明细」钻取：该职员/部门在三张单据表下的回款流水 */
    Map<String, Object> detail(AnalyticsQuery query);
}
