package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.PartyGradeRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface PartyGradeRelationService extends IService<PartyGradeRelation> {

    PartyGradeRelation getCurrentRelation(Long partyId);

    List<PartyGradeRelation> listHistoryRelations(Long partyId);

    boolean assignDefaultGrade(Long partyId);

    boolean manualSetGrade(Long partyId, Long gradeId, Long operatorId, String reason);

    boolean upgradeGrade(Long partyId, Long newGradeId);

    boolean downgradeGrade(Long partyId, Long newGradeId, String reason);

    boolean checkAndUpdateGrade(Long partyId);

    void batchCheckAndUpdateGrades();

    boolean updateTotalAmount(Long partyId, BigDecimal amount);

    boolean updateTotalOrders(Long partyId, Integer orders);

    Long getCurrentGradeId(Long partyId);

    String getCurrentGradeName(Long partyId);

    boolean canUpgrade(Long partyId);

    boolean shouldDowngrade(Long partyId);

    Long getNextGradeId(Long partyId);

    Integer getUpgradeProgress(Long partyId);

    BigDecimal getAmountToNextGrade(Long partyId);

    boolean extendGradeExpireTime(Long partyId, Integer days);

    boolean cancelManualAdjust(Long partyId);
}
