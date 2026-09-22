package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.Agreement;

/**
 * 「本方是协议的哪一端」——**由登录会话租户与两端 tenant 比对得出，不由前端传**。
 *
 * <p>为什么必须服务端判定：如果让前端传"我是甲方还是乙方"，那么任何人都可以
 * 声明自己是对方并替对方确认（双签就形同虚设）。会话租户是登录时写进 Sa-Token Session 的，
 * 客户端改不了。</p>
 */
public enum AgreementPartySide {

    /** 甲方（会话租户 = partyATenantId） */
    A,

    /** 乙方（会话租户 = partyBTenantId） */
    B,

    /** 既不是甲方也不是乙方：只能看得到"不存在"（fail-closed），不能代签 */
    NONE;

    /**
     * 按会话租户判定本方身份。
     *
     * @param agreement        协议主档（可为 null → 返回 NONE）
     * @param sessionTenantId  登录会话的租户 ID（可为 null → 返回 NONE）
     */
    public static AgreementPartySide of(Agreement agreement, Long sessionTenantId) {
        if (agreement == null || sessionTenantId == null) {
            return NONE;
        }
        if (sessionTenantId.equals(agreement.getPartyATenantId())) {
            return A;
        }
        if (sessionTenantId.equals(agreement.getPartyBTenantId())) {
            return B;
        }
        return NONE;
    }
}
