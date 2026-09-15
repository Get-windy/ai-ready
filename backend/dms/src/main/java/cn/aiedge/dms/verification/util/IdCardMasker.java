package cn.aiedge.dms.verification.util;

/**
 * 身份证号「存储级」脱敏工具
 *
 * <p>与 {@code cn.aiedge.permission.jackson.DataMaskSerializer}（展示级脱敏，前 6 后 4）不同，
 * 本工具用于<b>落库前</b>的敏感信息最小化：只保留前 3 位与后 4 位，中间以 {@code *} 填充，
 * 系统任何位置都不保存证件号原文。</p>
 */
public final class IdCardMasker {

    private IdCardMasker() {
    }

    private static final int PREFIX = 3;
    private static final int SUFFIX = 4;

    /**
     * 存储级脱敏：前 3 后 4
     *
     * @param raw 证件号原文；为空或过短时返回空串（不保存）
     */
    public static String mask(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() <= PREFIX + SUFFIX) {
            return "*".repeat(value.length());
        }
        return value.substring(0, PREFIX)
                + "*".repeat(value.length() - PREFIX - SUFFIX)
                + value.substring(value.length() - SUFFIX);
    }

    /**
     * 是否已是脱敏值（含 {@code *} 视为已脱敏，避免二次脱敏破坏数据）
     */
    public static boolean isMasked(String value) {
        return value != null && value.indexOf('*') >= 0;
    }
}
