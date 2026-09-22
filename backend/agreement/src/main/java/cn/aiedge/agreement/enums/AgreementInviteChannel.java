package cn.aiedge.agreement.enums;

/**
 * 邀请的送达 / 领取渠道（§13.5「留痕：谁、何时、通过哪个渠道领取或查看了它」）。
 *
 * <p>后端**不生成二维码图片**：本仓没有任何二维码依赖（实测无 zxing / 无二维码工具类），
 * 因此后端只返回"可供前端渲染二维码的短链 / 邀请码"，渲染留给前端。
 * 渠道字段记录的就是"这份邀请是从二维码扫开的还是点链接打开的"。</p>
 */
public enum AgreementInviteChannel {

    /** 二维码扫码 */
    QRCODE("二维码"),

    /** 链接 / 短链 */
    LINK("链接");

    private final String label;

    AgreementInviteChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 解析渠道名；为空或无法识别时返回 {@link #LINK}（默认按链接处理，不报错 —— 渠道只是留痕字段）。 */
    public static AgreementInviteChannel parseOrLink(String name) {
        if (name == null || name.isBlank()) {
            return LINK;
        }
        String n = name.trim();
        for (AgreementInviteChannel c : values()) {
            if (c.name().equalsIgnoreCase(n)) {
                return c;
            }
        }
        return LINK;
    }

    public static String labelOf(String name) {
        if (name == null) {
            return null;
        }
        for (AgreementInviteChannel c : values()) {
            if (c.name().equalsIgnoreCase(name.trim())) {
                return c.label;
            }
        }
        return name;
    }
}
