package com.aiready.party.service.impl;

import com.aiready.party.entity.CustomerGrade;
import com.aiready.party.entity.Party;
import com.aiready.party.entity.PartyGradeRelation;
import com.aiready.party.service.CustomerGradeService;
import com.aiready.party.service.PartyGradeRelationService;
import com.aiready.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 往来单位等级关系服务实现类
 */
@Service
public class PartyGradeRelationServiceImpl extends ServiceImpl<com.aiready.party.mapper.PartyGradeRelationMapper, PartyGradeRelation> implements PartyGradeRelationService {

    @Autowired
    private CustomerGradeService customerGradeService;

    @Autowired
    private PartyService partyService;

    @Override
    public PartyGradeRelation getCurrentRelation(Long partyId) {
        return this.getOne(
            new LambdaQueryWrapper<PartyGradeRelation>()
                .eq(PartyGradeRelation::getPartyId, partyId)
                .eq(PartyGradeRelation::getStatus, 1)
                .eq(PartyGradeRelation::getDeleted, 0)
                .orderByDesc(PartyGradeRelation::getCreateTime)
                .last("LIMIT 1")
        );
    }

    @Override
    public List<PartyGradeRelation> listHistoryRelations(Long partyId) {
        return this.list(
            new LambdaQueryWrapper<PartyGradeRelation>()
                .eq(PartyGradeRelation::getPartyId, partyId)
                .eq(PartyGradeRelation::getDeleted, 0)
                .orderByDesc(PartyGradeRelation::getCreateTime)
        );
    }

    @Override
    @Transactional
    public boolean assignDefaultGrade(Long partyId) {
        CustomerGrade defaultGrade = customerGradeService.getDefaultGrade();
        if (defaultGrade == null) {
            return false;
        }

        PartyGradeRelation relation = new PartyGradeRelation();
        relation.setPartyId(partyId);
        relation.setGradeId(defaultGrade.getId());
        relation.setGradeCode(defaultGrade.getGradeCode());
        relation.setGradeName(defaultGrade.getGradeName());
        relation.setTotalAmount(BigDecimal.ZERO);
        relation.setTotalOrders(0);
        relation.setObtainTime(LocalDateTime.now());
        relation.setPermanent(true);
        relation.setStatus(1);

        return this.save(relation);
    }

    @Override
    @Transactional
    public boolean manualSetGrade(Long partyId, Long gradeId, Long operatorId, String reason) {
        CustomerGrade grade = customerGradeService.getById(gradeId);
        if (grade == null) {
            return false;
        }

        // 获取当前等级关系
        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        Long previousGradeId = currentRelation != null ? currentRelation.getGradeId() : null;

        // 禁用当前等级关系
        if (currentRelation != null) {
            currentRelation.setStatus(0);
            this.updateById(currentRelation);
        }

        // 创建新的等级关系
        PartyGradeRelation newRelation = new PartyGradeRelation();
        newRelation.setPartyId(partyId);
        newRelation.setGradeId(gradeId);
        newRelation.setGradeCode(grade.getGradeCode());
        newRelation.setGradeName(grade.getGradeName());
        newRelation.setPreviousGradeId(previousGradeId);
        newRelation.setManualAdjust(true);
        newRelation.setAdjustBy(operatorId);
        newRelation.setAdjustReason(reason);
        newRelation.setObtainTime(LocalDateTime.now());
        newRelation.setPermanent(true);
        newRelation.setStatus(1);

        return this.save(newRelation);
    }

