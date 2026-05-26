package com.aiready.party.service;

import com.aiready.party.entity.PartyGradeRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 往来单位等级关系服务接口
 */
public interface PartyGradeRelationService extends IService<PartyGradeRelation> {

    /**
     * 根据往来单位ID获取当前等级关系
     */
    PartyGradeRelation getCurrentRelation(Long partyId);

    /**
     * 根据往来单位ID获取历史等级记录
     */
    List<PartyGradeRelation> listHistoryRelations(Long partyId);

    /**
     * 分配默认等级给往来单位
     */
    boolean assignDefaultGrade(Long partyId);

    /**
     * 手动设置往来单位等级
     */
    boolean manualSetGrade(Long partyId, Long gradeId, Long operatorId, String reason);

    /**
     * 升级往来单位等级
     */
    boolean upgradeGrade(Long partyId, Long newGradeId);

    /**
     * 降级往来单位等级
     */
    boolean downgradeGrade(Long partyId, Long newGradeId, String reason);

    /**
     * 检查并更新等级（根据消费金额自动调整）
     */
    boolean checkAndUpdateGrade(Long partyId);

    /**
     * 批量检查并更新等级
     */
    void batchCheckAndUpdateGrades();

    /**
     * 更新累计消费金额
     */
    boolean updateTotalAmount(Long partyId, BigDecimal amount);

    /**
     * 更新累计订单数
     */
    boolean updateTotalOrders(Long partyId, Integer orders);

    /**
     * 获取往来单位当前等级ID
     */
    Long getCurrentGradeId(Long partyId);

    /**
     * 获取往来单位当前等级名称
     */
    String getCurrentGradeName(Long partyId);

    /**
     * 判断往来单位是否达到升级条件
     */
    boolean canUpgrade(Long partyId);

    /**
     * 判断往来单位是否需要降级
     */
    boolean shouldDowngrade(Long partyId);

    /**
     * 获取下一等级信息
     */
    Long getNextGradeId(Long partyId);

    /**
     * 获取升级进度（百分比）
     */
    Integer getUpgradeProgress(Long partyId);

    /**
     * 获取升级还需金额
     */
    BigDecimal getAmountToNextGrade(Long partyId);

    /**
     * 延长等级有效期
     */
    boolean extendGradeExpireTime(Long partyId, Integer days);

    /**
     * 取消手动调整
     */
    boolean cancelManualAdjust(Long partyId);
}
