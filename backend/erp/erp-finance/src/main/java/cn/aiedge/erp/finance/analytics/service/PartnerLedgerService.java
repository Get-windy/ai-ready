package cn.aiedge.erp.finance.analytics.service;

import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 往来余额表（分析 → 财务分析 → 往来余额表，菜单 80459）。
 *
 * <p>四象限（应收 / 预收 / 应付 / 预付）各给 期初 / 本期发生 / 期末 三列，
 * 末组 {@code 往来合计 = （应收 + 预付） − （预收 + 应付）}（正 = 对方欠我）。</p>
 */
public interface PartnerLedgerService {

    /** 分页查询，返回 {records,total,page,size,summary} */
    Map<String, Object> page(AnalyticsQuery query);

    /** 行级「对账」：该结算单位四象限来源流水 */
    Map<String, Object> detail(AnalyticsQuery query);

    /** 行级「清账」：应收与应付对冲（经会计凭证，不直改余额） */
    Map<String, Object> reconcile(String partnerName, BigDecimal amount, String remark);

    /** 工具栏「清账历史」：历史清账单（按单号归并应收/应付两侧） */
    Map<String, Object> reconcileHistory(Integer page, Integer size);
}