    @Override
    @Transactional
    public boolean upgradeGrade(Long partyId, Long newGradeId) {
        CustomerGrade newGrade = customerGradeService.getById(newGradeId);
        if (newGrade == null) {
            return false;
        }

        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        Long previousGradeId = currentRelation != null ? currentRelation.getGradeId() : null;

        // 禁用当前等级关系
        if (currentRelation != null) {
            currentRelation.setStatus(0);
            this.updateById(currentRelation);
        }

        // 创建新的等级关系
        PartyGradeRelation newRelation = new PartyGradeRelation();
        newRelation.setPartyId(partyId);
        newRelation.setGradeId(newGradeId);
        newRelation.setGradeCode(newGrade.getGradeCode());
        newRelation.setGradeName(newGrade.getGradeName());
        newRelation.setPreviousGradeId(previousGradeId);
        newRelation.setUpgradeTime(LocalDateTime.now());
        newRelation.setObtainTime(LocalDateTime.now());
        newRelation.setPermanent(true);
        newRelation.setStatus(1);

        return this.save(newRelation);
    }

    @Override
    @Transactional
    public boolean downgradeGrade(Long partyId, Long newGradeId, String reason) {
        CustomerGrade newGrade = customerGradeService.getById(newGradeId);
        if (newGrade == null) {
            return false;
        }

        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        Long previousGradeId = currentRelation != null ? currentRelation.getGradeId() : null;

        // 禁用当前等级关系
        if (currentRelation != null) {
            currentRelation.setStatus(0);
            this.updateById(currentRelation);
        }

        // 创建新的等级关系
        PartyGradeRelation newRelation = new PartyGradeRelation();
        newRelation.setPartyId(partyId);
        newRelation.setGradeId(newGradeId);
        newRelation.setGradeCode(newGrade.getGradeCode());
        newRelation.setGradeName(newGrade.getGradeName());
        newRelation.setPreviousGradeId(previousGradeId);
        newRelation.setDowngradeTime(LocalDateTime.now());
        newRelation.setDowngradeReason(reason);
        newRelation.setObtainTime(LocalDateTime.now());
        newRelation.setPermanent(true);
        newRelation.setStatus(1);

        return this.save(newRelation);
    }

    @Override
    @Transactional
    public boolean checkAndUpdateGrade(Long partyId) {
        Party party = partyService.getById(partyId);
        if (party == null) {
            return false;
        }

        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        BigDecimal totalAmount = currentRelation != null ? currentRelation.getTotalAmount() : BigDecimal.ZERO;

        // 根据消费金额获取对应等级
        CustomerGrade targetGrade = customerGradeService.getGradeByAmount(totalAmount);
        if (targetGrade == null) {
            return false;
        }

        Long currentGradeId = currentRelation != null ? currentRelation.getGradeId() : null;
        if (targetGrade.getId().equals(currentGradeId)) {
            // 等级没有变化
            return true;
        }

        // 判断是升级还是降级
        if (currentGradeId == null) {
            // 首次分配等级
            return upgradeGrade(partyId, targetGrade.getId());
        }

        CustomerGrade currentGrade = customerGradeService.getById(currentGradeId);
        if (currentGrade == null) {
            return false;
        }

        if (targetGrade.getGradeLevel() > currentGrade.getGradeLevel()) {
            // 升级
            return upgradeGrade(partyId, targetGrade.getId());
        } else if (targetGrade.getGradeLevel() < currentGrade.getGradeLevel()) {
            // 降级
            return downgradeGrade(partyId, targetGrade.getId(), "消费金额不满足当前等级要求");
        }

        return true;
    }

    @Override
    public void batchCheckAndUpdateGrades() {
        // 获取所有有效的等级关系
        List<PartyGradeRelation> relations = this.list(
            new LambdaQueryWrapper<PartyGradeRelation>()
                .eq(PartyGradeRelation::getStatus, 1)
                .eq(PartyGradeRelation::getDeleted, 0)
        );

        for (PartyGradeRelation relation : relations) {
            try {
                checkAndUpdateGrade(relation.getPartyId());
            } catch (Exception e) {
                // 记录日志，继续处理下一个
                e.printStackTrace();
            }
        }
    }

    @Override
    @Transactional
    public boolean updateTotalAmount(Long partyId, BigDecimal amount) {
        PartyGradeRelation relation = getCurrentRelation(partyId);
        if (relation == null) {
            return false;
        }

        BigDecimal newAmount = relation.getTotalAmount().add(amount);
        relation.setTotalAmount(newAmount);
        return this.updateById(relation);
    }

