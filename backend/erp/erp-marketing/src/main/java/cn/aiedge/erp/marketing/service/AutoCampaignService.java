package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.dto.AutoCampaignCandidate;
import cn.aiedge.erp.marketing.entity.AutoCampaign;

import java.util.List;
import java.util.Map;

/**
 * 营销自动化（会员生命周期触达）。
 *
 * <p>执行链路：按触发点筛候选 → 频控/只一次判定 → 执行动作（复用券发放 / 短信群发 / 积分台账）
 * → 写执行台账。</p>
 */
public interface AutoCampaignService {

    /** 候选会员预览（不执行动作，供页面确认） */
    List<AutoCampaignCandidate> candidates(AutoCampaign campaign, int limit);

    /** 执行一条规则（返回执行统计） */
    Map<String, Object> run(Long campaignId, int limit);

    /** 执行全部启用规则（定时任务入口） */
    Map<String, Object> runAll(int limitPerCampaign);

    /** 演练统计：该租户全部启用规则的候选会员总数（不执行动作） */
    int candidatesCount(Long tenantId);
}
