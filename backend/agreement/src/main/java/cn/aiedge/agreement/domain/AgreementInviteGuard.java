package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementInvite;
import cn.aiedge.agreement.enums.AgreementInviteStatus;
import cn.aiedge.common.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * 「这份契约是不是发给你的」—— 唯一送达的**判定收敛点**（§13.5）。
 *
 * <h3>为什么单独一个类</h3>
 * token 绑定的五件事（目标主体 / 目标租户 / 目标版本 / 时效 / 一次性）如果散在 Service 的
 * if 里，迟早有人漏判一条，而漏判的后果是**别人能打开你的契约**。
 * 与 {@link AgreementVisibility} 同一纪律：条件只在这里构造，Service 一律调用本类。
 *
 * <h3>判定顺序（刻意如此，别调换）</h3>
 * <ol>
 *   <li><b>存在性</b>：邀请不存在 ⇒ 404（不区分"没这条"与"被删了"）；</li>
 *   <li><b>必须登录</b>：未登录 ⇒ 401「请先登录后再打开这份契约邀请」（§13.5 硬要求）；</li>
 *   <li><b>身份匹配</b>：会话租户 + 所代表主体都要与目标一致 ⇒ 否则 403
 *       「<b>这份契约不是发给你的</b>」。
 *       ⚠️ 身份判定**放在状态判定之前**：这样"转发给别人"不管 token 是什么状态，
 *       对方看到的都是同一句"不是发给你的"，既不泄露邀请的状态，也不泄露它是否存在过。</li>
 *   <li><b>状态</b>：撤回 / 过期 / 已使用三种失效分别给出不同的中文提示（可分别断言）。</li>
 * </ol>
 *
 * <p>纯函数、不依赖 Spring 与数据库，因此每一种失配都能被单测直接覆盖。</p>
 */
public final class AgreementInviteGuard {

    private AgreementInviteGuard() {
    }

    /**
     * 判定本次打开是否放行；不放行则抛中文业务异常。
     *
     * @param invite              邀请记录（可为 null ⇒ 404）
     * @param sessionUserId       登录用户 ID（null ⇒ 未登录，401）
     * @param sessionTenantId     登录会话租户（null ⇒ 401）
     * @param representedPartyId  打开者声明"我代表哪个主体"（null ⇒ 400，必须先选主体）
     * @param now                 当前时刻（由调用方传入，便于单测控制过期）
     */
    public static void assertOpenable(AgreementInvite invite,
                                      Long sessionUserId,
                                      Long sessionTenantId,
                                      Long representedPartyId,
                                      LocalDateTime now) {
        // ① 存在性
        if (invite == null) {
            throw BusinessException.notFound("邀请链接无效或已不存在，请让对方重新发一份");
        }
        // ② 必须先登录（§13.5：打开链接的人必须先登录）
        if (sessionUserId == null || sessionTenantId == null) {
            throw BusinessException.unauthorized("请先登录后再打开这份契约邀请——契约只发给指定的那一家，登录后我们才能确认是发给你本人的");
        }
        // ③ 所代表主体必须由调用方明确给出（否则"会话租户对了"也可能是随便点开的人）
        if (representedPartyId == null) {
            throw BusinessException.badRequest("请先选择你代表哪个主体，再打开这份契约邀请");
        }
        // ③ 五绑定之「目标主体 + 目标租户」：不匹配就是"不是发给你的"
        if (!sessionTenantId.equals(invite.getTargetTenantId())
                || !representedPartyId.equals(invite.getTargetPartyId())) {
            throw BusinessException.forbidden("这份契约不是发给你的"
                    + "（邀请只对指定的那一方开放；转发给别人是打不开的，请让发起方重新发起一份）");
        }
        // ④ 三种失效**分别**给出不同提示（顺序刻意：撤回 → 过期 → 已领取，
        //    这样"已撤回"不会被"也过期了"遮住，三种失效才都能被断言出来）
        if (status(invite) == AgreementInviteStatus.REVOKED) {
            throw BusinessException.badRequest("这份邀请已被发起方撤回，请与对方确认后再让对方重新发一份");
        }
        // 时效：到点即失效（status 之外**再**看一次 expires_at，双保险）
        if (isExpired(invite, now)) {
            throw BusinessException.badRequest("这份邀请已过期（有效期至 "
                    + (invite.getExpiresAt() == null ? "未设置" : invite.getExpiresAt().toString().replace('T', ' '))
                    + "），请让对方重新发一份");
        }
        // 一次性：已领取就不能再用（再看请走协议详情页）
        if (status(invite) == AgreementInviteStatus.ACCEPTED) {
            throw BusinessException.badRequest("这份邀请已经被领取过了（一次性链接，不能重复使用）；"
                    + "想再看内容请直接打开「协议详情」");
        }
    }

    /** 是否已过期：status 标了 EXPIRED，或到达了 expires_at。 */
    public static boolean isExpired(AgreementInvite invite, LocalDateTime now) {
        if (invite == null) {
            return true;
        }
        if (status(invite) == AgreementInviteStatus.EXPIRED) {
            return true;
        }
        return invite.getExpiresAt() != null && now != null && !now.isBefore(invite.getExpiresAt());
    }

    /** 是否还在"待领取"（可被领取）：未过期、未撤回、未被领取。 */
    public static boolean isOpenable(AgreementInvite invite, LocalDateTime now) {
        if (invite == null) {
            return false;
        }
        return status(invite) == AgreementInviteStatus.PENDING && !isExpired(invite, now);
    }

    /**
     * 撤销 / 领取前的状态基线判断（与 {@link #assertOpenable} 分开：
     * 撤回由**发起方**做，不需要"所代表主体"匹配，只需要还是待领取状态）。
     */
    public static void assertRevocable(AgreementInvite invite, LocalDateTime now) {
        if (invite == null) {
            throw BusinessException.notFound("邀请不存在");
        }
        if (status(invite) == AgreementInviteStatus.ACCEPTED) {
            throw BusinessException.badRequest("这份邀请已被对方领取，不能再撤回；如需停止合作请走「终止」流程");
        }
        if (status(invite) == AgreementInviteStatus.REVOKED) {
            throw BusinessException.badRequest("这份邀请已经是撤回状态，无需重复撤回");
        }
        if (isExpired(invite, now)) {
            throw BusinessException.badRequest("这份邀请已过期，无需撤回");
        }
    }

    private static AgreementInviteStatus status(AgreementInvite invite) {
        AgreementInviteStatus s = AgreementInviteStatus.of(invite.getStatus());
        // 状态值异常（库里出现枚举外的取值）时按"待领取"处理会放大风险 ⇒ 按最保守的方式判成过期
        return s == null ? AgreementInviteStatus.EXPIRED : s;
    }
}
