package cn.aiedge.base.credit;

/**
 * 「结算方式」—— 决定**有没有账期这回事**的**前置字段**（DOMAIN-MODEL §13.3 补充口径 3）。
 *
 * <h3>为什么它必须存在，且必须**排在账期天数之前**</h3>
 * 现行实现（2026-09-22 实测）一上来就问「账期天数」，于是协议约定「现金结算」时
 * 也照样报「未约定账期」—— 用户第 4 轮点名的就是这个毛病。
 * 「有没有账期」与「账期几天」是两个问题：<b>没有约定结算方式，就问不出账期天数</b>。
 *
 * <h3>为什么是**扁平五项**，不是"现金 + 子类型"</h3>
 * 「现款现货」在真实生意里是**三种**（用户第 7 轮原话）：
 * <ul>
 *   <li><b>先款后货</b>：款在发货<b>前</b> ⇒ 对卖方零风险，通常不产生应收；</li>
 *   <li><b>现款现结</b>：钱货<b>同时</b> ⇒ 到期日 = 业务日，最常见的零售形态；</li>
 *   <li><b>货到付款</b>：货在付款<b>前</b> ⇒ 卖方承担极短期在途敞口，到期日应为<b>到货日</b>。</li>
 * </ul>
 * 三者都无账期，但**资金与货物的先后不同 ⇒ 到期日与风控含义都不同**，
 * 压成一个笼统的「现金」会同时丢掉这两样。
 * 拆成两个字段（先选"现金/账期"、再选子类型）则会凭空造出一种**新的"未约定"态**
 * （选了第一层没选第二层），又得再定一遍它归哪一层 —— 故按 §13.3 的建议保持扁平。
 *
 * <h3>⚠️ 语义写在枚举上，不写在调用方的 if 里</h3>
 * 「是否需要账期天数」由 {@link #requiresCreditDays()} 统一回答，消费方不要自行手写判断，
 * 否则加一种结算方式时必然漏改一处。
 */
public enum SettlementType {

    /** 现款现货 · 先款后货：款在发货前 ⇒ 对卖方零风险。 */
    CASH_PREPAY("现款现货 · 先款后货"),

    /** 现款现货 · 现款现结：钱货同时 ⇒ 到期日 = 业务日。 */
    CASH_SPOT("现款现货 · 现款现结"),

    /** 现款现货 · 货到付款：货先到、款后付 ⇒ 卖方承担极短期在途敞口。 */
    CASH_ON_DELIVERY("现款现货 · 货到付款"),

    /** 账期结算：**唯一**需要（且必须）约定账期天数的一种；缺天数不许提交。 */
    CREDIT("账期结算"),

    /** 滚结（滚动结算）：无固定账期，按滚结规则处理。 */
    ROLLING("滚结");

    private final String label;

    SettlementType(String label) {
        this.label = label;
    }

    /** 中文名（可直接进提示文案与日志）。 */
    public String getLabel() {
        return label;
    }

    /**
     * 是否需要账期天数 —— <b>只有「账期结算」需要</b>。
     *
     * <p>「需要」= 条件必填：选了它却没填天数 ⇒ {@link CreditTermResult.Status#UNDECLARED}，
     * 由单据提交处**拒单**（§13.3 铁律②）。</p>
     */
    public boolean requiresCreditDays() {
        return this == CREDIT;
    }

    /** 是不是现款类（钱货两清或款货先后明确，无信用敞口）。 */
    public boolean isCash() {
        return this == CASH_PREPAY || this == CASH_SPOT || this == CASH_ON_DELIVERY;
    }

    /**
     * 按编码解析（大小写不敏感、容忍空白）。
     *
     * @return 认不出来时返回 {@code null} —— 调用方据此走"认不出 ⇒ 不赊账"的低风险兜底，
     *         <b>不要</b>用一个默认值顶替（那等于平台替双方决定结算方式）
     */
    public static SettlementType parseOrNull(String code) {
        if (code == null) {
            return null;
        }
        String s = code.trim();
        if (s.isEmpty()) {
            return null;
        }
        for (SettlementType t : values()) {
            if (t.name().equalsIgnoreCase(s)) {
                return t;
            }
        }
        return null;
    }
}
