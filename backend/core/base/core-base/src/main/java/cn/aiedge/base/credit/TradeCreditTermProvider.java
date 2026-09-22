package cn.aiedge.base.credit;

/**
 * 「双方约定的账期天数」的**提供方契约**（字段设定版的第一个真实消费方之一）。
 *
 * <pre>
 * 按「卖方主体 + 买方主体 + 租户对 + 业务时点」取双方约定的账期天数
 * </pre>
 *
 * <h2>本接口为什么存在（分层理由，改之前先读）</h2>
 * 「账期」在系统里有<b>两个主人</b>：
 * <ul>
 *   <li><b>协议模块</b>（{@code backend/agreement}）—— 它是<b>唯一</b>知道"双方约定了什么"的地方
 *       （{@code agreement_setting} 里的 {@code AR_CREDIT_DAYS} / {@code AR_CREDIT_PROVIDER}，
 *       读取入口是 {@code AgreementRuntime}）；</li>
 *   <li><b>财务模块</b>（{@code erp/erp-finance}）—— 它是<b>唯一</b>写应收/应付到期日的地方。</li>
 * </ul>
 * 如果让财务直接依赖协议模块，就会形成<b>业务模块之间的反向依赖</b>：
 * 协议模块被停用 / 未部署 / 裁剪发行时，财务链路<b>直接起不来</b> ——
 * 而收付款是记账的最小闭环，绝不能因为一份协议配置而停摆。
 *
 * <p>因此照本仓既有范式（{@code cn.aiedge.base.payment.PaymentCallbackVerifier}：
 * <b>契约接口放 core-base、实现放各业务模块</b>）办：
 * <b>接口在 core-base（本文件），实现在 agreement，消费方在 erp-finance 且可选注入</b>。
 * 消费方用 {@code ObjectProvider} 容忍"实现不存在"，此时行为<b>与接入前逐字一致</b>。</p>
 *
 * <h2>三条硬性口径</h2>
 * <ol>
 *   <li><b>必须带业务时点</b>：见 {@link CreditTermQuery}。协议按"那一刻生效的那一版"执行（㉛）。</li>
 *   <li><b>不许凭空给默认值</b>：结果用 {@link CreditTermResult} 表达四态，
 *       本接口<b>不提供</b>任何"拿不到就给个默认天数"的入口 ——
 *       平台给默认值等于替双方定商业条款（DOMAIN-MODEL §3.4.4d1 / ㉜）。</li>
 *   <li><b>顺序必须是「先结算方式、再账期天数」</b>：结算方式是<b>前置字段</b>，
 *       它决定账期天数是否适用。现款现货（三种）与滚结<b>压根不问天数</b>，
 *       直接回"本笔无账期"；只有「账期结算」才去取天数与方向（§13.3 铁律①）。</li>
 * </ol>
 *
 * <h2>⚠️ 「不许阻断单据」有一条例外（唯一一条）</h2>
 * 四态里只有 {@link CreditTermResult.Status#UNDECLARED}（约定了<b>账期结算</b>却缺天数或方向）
 * 是<b>条件必填缺失</b>，按 §13.3 铁律② 要<b>拦住单据提交</b>，
 * 由消费方在<b>提交处</b>抛业务异常 —— 实现方照常把它作为状态返回，不要自己抛。
 * 其余三态都<b>不得</b>挡住单据：无账期是正常结论、无适用协议走现款现结（低风险缺省）、
 * 协议取数异常一律降级放行（㉜ 要的是"不自动执行"，不是"用报错惩罚没约定"）。
 *
 * <h2>接入一个实现要做什么</h2>
 * <ol>
 *   <li>在业务模块里实现本接口，并注册成 Spring Bean（无需改消费方）；</li>
 *   <li>{@link #resolve(CreditTermQuery)} 内部委托该模块自己的唯一读取入口
 *       （协议模块即 {@code AgreementRuntime.resolve(...)}），不要自己另写一套查库逻辑；</li>
 *   <li>把认定不了的每一种情况都归到 {@link CreditTermResult#noAgreement(String)} 或
 *       {@link CreditTermResult#undeclared(String, Long, Long, Integer)}，并给出<b>中文原因</b> ——
 *       排查的人只看日志，看不出"为什么没算出来"的返回等于没返回。</li>
 * </ol>
 *
 * @see CreditTermQuery
 * @see CreditTermResult
 */
public interface TradeCreditTermProvider {

    /**
     * 取该笔交易的结算口径（四态结果，见 {@link CreditTermResult}）。
     *
     * <p><b>本方法不会抛业务异常</b>：没约定结算方式、没有适用协议、账期缺项，
     * 都应当是 {@link CreditTermResult} 的一种状态 ——
     * 要不要因此拒单由<b>消费方在提交处</b>决定（只有 {@code UNDECLARED} 该拒），
     * 而不是由提供方抛错。</p>
     *
     * @param query 卖方/买方主体与租户对 + 业务时点
     */
    CreditTermResult resolve(CreditTermQuery query);

    /** 提供方名称（日志用，说明"这个天数是谁给的"）。 */
    default String providerName() {
        return getClass().getSimpleName();
    }
}
