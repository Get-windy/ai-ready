package cn.aiedge.dms.common.util;

import org.springframework.util.StringUtils;

/**
 * 敏感配置识别与脱敏
 *
 * 用于配置中心（`dms_config`）对「Key / 密钥 / 令牌 / 密码」类配置的**脱敏展示**：
 * 列表与详情接口只回掩码（如 `sk****cd12`）与是否已配置，明文仅在服务端使用；
 * 页面编辑时留空表示「不修改」，清除需显式调用清除接口（避免误把掩码写回库）。
 *
 * @author AI-Ready Team
 */
public final class SecretMasker {

    private SecretMasker() {
    }

    /** 敏感键名特征（小写包含匹配） */
    private static final String[] SECRET_KEYWORDS = {
            "api-key", "apikey", "secret", "password", "passwd", "token", "private-key", "access-key"
    };

    /**
     * 是否为敏感配置键
     */
    public static boolean isSecret(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return false;
        }
        String key = configKey.toLowerCase();
        for (String keyword : SECRET_KEYWORDS) {
            if (key.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 脱敏（保留首 2 位与末 4 位，便于运维核对；过短则整体掩码）
     */
    public static String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String v = value.trim();
        if (v.length() <= 6) {
            return "******";
        }
        return v.substring(0, 2) + "******" + v.substring(v.length() - 4);
    }
}
