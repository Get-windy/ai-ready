package cn.aiedge.base.credit;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 「取双方约定的账期天数」的查询入参：<b>卖方（债权方）+ 买方（债务方）+ 租户对 + 业务时点</b>。
 *
 * <h3>为什么必须带业务时点</h3>
 * 协议口径是"<b>下单/发货那一刻生效的那一版</b>"（DOMAIN-MODEL ㉛）。不许按"当前版本"取数，
 * 否则今天改一次账期，昨天已经发生的应收就会被**追溯改口径**。所以 {@code businessDate}
 * 是必填项，没有"不传就用今天"的方便。
 *
 * <h3>为什么租户对两个都不能为空</h3>
 * 协议是<b>跨租户</b>契约（两端各属一个租户），"可见性 / 生效性"完全靠
 * {@code party_a_tenant_id / party_b_tenant_id} 这对条件判定。少一端就等于
 * "谁都能看到"，是跨租户数据泄露。故两端租户在这里就拒绝为空。
 *
 * <h3>为什么主体 ID 允许为空（且这不是"编一个主体"）</h3>
 * 本仓现状（2026-09-22 实测）：单据上只有<b>对方</b>主体（应收是客户、应付是供应商，
 * 取值就是 {@code biz_party.id}），而"<b>本租户自己的主体</b>"在系统里<b>没有登记</b>
 * （{@code sys_tenant} 没有 party 外键，{@code biz_party} 也没有 SELF 约定）。
 * 与其硬编一个主体去查协议（那会查错人的商业条款，比查不到危险得多），
 * 不如如实留空 —— 协议侧会退化成"只按租户对 + 已知那一端主体"定位协议，
 * 方向核对则依据"债务方在协议哪一端"反推债权方是哪一端。
 *
 * <p>两端主体都为空的查询是<b>无意义</b>的（无法核对账期方向），
 * 调用方应自行判定"身份不足"并直接走"无适用协议"分支，不要构造这种查询。</p>
 *
 * @see TradeCreditTermProvider
 * @see CreditTermResult
 */
public final class CreditTermQuery {

    private final Long sellerTenantId;
    private final Long sellerPartyId;
    private final Long buyerTenantId;
    private final Long buyerPartyId;
    private final LocalDate businessDate;
    private final String usage;

    private CreditTermQuery(Long sellerTenantId, Long sellerPartyId,
                            Long buyerTenantId, Long buyerPartyId,
                            LocalDate businessDate, String usage) {
        this.sellerTenantId = sellerTenantId;
        this.sellerPartyId = sellerPartyId;
        this.buyerTenantId = buyerTenantId;
        this.buyerPartyId = buyerPartyId;
        this.businessDate = businessDate;
        this.usage = usage;
    }

    /**
     * 构造查询。
     *
     * @param sellerTenantId 卖方（债权方/收款方）所属租户，不可为空
     * @param sellerPartyId  卖方主体（{@code biz_party.id}）；本仓暂无"本租户主体"登记，可为空
     * @param buyerTenantId  买方（债务方/付款方）所属租户，不可为空
     * @param buyerPartyId   买方主体（{@code biz_party.id}）；对方是客户时即 {@code customer_id}，可为空
     * @param businessDate   **业务日期**（起算基准日，不是"当前时间"），不可为空
     * @param usage          这个值要拿去做什么（会拼进"未约定"的原因文案，让人一眼知道该怎么办）
     */
    public static CreditTermQuery of(Long sellerTenantId, Long sellerPartyId,
                                     Long buyerTenantId, Long buyerPartyId,
                                     LocalDate businessDate, String usage) {
        if (sellerTenantId == null || buyerTenantId == null) {
            throw new IllegalArgumentException("取协议账期必须提供卖方与买方两端所属租户："
                    + "协议是跨租户契约，缺任何一端都无法确定按哪一份协议执行");
        }
        if (businessDate == null) {
            throw new IllegalArgumentException("取协议账期必须提供业务日期（起算基准日）："
                    + "不许按「当前时间」取数，否则改协议会篡改已发生交易的口径");
        }
        return new CreditTermQuery(sellerTenantId, sellerPartyId, buyerTenantId, buyerPartyId,
                businessDate, usage);
    }

    public Long getSellerTenantId() {
        return sellerTenantId;
    }

    public Long getSellerPartyId() {
        return sellerPartyId;
    }

    public Long getBuyerTenantId() {
        return buyerTenantId;
    }

    public Long getBuyerPartyId() {
        return buyerPartyId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public String getUsage() {
        return usage;
    }

    /** 两端主体都没有 ⇒ 无法核对账期方向，调用方应直接按"无适用协议"处理，不必发起查询。 */
    public boolean hasNoPartyAtAll() {
        return sellerPartyId == null && buyerPartyId == null;
    }

    /** 业务时点（协议按"哪一版的区间覆盖它"挑选）：业务日期当日零点。 */
    public java.time.LocalDateTime businessTime() {
        return businessDate.atStartOfDay();
    }

    @Override
    public String toString() {
        return "CreditTermQuery{卖方租户=" + sellerTenantId + ", 卖方主体=" + sellerPartyId
                + ", 买方租户=" + buyerTenantId + ", 买方主体=" + buyerPartyId
                + ", 业务日期=" + businessDate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreditTermQuery other)) {
            return false;
        }
        return Objects.equals(sellerTenantId, other.sellerTenantId)
                && Objects.equals(sellerPartyId, other.sellerPartyId)
                && Objects.equals(buyerTenantId, other.buyerTenantId)
                && Objects.equals(buyerPartyId, other.buyerPartyId)
                && Objects.equals(businessDate, other.businessDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellerTenantId, sellerPartyId, buyerTenantId, buyerPartyId, businessDate);
    }
}
