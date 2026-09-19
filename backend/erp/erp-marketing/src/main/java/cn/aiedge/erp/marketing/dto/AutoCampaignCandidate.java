package cn.aiedge.erp.marketing.dto;

import lombok.Data;

/** 营销自动化候选会员（含触发依据，供预览与执行共用） */
@Data
public class AutoCampaignCandidate {

    private Long partnerId;
    private String partyCode;
    private String partyName;
    private String memberName;
    private String memberCardNo;
    private String mobile;
    /** 触发依据说明（如「生日还有 3 天」「沉睡 95 天」「积分 80 分将于 12 天后过期」） */
    private String triggerNote;
    /** 生日 / 最近交易 / 卡到期 / 积分到期 等关键时点（页面展示用） */
    private String keyDate;
}
