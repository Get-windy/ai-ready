package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.LoyaltyCard;
import cn.aiedge.erp.marketing.mapper.LoyaltyCardMapper;
import cn.aiedge.erp.marketing.service.LoyaltyCardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class LoyaltyCardServiceImpl extends ServiceImpl<LoyaltyCardMapper, LoyaltyCard>
        implements LoyaltyCardService {

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
            }
        }
    }
}
