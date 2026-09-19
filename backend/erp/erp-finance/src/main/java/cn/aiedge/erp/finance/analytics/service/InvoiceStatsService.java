package cn.aiedge.erp.finance.analytics.service;

import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;

import java.util.Map;

/**
 * 发票统计（分析 → 财务分析 → 发票统计，菜单 80455）。
 *
 * <p>增值税进销项月度台账：行 = 年度 + 月份，两组多级表头
 * （销项开票 7 叶子 / 取得进项专票 7 叶子）+ 首列「抵扣后应交税额」= 销项税额 − 进项税额。</p>
 */
public interface InvoiceStatsService {

    /** 分页查询，返回 {records,total,page,size,summary} */
    Map<String, Object> page(AnalyticsQuery query);
}
