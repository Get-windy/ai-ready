package cn.aiedge.base.util;

/**
 * 个人信息脱敏工具（《个人信息保护法》最小必要原则）。
 *
 * <p>列表 / 台账类<b>只读展示</b>场景一律掩码：后台操作员处理业务并不需要完整号码，
 * 而一张列表页会被截图、导出、转发，完整手机号属高敏感个人信息。
 * <b>存储层与投递链路仍用明文</b>——短信下发、退订匹配都必须拿真实号码，
 * 所以掩码只发生在「出参给前端」这一刻。</p>
 *
 * <p>与同包的 {@code @DataMask} / {@code @DataMaskNumber} 注解分工：注解版依赖
 * {@code sys_field_permission} 的字段级权限配置、按角色动态决定是否脱敏（管理员可在
 * 「岗位权限 → 敏感信息保护」里开关）；本工具用于「该字段本身就是日志/台账性质，
 * 不需要任何角色看到全文」的场景，无需配置即生效。二者不重复，也不互相替代。</p>
 */
public final class DesensitizeUtils {

    private DesensitizeUtils() {
    }

    /**
     * 手机号掩码：保留前 3 后 4，中间以 * 填充（{@code 138****8888}）。
     * 非 11 位或过短的字符串原样返回——不确定格式时宁可不动，避免把「-」之类
     * 的占位值改得面目全非。
     */
    public static String mobile(String mobile) {
        if (mobile == null || mobile.isBlank()) return mobile;
        String v = mobile.trim();
        if (v.length() < 7) return v;
        int keepTail = 4;
        int maskLen = v.length() - 3 - keepTail;
        if (maskLen <= 0) return v;
        return v.substring(0, 3) + "*".repeat(maskLen) + v.substring(v.length() - keepTail);
    }
}
