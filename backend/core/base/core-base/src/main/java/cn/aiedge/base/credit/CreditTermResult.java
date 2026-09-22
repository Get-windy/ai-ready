package cn.aiedge.base.credit;

import java.time.LocalDate;

/**
 * 「本笔账的账期口径」的查询结果 —— 用**四态**表达，<b>没有任何默认值入口</b>。
 *
 * <h3>为什么必须是多态（本类存在的唯一理由）</h3>
 * 下游（财务生成应收/应付到期日）要问的核心问题不是"几天"，而是
 * "<b>这笔账到底有没有账期、双方约定了没有</b>"。四种答案对应四种完全不同的处置：
 *
 * <table border="1">
 *   <tr><th>状态</th><th>含义</th><th>下游应当怎么做</th></tr>
 *   <tr><td>{@link Status#AGREED}</td><td>协议里约定了<b>账期结算</b>且天数、方向齐全</td>
 *       <td>到期日 = <b>业务日期</b> + 约定天数（{@link #dueDateFrom(LocalDate)}）</td></tr>
 *   <tr><td>{@link Status#NO_CREDIT_TERM}</td><td><b>本笔压根没有账期这回事</b>
 *       —— 协议约定的结算方式是现款现货（先款后货 / 现款现结 / 货到付款）或滚结</td>
 *       <td>到期日 = <b>业务日</b>（当天结清、不产生账龄），<b>单据照常提交</b>。
 *           这是<b>正常的业务结论</b>，不是缺项</td></tr>
 *   <tr><td>{@link Status#NO_AGREEMENT}</td><td>该时点<b>没有可执行的结算方式约定</b>
 *       （无协议 / 协议已到期 / 有协议但没约定结算方式 / 结算方式取值认不出）</td>
 *       <td>走「<b>无协议无特定客户模式</b>」：一律<b>现款现结</b>（低风险缺省），到期日 = 业务日</td></tr>
 *   <tr><td>{@link Status#UNDECLARED}</td><td><b>约定了「账期结算」却缺账期天数或账期方向</b>
 *       —— 条件必填缺失</td>
 *       <td><b>拒绝这笔单据提交</b>（§13.3 铁律② / 第一轮"提交不了"的准确含义）</td></tr>
 * </table>
 *
 * <h3>⚠️ {@code NO_CREDIT_TERM} 与 {@code UNDECLARED} 不是一回事（最容易搞错的一对）</h3>
 * 前者是"<b>本笔无账期</b>"—— 双方明确约定了现金或滚结，账期这个词压根不适用，
 * 单据照开、到期日就是业务日；后者是"<b>该约的没约</b>"—— 双方选了账期结算却没写几天，
 * 配置没做完，<b>不许提交</b>。把两者混成一个状态，必然导致
 * 「约定现金 ⇒ 被误判成未约定 ⇒ 拒单」或者「账期缺天数 ⇒ 被放行 ⇒ 账期形同不存在」。
 *
 * <p>⚠️ 本类<b>刻意没有</b> {@code orElse(...)} / {@code getOrDefault(...)} 之类的入口：
 * 一旦提供"拿不到就给个默认天数"的方便，就等于平台替双方定了商业条款（㉜ 明令禁止）。
 * {@link #getCreditDays()} 在非 {@code AGREED} 时<b>恒为 null</b>。</p>
 *
 * <p>随结果一起带回 {@code agreementId / versionId / versionNo} 不是装饰：
 * 出了问题要回答的第一个问题是「<b>这笔账当时按的是哪一版协议</b>」（㉛）。</p>
 *
 * @see SettlementType
 * @see TradeCreditTermProvider
 * @see CreditTermQuery
 */
public final class CreditTermResult {

    /** 四态（见类注释表格）。 */
    public enum Status {

        /** 已约定：协议里明确写了账期天数与方向，且方向与本笔交易的债权方一致。 */
        AGREED("已约定"),

