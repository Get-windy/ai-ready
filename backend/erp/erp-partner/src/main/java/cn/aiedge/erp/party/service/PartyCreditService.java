package cn.aiedge.erp.party.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 往来单位信用服务（信用能力归 ERP 模块）。
 *
 * <p><b>归属（2026-09-26 用户裁定）</b>：信用额度与欠款是**往来单位**的属性，
 * 由 ERP 提供实现，其它模块（含 CRM）按需调用。此前实现在 CRM 的 {@code CustomerCreditService}，
 * 被 {@code erp-sales} 的销售出库以**往来单位 ID** 调用、却按 {@code crm_customer.id} 取数，
 * 属跨域 ID 混用；且 {@code calculateTotalDebt} 是恒返回 0 的桩，
 * 叠加每日定时任务会把所有客户欠款清零。本轮一并订正。</p>
 */
public interface PartyCreditService {

    /** 信用额度（未配置返回 0） */
    BigDecimal getCreditLimit(Long partyId);

    /** 当前欠款 */
    BigDecimal getCurrentDebt(Long partyId);

    /**
     * 可用信用额度 = 额度 - 欠款（下限 0）。
     *
     * <p>额度为 0 视为「未配置信用管控」，返回一个足够大的值以免把正常业务全部挡下
     * （与原 CRM 实现口径一致）。</p>
     */
    BigDecimal getAvailableCredit(Long partyId);

    /** 本单能否放行：可用额度 ≥ 本单金额 */
    boolean checkCreditAvailable(Long partyId, BigDecimal newAmount);

    /** 信用状态详情（额度 / 欠款 / 可用 / 使用率 / 状态标签） */
    Map<String, Object> getCreditStatus(Long partyId);

    /** 使用率处于 [80%, 100%) 的往来单位清单，按使用率倒序 */
    List<Map<String, Object>> getCreditWarningList();

    /** 欠款超过额度的往来单位清单，按超额倒序 */
    List<Map<String, Object>> getOverCreditList();

    /** 信用总览统计 */
    Map<String, Object> getCreditStatistics();

    /** 重算单个往来单位的欠款（=∑ 未收应收余额）并回写 */
    void recalcPartyDebt(Long partyId);

    /** 批量重算所有配置了信用额度的往来单位欠款 */
    void recalcAllPartyDebt();
}
