package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.marketing.entity.StoredCard;
import cn.aiedge.erp.marketing.entity.StoredCardFlow;
import cn.aiedge.erp.marketing.mapper.StoredCardFlowMapper;
import cn.aiedge.erp.marketing.mapper.StoredCardMapper;
import cn.aiedge.erp.marketing.service.StoredCardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoredCardServiceImpl extends ServiceImpl<StoredCardMapper, StoredCard>
        implements StoredCardService {

    private final StoredCardFlowMapper flowMapper;
    /** 资金闭环：金额动作同步推总账（同事务；凭证失败整笔回滚） */
    private final cn.aiedge.erp.marketing.service.StoredCardAccountingService accountingService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? ZERO : v; }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long issue(StoredCard card, BigDecimal bonusAmount, String remark) {
        if (card.getCardNo() == null || card.getCardNo().isBlank()) {
            throw new IllegalArgumentException("卡号不能为空");
        }
        if (nz(card.getFaceValue()).compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("开卡面值必须大于 0");
        }
        Long exist = baseMapper.selectCount(new LambdaQueryWrapper<StoredCard>()
                .eq(StoredCard::getCardNo, card.getCardNo()));
        if (exist != null && exist > 0) throw new IllegalArgumentException("卡号已存在：" + card.getCardNo());

        BigDecimal bonus = nz(bonusAmount);
        card.setId(null);
        card.setTenantId(tenantId());
        if (card.getCardType() == null) card.setCardType(StoredCard.TYPE_STORED);
        card.setBalance(nz(card.getFaceValue()).add(bonus));
        card.setTotalRecharge(nz(card.getFaceValue()));
        card.setTotalConsume(ZERO);
        card.setTotalBonus(bonus);
        card.setStatus(StoredCard.STATUS_ACTIVE);
        card.setIssueTime(LocalDateTime.now());
        card.setCreateBy(SecurityUtils.getCurrentUserId());
        card.setCreateTime(LocalDateTime.now());
        card.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(card);

        StoredCardFlow issueFlow = writeFlow(card, StoredCardFlow.ISSUE, nz(card.getFaceValue()), bonus, null,
                remark == null ? "开卡" : remark, card.getSettleAccount());
        // 资金闭环：开卡收款 → 借 现金/银行，贷 预收账款（只按实收面值记账，赠送不进凭证）
        accountingService.postVoucher(card, issueFlow, nz(card.getFaceValue()));
        if (bonus.compareTo(ZERO) > 0) {
            writeFlow(card, StoredCardFlow.BONUS, ZERO, bonus, null, "开卡赠送", null);
        }
        log.info("[储值卡] 开卡 {} 面值 {} 赠送 {}（客户 {}）",
                card.getCardNo(), card.getFaceValue(), bonus, card.getPartnerId());
        return card.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoredCard recharge(Long cardId, BigDecimal amount, BigDecimal bonusAmount, String sourceBillNo) {
        return recharge(cardId, amount, bonusAmount, sourceBillNo, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoredCard recharge(Long cardId, BigDecimal amount, BigDecimal bonusAmount, String sourceBillNo,
                               String settleAccount) {
        StoredCard card = requireActive(cardId, "充值");
        BigDecimal amt = nz(amount);
        if (amt.compareTo(ZERO) <= 0) throw new IllegalArgumentException("充值金额必须大于 0");
        BigDecimal bonus = nz(bonusAmount);

        card.setTotalRecharge(nz(card.getTotalRecharge()).add(amt));
        card.setTotalBonus(nz(card.getTotalBonus()).add(bonus));
        card.setBalance(nz(card.getBalance()).add(amt).add(bonus));
        card.setUpdateTime(LocalDateTime.now());
        if (StoredCard.STATUS_USED_UP.equals(card.getStatus())) card.setStatus(StoredCard.STATUS_ACTIVE);
        baseMapper.updateById(card);

        // 本次收款走哪个账户：本次指定优先，其次用卡上默认账户，都没有则记账侧回落银行存款
        String settle = settleAccount == null || settleAccount.isBlank()
                ? card.getSettleAccount() : settleAccount;
        StoredCardFlow rechargeFlow = writeFlow(card, StoredCardFlow.RECHARGE, amt, ZERO, sourceBillNo, "充值", settle);
        // 资金闭环：充值收款 → 借 现金/银行，贷 预收账款
        accountingService.postVoucher(card, rechargeFlow, amt);
        if (bonus.compareTo(ZERO) > 0) {
            writeFlow(card, StoredCardFlow.BONUS, ZERO, bonus, sourceBillNo, "充值赠送", null);
        }
        log.info("[储值卡] {} 充值 {} 赠送 {}，余额 {}", card.getCardNo(), amt, bonus, card.getBalance());
        return card;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoredCard consume(Long cardId, BigDecimal amount, String sourceBillNo) {
        StoredCard card = requireActive(cardId, "消费");
        BigDecimal amt = nz(amount);
        if (amt.compareTo(ZERO) <= 0) throw new IllegalArgumentException("消费金额必须大于 0");
        if (nz(card.getBalance()).compareTo(amt) < 0) {
            throw new IllegalArgumentException("卡内余额不足：当前余额 " + card.getBalance() + "，本次消费 " + amt);
        }
        card.setBalance(nz(card.getBalance()).subtract(amt));
        card.setTotalConsume(nz(card.getTotalConsume()).add(amt));
        if (card.getBalance().compareTo(ZERO) <= 0) card.setStatus(StoredCard.STATUS_USED_UP);
        card.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(card);

        StoredCardFlow consumeFlow = writeFlow(card, StoredCardFlow.CONSUME, amt.negate(), ZERO, sourceBillNo,
                "消费扣减", null);
        // 资金闭环：消费结转 → 借 预收账款，贷 主营业务收入
        accountingService.postVoucher(card, consumeFlow, amt);
        return card;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoredCard refund(Long cardId, BigDecimal amount, String remark) {
        StoredCard card = getById(cardId);
        if (card == null) throw new IllegalArgumentException("卡不存在");
        if (StoredCard.STATUS_REFUNDED.equals(card.getStatus())) {
            throw new IllegalArgumentException("该卡已退卡，不能重复退款");
        }
        BigDecimal amt = nz(amount);
        if (amt.compareTo(ZERO) <= 0) throw new IllegalArgumentException("退款金额必须大于 0");
        if (nz(card.getBalance()).compareTo(amt) < 0) {
            throw new IllegalArgumentException("退款金额超过卡内余额：余额 " + card.getBalance() + "，退款 " + amt);
        }

        card.setBalance(nz(card.getBalance()).subtract(amt));
        // 全额退款即退卡；部分退款保留卡
        if (card.getBalance().compareTo(ZERO) <= 0) card.setStatus(StoredCard.STATUS_REFUNDED);
        card.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(card);

        StoredCardFlow refundFlow = writeFlow(card, StoredCardFlow.REFUND, amt.negate(), ZERO, null,
                remark == null ? "退款（预付费合规入口）" : remark, card.getSettleAccount());
        // 资金闭环：退款 → 借 预收账款，贷 现金/银行
        accountingService.postVoucher(card, refundFlow, amt);
        log.info("[储值卡] {} 退款 {}，余额 {}", card.getCardNo(), amt, card.getBalance());
        return card;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoredCard changeStatus(Long cardId, String status, String remark) {
        StoredCard card = getById(cardId);
        if (card == null) throw new IllegalArgumentException("卡不存在");
        card.setStatus(status);
        card.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(card);
        writeFlow(card, StoredCardFlow.ADJUST, ZERO, ZERO, null,
                "状态变更：" + status + (remark == null || remark.isBlank() ? "" : "（" + remark + "）"), null);
        return card;
    }

    @Override
    public List<StoredCard> listByPartner(Long partnerId) {
        return baseMapper.selectList(new LambdaQueryWrapper<StoredCard>()
                .eq(StoredCard::getPartnerId, partnerId)
                .orderByDesc(StoredCard::getIssueTime));
    }

    @Override
    public StoredCard getByCardNo(String cardNo) {
        return baseMapper.selectOne(new LambdaQueryWrapper<StoredCard>()
                .eq(StoredCard::getCardNo, cardNo).last("limit 1"));
    }

    // ══════════════ 内部 ══════════════

    private StoredCard requireActive(Long cardId, String action) {
        StoredCard card = getById(cardId);
        if (card == null) throw new IllegalArgumentException("卡不存在");
        if (StoredCard.STATUS_FROZEN.equals(card.getStatus())) {
            throw new IllegalArgumentException("该卡已冻结，不能" + action);
        }
        if (StoredCard.STATUS_REFUNDED.equals(card.getStatus())) {
            throw new IllegalArgumentException("该卡已退卡，不能" + action);
        }
        if (StoredCard.STATUS_EXPIRED.equals(card.getStatus())) {
            throw new IllegalArgumentException("该卡已过期，不能" + action);
        }
        if (card.getExpireTime() != null && card.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("该卡有效期已过（" + card.getExpireTime().toLocalDate() + "），不能" + action);
        }
        return card;
    }

    /**
     * 写一条流水。
     *
     * <p>⚠️ 所有字段必须在 {@code insert} **之前** set：本方法返回的是已入库的对象，
     * 调用方再 set 只会改内存副本，列在库里仍是 NULL（settle_account 曾因此整列为空，
     * 事后无法回答"这笔钱走的现金还是银行"）。</p>
     */
    private StoredCardFlow writeFlow(StoredCard card, String flowType, BigDecimal amount, BigDecimal bonus,
                                     String sourceBillNo, String remark, String settleAccount) {
        StoredCardFlow flow = new StoredCardFlow()
                .setTenantId(card.getTenantId())
                .setCardId(card.getId())
                .setCardNo(card.getCardNo())
                .setPartnerId(card.getPartnerId())
                .setFlowType(flowType)
                .setAmount(amount)
                .setBonusAmount(bonus)
                .setBalanceAfter(card.getBalance())
                .setSourceBillNo(sourceBillNo)
                .setSettleAccount(settleAccount)
                .setHandlerName(SecurityUtils.getCurrentUsername())
                .setRemark(remark)
                .setDeleted(0)
                .setCreateTime(LocalDateTime.now());
        flowMapper.insert(flow);
        return flow;
    }
}
