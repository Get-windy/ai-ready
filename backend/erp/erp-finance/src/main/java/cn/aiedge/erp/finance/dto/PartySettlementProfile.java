package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 往来单位的**结算档案**（{@code biz_party} 上那几列）—— 「租户内交易」的结算口径。
 *
 * <h3>它在三层结算口径里的位置（DOMAIN-MODEL §7.7 附）</h3>
 * <table border="1">
 *   <tr><th>场景</th><th>用哪层口径</th></tr>
 *   <tr><td>跨租户 + 有协议有约定</td><td>协议</td></tr>
 *   <tr><td>跨租户 + 无协议 / 协议到期 / 协议未约定结算方式</td><td>现款现结</td></tr>
 *   <tr><td><b>租户内交易</b>（客户就是我自己的客户）</td><td><b>本类（{@code biz_party} 档案）</b></td></tr>
 *   <tr><td>档案也没设</td><td>现款现结</td></tr>
 * </table>
 *
 * <h3>⚠️ 本仓现状：这几列此前**一个消费方都没有**（2026-09-22 实测）</h3>
 * {@code settlement_type} / {@code credit_days} / {@code payment_days} 等列早就在表上
 * （{@code V9.13.1} / {@code V11.135.0} / {@code V11.151.0}），但旧实现只读协议里的
 * {@code AR_CREDIT_DAYS}，加上采购/销售硬编码的 30 天，一共**三套口径并存且互不知情**。
 * 本类就是那张"档案口径"的取数载体。
 *
 * <h3>⚠️ {@code settlementType} 是**历史两值编码**，不是协议侧的五项枚举</h3>
 * {@code V9.14.0} 把它从字符串改成了整数：{@code 0 = 现结}，非 0 = 有账期。
 * 因此这里的判定只能是「0 ⇒ 无账期；非 0 ⇒ 看天数」，
 * <b>不要</b>拿它与 {@code cn.aiedge.base.credit.SettlementType} 的五个编码混用
 * （那一套是协议字段字典的取值，两者不是同一个东西）。
 */
@Data
public class PartySettlementProfile {

    /** 结算方式（历史编码）：{@code 0} = 现结（现款现结、无账期）；非 0 = 有账期。 */
    private Integer settlementType;

    /** 动态收款期限（天）—— **应收**方向：本租户给这个客户多少天账期。 */
    private Integer creditDays;

    /** 动态付款期限（天）—— **应付**方向：本租户欠这个供应商多少天。 */
    private Integer paymentDays;
}
