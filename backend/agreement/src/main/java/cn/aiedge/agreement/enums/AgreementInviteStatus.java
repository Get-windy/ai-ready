package cn.aiedge.agreement.enums;

/**
 * 邀请（唯一送达）状态 —— 对应 {@code agreement_invite.status}。
 *
 * <p>三种失效**分别可断言**（§13.5）：过期 = {@link #EXPIRED}、被发起方撤回 = {@link #REVOKED}、
 * 已使用 = {@link #ACCEPTED}。把它们混成一个"无效"会让用户不知道该找谁重发，
 * 也让验证脚本断言不出"到底为什么打不开"。</p>
 */
public enum AgreementInviteStatus {

    /** 0 待领取：token 有效、未过期、未被撤回 */
    PENDING(0, "待领取"),

    /** 1 已领取（**一次性**：用掉即失效，再看请走协议详情页） */
    ACCEPTED(1, "已领取"),

    /** 2 被发起方撤回 */
    REVOKED(2, "已被发起方撤回"),

    /** 3 已过期 */
    EXPIRED(3, "已过期");

    private final int code;
    private final String label;

    AgreementInviteStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AgreementInviteStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgreementInviteStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        AgreementInviteStatus s = of(code);
        return s == null ? "未知" : s.label;
    }
}
