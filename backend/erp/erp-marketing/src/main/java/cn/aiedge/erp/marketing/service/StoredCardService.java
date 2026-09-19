package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.StoredCard;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 储值卡（预付费载体）：开卡 → 充值（含赠送）→ 消费扣减 → 退款 → 冻结/解冻。
 *
 * <p>口径：所有金额变动都写 {@code mkt_stored_card_flow} 流水（含变动后余额），
 * 卡档案上的 balance/total_* 与流水**恒等**（可对账）。</p>
 * <p>合规（法释〔2025〕4 号 / 单用途商业预付卡管理办法）：必须支持退款，页面须有告知。</p>
 */
public interface StoredCardService extends IService<StoredCard> {

    /** 开卡（面值即初始余额；可带赠送额） */
    Long issue(StoredCard card, BigDecimal bonusAmount, String remark);

    /** 充值（金额 + 按比例/定额赠送） */
    StoredCard recharge(Long cardId, BigDecimal amount, BigDecimal bonusAmount, String sourceBillNo);

    /** 充值（可指定结算账户：CASH 库存现金 / BANK 银行存款，用于记账借贷方向） */
    StoredCard recharge(Long cardId, BigDecimal amount, BigDecimal bonusAmount, String sourceBillNo, String settleAccount);

    /** 消费扣减（余额不足或卡非正常状态会拒绝） */
    StoredCard consume(Long cardId, BigDecimal amount, String sourceBillNo);

    /**
     * 退款：按指定金额退回本金（余额随之减少，并记 REFUND 流水）。
     * 与「消费」的区别在流水语义，便于财务与合规追溯。
     */
    StoredCard refund(Long cardId, BigDecimal amount, String remark);

    /** 冻结 / 解冻 */
    StoredCard changeStatus(Long cardId, String status, String remark);

    /** 某会员的全部卡 */
    List<StoredCard> listByPartner(Long partnerId);

    /** 按卡号查询（收银/开单场景） */
    StoredCard getByCardNo(String cardNo);
}