        /**
         * 本笔无账期：协议约定的结算方式是现款现货（三种之一）或滚结。
         *
         * <p>这是<b>正常的业务结论</b>（钱货两清、零信用敞口），不是"未约定"、更不是缺项。</p>
         */
        NO_CREDIT_TERM("本笔无账期"),

        /**
         * 无适用协议：该业务时点没有可执行的结算方式约定
         * （无协议 / 已到期 / 有协议但没约定结算方式 / 结算方式取值认不出）。
         *
         * <p>处置 = 走「无协议无特定客户模式」：一律现款现结（低风险缺省）。</p>
         */
        NO_AGREEMENT("无适用协议"),

        /** 约定了「账期结算」却缺天数或方向：条件必填缺失，**拒单**。 */
        UNDECLARED("未约定");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Status status;

    /** 约定的账期天数；**仅 AGREED 时非空**（非 AGREED 一律 null，不回落到 0 或任何默认值）。 */
    private final Integer creditDays;

    /** 本笔的结算方式；{@code NO_CREDIT_TERM} 时必有值，其余状态可为空。 */
    private final SettlementType settlementType;

    /** 中文原因：为什么没有账期 / 为什么没有适用协议 / 缺了哪一项（可直接进日志或提示文案）。 */
    private final String reason;

    /** 是哪一份协议（AGREED / NO_CREDIT_TERM / UNDECLARED 时有值），供留痕与排查。 */
    private final Long agreementId;

    /** 是哪一版（ABOVE 同）—— 举证与复盘要回答"当时按哪一版"。 */
    private final Long versionId;

    private final Integer versionNo;

    private CreditTermResult(Status status, Integer creditDays, SettlementType settlementType, String reason,
                             Long agreementId, Long versionId, Integer versionNo) {
        this.status = status;
        this.creditDays = creditDays;
        this.settlementType = settlementType;
        this.reason = reason;
        this.agreementId = agreementId;
        this.versionId = versionId;
        this.versionNo = versionNo;
    }

    // ══════════════════════ 四个工厂（四态各一个，没有第五个） ══════════════════════

    /** 已约定：双方在某一版协议里明确写了「账期结算」的天数与方向。 */
    public static CreditTermResult agreed(int creditDays, Long agreementId, Long versionId, Integer versionNo) {
        return new CreditTermResult(Status.AGREED, creditDays, SettlementType.CREDIT, null,
                agreementId, versionId, versionNo);
    }

    /**
     * 本笔无账期：协议约定的结算方式是现款现货或滚结 —— 账期这个词不适用，单据照常提交。
     *
     * @param settlementType 具体是哪一种（先款后货 / 现款现结 / 货到付款 / 滚结），
     *                       带出来是为了保留<b>资金与货物先后</b>这一层信息，
     *                       不许在调用方压成一个笼统的"现金"
     */
    public static CreditTermResult noCreditTerm(SettlementType settlementType, String reason,
                                                Long agreementId, Long versionId, Integer versionNo) {
        if (settlementType == null) {
            throw new IllegalArgumentException("「本笔无账期」必须说明约定的结算方式是哪一种");
        }
        return new CreditTermResult(Status.NO_CREDIT_TERM, null, settlementType, reason,
                agreementId, versionId, versionNo);
    }

    /** 无适用协议：该业务时点没有可执行的结算方式约定 ⇒ 走无协议无特定客户模式（现款现结）。 */
    public static CreditTermResult noAgreement(String reason) {
        return new CreditTermResult(Status.NO_AGREEMENT, null, null, reason, null, null, null);
    }

    /** 约定了账期结算却缺项（天数 / 方向）：**拒单**。 */
    public static CreditTermResult undeclared(String reason, Long agreementId, Long versionId, Integer versionNo) {
        return new CreditTermResult(Status.UNDECLARED, null, SettlementType.CREDIT, reason,
                agreementId, versionId, versionNo);
    }

