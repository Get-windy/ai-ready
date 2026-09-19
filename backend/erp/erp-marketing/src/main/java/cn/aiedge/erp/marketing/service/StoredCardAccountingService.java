package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.StoredCard;
import cn.aiedge.erp.marketing.entity.StoredCardFlow;

/**
 * 储值卡资金闭环：把开卡/充值/消费/退款推送总账（会计凭证）。
 *
 * <p>记账口径见 `V11.388.0` 迁移注释；实现要点：</p>
 * <ul>
 *   <li><b>同事务</b>：与卡余额变动在同一事务内，凭证失败则余额回滚，避免"钱动了账没动"；</li>
 *   <li><b>幂等</b>：以流水行 {@code voucher_no} 为幂等标记，非空即跳过（重试安全）；</li>
 *   <li><b>以流水为源单据</b>：`sourceId` 用流水 id（非卡 id），保证一张卡多次充值是各自独立的凭证；</li>
 *   <li><b>赠送不进凭证</b>：只按实收/实退/实消金额记账（赠送不产生现金流）。</li>
 * </ul>
 */
public interface StoredCardAccountingService {

    /**
     * 生成凭证并把凭证号回写到流水
     *
     * @param card   卡（取客户信息用于辅助核算）
     * @param flow   已落库的流水（取 id 作 sourceId、类型决定借贷方向）
     * @param amount 本次实收/实退/实消金额（正数）
     * @return 凭证号；未生成（无金额/无科目配置）时返回 null
     */
    String postVoucher(StoredCard card, StoredCardFlow flow, java.math.BigDecimal amount);
}
