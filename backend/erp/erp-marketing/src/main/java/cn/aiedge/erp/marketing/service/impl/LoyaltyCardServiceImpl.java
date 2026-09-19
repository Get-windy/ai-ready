package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.LoyaltyCard;
import cn.aiedge.erp.marketing.mapper.LoyaltyCardMapper;
import cn.aiedge.erp.marketing.service.LoyaltyCardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.aiedge.erp.marketing.service.PointsLedgerService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyCardServiceImpl extends ServiceImpl<LoyaltyCardMapper, LoyaltyCard>
        implements LoyaltyCardService {

    /**
     * 会员积分台账（批次 + FIFO + 过期）。
     * 积分账户（本表 points）与批次剩余之和保持同步：加/减都同时落两者，
     * 这样「积分有效期」才有意义（过期的是**批次剩余**，不是账户总额）。
     */
    private final PointsLedgerService pointsLedgerService;

    @Override
    public List<LoyaltyCard> listByMember(Long memberId) {
        return list(new LambdaQueryWrapper<LoyaltyCard>()
                .eq(LoyaltyCard::getPartnerId, memberId)
                .eq(LoyaltyCard::getIsActive, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long cardId, Integer points) {
        LoyaltyCard card = getById(cardId);
        if (card != null) {
            BigDecimal currentPoints = card.getPoints() != null ? card.getPoints() : BigDecimal.ZERO;
            card.setPoints(currentPoints.add(new BigDecimal(points)));
            BigDecimal totalEarned = card.getTotalEarned() != null ? card.getTotalEarned() : BigDecimal.ZERO;
            card.setTotalEarned(totalEarned.add(new BigDecimal(points)));
            updateById(card);
            // 同步落批次台账（按《会员设置》的积分有效期计算到期时间）
            pointsLedgerService.earn(card.getCardCode(), card.getPartnerId(), new BigDecimal(points), "ADJUST", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long cardId, Integer points) {
        LoyaltyCard card = getById(cardId);
        if (card != null && card.getPoints() != null) {
            BigDecimal deductAmount = new BigDecimal(points);
            if (card.getPoints().compareTo(deductAmount) >= 0) {
                card.setPoints(card.getPoints().subtract(deductAmount));
                BigDecimal totalRedeemed = card.getTotalRedeemed() != null ? card.getTotalRedeemed() : BigDecimal.ZERO;
                card.setTotalRedeemed(totalRedeemed.add(deductAmount));
                updateById(card);
                // FIFO 扣减批次（先到期的先扣），保证账户余额与批次剩余一致
                BigDecimal used = pointsLedgerService.use(card.getCardCode(), deductAmount, null);
                if (used.compareTo(deductAmount) < 0) {
                    log.warn("[会员积分] 批次可用额度({})小于扣减额({})，卡号={}：请用「积分台账」核对批次",
                            used, deductAmount, card.getCardCode());
                }
            }
        }
    }
}