    @Override
    @Transactional
    public boolean updateTotalOrders(Long partyId, Integer orders) {
        PartyGradeRelation relation = getCurrentRelation(partyId);
        if (relation == null) {
            return false;
        }

        relation.setTotalOrders(relation.getTotalOrders() + orders);
        return this.updateById(relation);
    }

    @Override
    public Long getCurrentGradeId(Long partyId) {
        PartyGradeRelation relation = getCurrentRelation(partyId);
        return relation != null ? relation.getGradeId() : null;
    }

    @Override
    public String getCurrentGradeName(Long partyId) {
        PartyGradeRelation relation = getCurrentRelation(partyId);
        return relation != null ? relation.getGradeName() : null;
    }

    @Override
    public boolean canUpgrade(Long partyId) {
        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        if (currentRelation == null) {
            return false;
        }

        CustomerGrade nextGrade = customerGradeService.getNextGrade(currentRelation.getGradeId());
        if (nextGrade == null) {
            return false;
        }

        return currentRelation.getTotalAmount().compareTo(nextGrade.getMinAmount()) >= 0;
    }

    @Override
    public boolean shouldDowngrade(Long partyId) {
        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        if (currentRelation == null) {
            return false;
        }

        CustomerGrade currentGrade = customerGradeService.getById(currentRelation.getGradeId());
        if (currentGrade == null || currentGrade.getMaxAmount() == null) {
            return false;
        }

        return currentRelation.getTotalAmount().compareTo(currentGrade.getMaxAmount()) < 0;
    }

    @Override
    public Long getNextGradeId(Long partyId) {
        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        if (currentRelation == null) {
            return null;
        }

        CustomerGrade nextGrade = customerGradeService.getNextGrade(currentRelation.getGradeId());
        return nextGrade != null ? nextGrade.getId() : null;
    }

    @Override
    public Integer getUpgradeProgress(Long partyId) {
        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        if (currentRelation == null) {
            return 0;
        }

        CustomerGrade nextGrade = customerGradeService.getNextGrade(currentRelation.getGradeId());
        if (nextGrade == null || nextGrade.getMinAmount() == null) {
            return 100;
        }

        BigDecimal currentAmount = currentRelation.getTotalAmount();
        BigDecimal targetAmount = nextGrade.getMinAmount();

        if (currentAmount.compareTo(targetAmount) >= 0) {
            return 100;
        }

        return currentAmount.multiply(BigDecimal.valueOf(100))
            .divide(targetAmount, 0, RoundingMode.HALF_UP)
            .intValue();
    }

    @Override
    public BigDecimal getAmountToNextGrade(Long partyId) {
        PartyGradeRelation currentRelation = getCurrentRelation(partyId);
        if (currentRelation == null) {
            return BigDecimal.ZERO;
        }

        return customerGradeService.calculateAmountToNextGrade(
            currentRelation.getGradeId(),
            currentRelation.getTotalAmount()
        );
    }

    @Override
    @Transactional
    public boolean extendGradeExpireTime(Long partyId, Integer days) {
        PartyGradeRelation relation = getCurrentRelation(partyId);
        if (relation == null) {
            return false;
        }

        LocalDateTime currentExpireTime = relation.getExpireTime();
        if (currentExpireTime == null) {
            currentExpireTime = LocalDateTime.now();
        }

        relation.setExpireTime(currentExpireTime.plusDays(days));
        return this.updateById(relation);
    }

    @Override
    @Transactional
    public boolean cancelManualAdjust(Long partyId) {
        PartyGradeRelation relation = getCurrentRelation(partyId);
        if (relation == null || !relation.getManualAdjust()) {
            return false;
        }

        relation.setManualAdjust(false);
        relation.setAdjustBy(null);
        relation.setAdjustReason(null);
        return this.updateById(relation);
    }
}