    // ══════════════════════ 读取 ══════════════════════

    public Status getStatus() {
        return status;
    }

    public boolean isAgreed() {
        return status == Status.AGREED;
    }

    public boolean isNoCreditTerm() {
        return status == Status.NO_CREDIT_TERM;
    }

    public boolean isNoAgreement() {
        return status == Status.NO_AGREEMENT;
    }

    public boolean isUndeclared() {
        return status == Status.UNDECLARED;
    }

    /** 约定的账期天数；非"已约定"时返回 {@code null}（**不回落**成 0 或任何默认天数）。 */
    public Integer getCreditDays() {
        return creditDays;
    }

    /** 本笔的结算方式；{@code NO_CREDIT_TERM} 时必有值。 */
    public SettlementType getSettlementType() {
        return settlementType;
    }

    public String getReason() {
        return reason;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    /**
     * 到期日。
     *
     * <ul>
     *   <li>{@code AGREED} ⇒ <b>业务日期 + 约定天数</b>（自然日历进位，不做"按 30 天折算"）；</li>
     *   <li>{@code NO_CREDIT_TERM} ⇒ <b>业务日期本身</b>（当天结清、不产生账龄）。</li>
     * </ul>
     *
     * <h3>⚠️ 起算基准是「业务日期」，不是"当前时间"</h3>
     * 协议的口径是"下单时刻生效的那一版"（㉛），而且应收到期日必须由业务日期推算 ——
     * 用 {@code LocalDate.now()} 会让同一天补录的两张单得到不同的到期日，
     * 也会让历史单据的对账口径随"今天几号"漂移。
     *
     * <h3>⚠️ 已知待收紧：货到付款的到期日</h3>
     * §13.3 第 2 层表格里「货到付款」的到期日应为<b>到货日</b>（货在付款之前，卖方承担在途敞口）。
     * 但记账时点这条链路<b>拿不到到货日</b>（只有业务日期），故与另外两种现款现货一样取业务日；
     * 具体是哪一种结算方式已随 {@link #getSettlementType()} 带出，
     * 待链路提供到货日后在此收紧，<b>不需要</b>改调用方。
     *
     * @throws IllegalStateException 非"已约定"且非"本笔无账期"时调用（这是调用方的编程错误，
     *                               "未约定"应当去拒单，而不是算一个默认日期出来）
     */
    public LocalDate dueDateFrom(LocalDate businessDate) {
        if (businessDate == null) {
            throw new IllegalStateException("必须提供业务日期才能推算到期日（业务日期是协议口径的起算基准）");
        }
        if (status == Status.NO_CREDIT_TERM) {
            // 无账期：钱货两清（或款货先后明确），当天结清 ⇒ 到期日 = 业务日
            return businessDate;
        }
        if (status != Status.AGREED || creditDays == null) {
            throw new IllegalStateException("非「已约定」/「本笔无账期」状态不能推算到期日（当前=" + status + "）"
                    + "：账期结算缺项时应当拒单，而不是算出一个默认到期日");
        }
        return businessDate.plusDays(creditDays);
    }

    @Override
    public String toString() {
        return switch (status) {
            case AGREED -> "CreditTermResult{已约定=" + creditDays + " 天, 协议=" + agreementId
                    + ", 版本=" + versionNo + "}";
            case NO_CREDIT_TERM -> "CreditTermResult{本笔无账期: 结算方式="
                    + (settlementType == null ? "?" : settlementType.getLabel())
                    + ", 协议=" + agreementId + ", 版本=" + versionNo + "}";
            case NO_AGREEMENT -> "CreditTermResult{无适用协议: " + reason + "}";
            case UNDECLARED -> "CreditTermResult{账期结算缺项（须拒单）: " + reason + ", 协议=" + agreementId
                    + ", 版本=" + versionNo + "}";
        };
    }
}
