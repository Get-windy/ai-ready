package cn.aiedge.common.core.utils;

/**
 * 安全工具类
 */
public class SecurityUtils {

    /**
     * 获取当前租户ID（字符串形式，与实体字段类型匹配）
     */
    public static String getTenantId() {
        try {
            // 从Sa-Token上下文获取登录ID（当前用户ID）
            // 租户ID在登录时已通过SaSession存储
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginId();
            // 开发环境默认返回租户1
            return "1";
        } catch (Exception e) {
            return "1";
        }
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        try {
            return cn.dev33.satoken.stp.StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "admin";
        }
    }

    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        try {
            return String.valueOf(cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            return "1";
        }
    }
}
