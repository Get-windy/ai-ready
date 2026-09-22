package cn.aiedge.agreement.provider;

import cn.aiedge.agreement.domain.AgreementResolvedSettings;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.domain.AgreementSettingValue;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.base.credit.CreditTermQuery;
import cn.aiedge.base.credit.CreditTermResult;
import cn.aiedge.base.credit.SettlementType;
import cn.aiedge.base.credit.TradeCreditTermProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@link TradeCreditTermProvider} 的**协议模块实现**：把"双方约定的结算口径"喂给财务的应收/应付到期日。
 *
 * <p>这是 DOMAIN-MODEL §13.1「字段设定版是<b>运行时配置</b>、必须被下游消费」的
 * <b>第一处真实落地</b>：协议不再只是存下来的一份文档，而是真的会改变钱流
 * （同一张销售出库单，签了 45 天账期的协议与没签，生成的应收到期日不同）。</p>
 *
 * <h3>本类只做三件事，其余一律交给 {@link AgreementRuntime}</h3>
 * <ol>
 *   <li><b>取那一版的设定</b>：带业务时点调用 {@code AgreementRuntime.resolve(...)}
 *       —— 绝不自己写查询、也绝不读"当前版本"（㉛）。</li>
 *   <li><b>按两层判定翻译成 {@link CreditTermResult} 的四态</b>（见下）。</li>
 *   <li><b>核对账期方向</b>：账期是<b>有向</b>的（{@code AR_CREDIT_PROVIDER} = 谁给谁账期）。
 *       本笔账的债权方必须是协议约定的"账期提供方"，否则这份协议说的不是这笔账的账期。</li>
 * </ol>
 *
 * <h3>⚠️ 两层判定（§13.3 补充口径 3 —— 顺序反了就会出"约定现金却报未约定账期"的老毛病）</h3>
 * <b>第 1 层：先取「结算方式」</b>（{@code SETTLEMENT_TYPE}，账期天数的<b>前置字段</b>）
 * <table border="1">
 *   <tr><th>第 1 层</th><th>结论</th></tr>
 *   <tr><td>无生效版本（无协议 / <b>已到期</b>）</td>
 *       <td>{@code NO_AGREEMENT} —— 走「无协议无特定客户模式」</td></tr>
 *   <tr><td>有协议但<b>没约定结算方式</b> / 取值认不出</td>
 *       <td>{@code NO_AGREEMENT} —— 同上（现款现结是低风险缺省）</td></tr>
 *   <tr><td>约定了结算方式</td><td>进第 2 层</td></tr>
 * </table>
 *
 * <b>第 2 层：约定的结算方式是哪一种</b>
 * <table border="1">
 *   <tr><th>结算方式</th><th>结论</th><th>账期天数</th></tr>
 *   <tr><td>现款现货（先款后货 / 现款现结 / 货到付款）、滚结</td>
 *       <td>{@code NO_CREDIT_TERM}（<b>正常结论</b>，单据照常提交）</td>
 *       <td><b>压根不问</b></td></tr>
 *   <tr><td>账期结算</td><td>天数 + 方向齐全且方向一致 ⇒ {@code AGREED}；
 *       任一缺失 / 天数非法 / 方向对不上 ⇒ {@code UNDECLARED}（消费方<b>拒单</b>）</td>
 *       <td><b>条件必填</b></td></tr>
 * </table>
 *
 * <h3>⚠️ 为什么"方向对不上"归到「未约定」而不是「无适用协议」</h3>
 * 「无适用协议」的处置是走<b>现款现结</b>。若把"协议约定了反向账期"也归到那里，
 * 等于把双方明确表达过的方向意思静默丢掉；归到「未约定」的处置是<b>挂人工</b>
 * （现行口径下即"单据提交处拒单"）—— 不自动执行、也不自作主张，
 * 与 §3.4.4d1「未约定就不自动执行」同一口径。</p>
 *
 * <h3>⚠️ 本类绝不抛业务异常</h3>
 * 调用方正在开单。任何取数失败（协议侧数据异常、两端主体都缺失等）都必须降级成
 * {@link CreditTermResult#undeclared(String, Long, Long, Integer)} 或
 * {@link CreditTermResult#noAgreement(String)}，并带上中文原因，
 * 而不是把单据拦下来 —— <b>要不要拒单是消费方在提交处的事</b>（只对 {@code UNDECLARED} 拒）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementCreditTermProvider implements TradeCreditTermProvider {

    /**
     * 结算方式（本笔按现款、滚结还是账期结算）。
     *
     * <p>它是<b>账期天数的前置字段</b>：没有它，系统不知道本笔该不该问天数（§13.3 铁律①）。
     * 定义见 {@code agreement_setting_def}，消费方 = {@code AR_DUE_DATE}。</p>
     */
    public static final String KEY_SETTLEMENT_TYPE = "SETTLEMENT_TYPE";

    /** 账期天数（单位：天）。定义见 {@code agreement_setting_def}，消费方 = {@code AR_DUE_DATE}。 */
    public static final String KEY_CREDIT_DAYS = "AR_CREDIT_DAYS";

    /** 账期方向（谁给谁账期）：{@code PARTY_A} / {@code PARTY_B}。与账期天数配套使用。 */
    public static final String KEY_CREDIT_PROVIDER = "AR_CREDIT_PROVIDER";

    private static final String DIRECTION_PARTY_A = "PARTY_A";
    private static final String DIRECTION_PARTY_B = "PARTY_B";

    private final AgreementRuntime runtime;

    /**
     * 只用来读协议主档的两端主体 ID（核对账期方向）。
     * {@link AgreementResolvedSettings} 只带"是哪一份协议"，不带"两端分别是谁"。
     */
    private final AgreementMapper agreementMapper;

    @Override
    public CreditTermResult resolve(CreditTermQuery query) {
        AgreementResolvedSettings settings = runtime.resolve(
                query.getSellerPartyId(), query.getSellerTenantId(),
                query.getBuyerPartyId(), query.getBuyerTenantId(),
                query.businessTime());

        // ① 该业务时点没有生效版本（未签生效 / 已终止 / 业务时点不在有效期区间）⇒ 与"无适用协议"同处理。
        //    消费方据此走「无协议无特定客户模式」，零行为变化。
        if (!settings.isVersionFound()) {
            return CreditTermResult.noAgreement(settings.getAbsenceReason());
        }

        Long agreementId = settings.getAgreementId();
        Long versionId = settings.getVersionId();
        Integer versionNo = settings.getVersionNo();
        String at = "（协议 " + agreementId + " 第 " + versionNo + " 版）";

        // ② 第 1 层：先取「结算方式」。它是账期天数的前置字段 —— 没有它，下面第 2 层无从判定。
        //    没约定 ⇒ 「无协议无特定客户模式」⇒ 现款现结（钱货两清、零信用敞口，是低风险缺省）。
        AgreementSettingValue settlementValue = settings.get(KEY_SETTLEMENT_TYPE);
        if (!settlementValue.isAgreed()) {
            return CreditTermResult.noAgreement(
                    settlementValue.describeAbsence("判定本笔按现款、滚结还是账期结算") + at
                            + "；本笔按无协议无特定客户模式处理：现款现结（不赊账、到期日为业务日）");
        }
        SettlementType settlementType = SettlementType.parseOrNull(settlementValue.requireText());
        if (settlementType == null) {
            // 认不出的取值是平台/数据问题，绝不能就此给双方记一笔赊账 ⇒ 落到最低风险的现款现结。
            return CreditTermResult.noAgreement(
                    "本协议「" + settlementValue.getLabel() + "」的取值「" + settlementValue.requireText()
                            + "」不是有效的结算方式，无法判定本笔有无账期" + at
                            + "；本笔按无协议无特定客户模式处理：现款现结");
        }

        // ③ 第 2 层：现款现货（三种）与滚结本就没有账期这回事 ⇒ 压根不问账期天数。
        if (!settlementType.requiresCreditDays()) {
            log.info("协议约定的结算方式无账期: 协议={}, 版本={}, 结算方式={}, 用途={}",
                    agreementId, versionNo, settlementType, usageOf(query));
            return CreditTermResult.noCreditTerm(settlementType,
                    "本协议约定的结算方式是「" + settlementType.getLabel() + "」，本笔没有账期这回事" + at,
                    agreementId, versionId, versionNo);
        }

        // ④ 账期结算 ⇒ 账期天数是**条件必填**：缺 ⇒ UNDECLARED ⇒ 消费方在提交处拒单。
        AgreementSettingValue daysValue = settings.get(KEY_CREDIT_DAYS);
        if (!daysValue.isAgreed()) {
            return CreditTermResult.undeclared(
                    daysValue.describeAbsence(usageOf(query))
                            + "（结算方式为「账期结算」时账期天数是必填项）" + at,
                    agreementId, versionId, versionNo);
        }

        // ⑤ 账期方向同样是条件必填：没有它，无法判断这笔账的到期方是谁。有天数也不能执行（有向才成立）。
        AgreementSettingValue providerValue = settings.get(KEY_CREDIT_PROVIDER);
        if (!providerValue.isAgreed()) {
            return CreditTermResult.undeclared(
                    providerValue.describeAbsence("判断" + usageOf(query) + "的账期方向")
                            + "（结算方式为「账期结算」时账期方向是必填项）" + at,
                    agreementId, versionId, versionNo);
        }

        // ⑥ 天数必须是有效的非负整数天：45.5 天这种值无法推算日期，按缺项处理（挂人工），不四舍五入。
        BigDecimal days = daysValue.requireNumber();
        if (days.signum() < 0 || days.stripTrailingZeros().scale() > 0) {
            return CreditTermResult.undeclared(
                    "本协议「" + daysValue.getLabel() + "」的值不是有效的非负整数天数（实际 "
                            + days.stripTrailingZeros().toPlainString() + "），无法推算到期日，需人工确认" + at,
                    agreementId, versionId, versionNo);
        }

        // ⑦ 核对方向：本笔账的债权方（收钱的那一方）必须就是协议约定的账期提供方。
        String mismatch = checkDirection(settings.getAgreementId(), query, providerValue.requireText());
        if (mismatch != null) {
            return CreditTermResult.undeclared(mismatch + at,
                    agreementId, versionId, versionNo);
        }

        log.info("协议账期已命中: 协议={}, 版本={}, 账期={}天, 业务日期={}, 用途={}",
                agreementId, versionNo, days.intValue(), query.getBusinessDate(), usageOf(query));
        return CreditTermResult.agreed(days.intValue(), agreementId, versionId, versionNo);
    }

    // ══════════════════════ 内部 ══════════════════════

    /**
     * 账期方向核对：账期提供方必须是本笔账的<b>债权方</b>（收钱那一方）。
     *
     * <p>调用方通常只拿得到一端主体（应收=客户是债务方、应付=供应商是债权方），
     * 所以这里按"已知那一端"反推另一端：</p>
     * <ul>
     *   <li>已知债权方 ⇒ 它必须就是账期提供方；</li>
     *   <li>已知债务方 ⇒ 它<b>不能</b>是账期提供方（提供方是协议的另一端）。</li>
     * </ul>
     *
     * @return {@code null} 表示方向一致；否则返回中文不一致原因
     */
    private String checkDirection(Long agreementId, CreditTermQuery query, String declaredDirection) {
        String direction = declaredDirection == null ? "" : declaredDirection.trim();
        boolean wantPartyA = DIRECTION_PARTY_A.equals(direction);
        boolean wantPartyB = DIRECTION_PARTY_B.equals(direction);
        if (!wantPartyA && !wantPartyB) {
            return "本协议「账期方向」的取值不是有效的 PARTY_A / PARTY_B（实际「" + declaredDirection
                    + "」），无法判断账期由谁提供";
        }

        Agreement agreement = agreementMapper.selectById(agreementId);
        if (agreement == null) {
            return "本协议主档已不存在（agreementId=" + agreementId + "），无法核对账期方向";
        }
        Long providerPartyId = wantPartyA ? agreement.getPartyAId() : agreement.getPartyBId();
        if (providerPartyId == null) {
            // 消费者单方承诺场景：账期提供方那一端是"不特定消费者"，没有主体可核对。
            return "本协议约定的账期提供方（" + (wantPartyA ? "甲方" : "乙方")
                    + "）不是具体主体，无法核对这笔账的账期方向";
        }

        Long creditor = query.getSellerPartyId();
        if (creditor != null) {
            return creditor.equals(providerPartyId)
                    ? null
                    : "本协议约定的账期是由" + sideLabel(agreement, providerPartyId)
                    + "提供给对方的，本笔交易的债权方（卖方）不是该主体，方向对不上";
        }

        Long debtor = query.getBuyerPartyId();
        if (debtor == null) {
            return "调用方未提供任何一端的往来主体，无法核对账期方向";
        }
        boolean debtorIsPartyA = debtor.equals(agreement.getPartyAId());
        boolean debtorIsPartyB = agreement.getPartyBId() != null && debtor.equals(agreement.getPartyBId());
        if (debtorIsPartyA == debtorIsPartyB) {
            // 两端都命中或都不命中：数据对不上，不猜
            return "本笔交易的债务方（买方）与协议两端主体都对不上，无法核对账期方向";
        }
        // 债务方在甲方 ⇒ 债权方应在乙方，账期提供方就必须是乙方；反之亦然
        Long expectedProviderPartyId = debtorIsPartyA ? agreement.getPartyBId() : agreement.getPartyAId();
        if (expectedProviderPartyId == null || !providerPartyId.equals(expectedProviderPartyId)) {
            return "本协议约定的账期是由" + sideLabel(agreement, providerPartyId)
                    + "提供给对方的，与本笔交易的买卖方向（债权方在"
                    + (debtorIsPartyA ? "乙方" : "甲方") + "）对不上";
        }
        return null;
    }

    private static String sideLabel(Agreement agreement, Long partyId) {
        return partyId != null && partyId.equals(agreement.getPartyAId()) ? "甲方" : "乙方";
    }

    private static String usageOf(CreditTermQuery query) {
        String usage = query.getUsage();
        return usage == null || usage.isBlank() ? "生成本笔账的到期日" : usage;
    }
}
