package cn.aiedge.base.utils;

/**
 * 安全工具类
 *
 * 封装 Sa-Token 常用操作，提供静态方法供各处调用。
 * 替代硬编码的占位实现，所有方法均委托给 StpUtil 真实校验。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class SecurityUtils {

    /**
     * 获取当前用户ID
     * @return 用户ID，未登录时返回 null
     */
    public static Long getCurrentUserId() {
        try {
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdDefaultNull();
            return loginId != null ? Long.parseLong(loginId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户名（从 Sa-Token Session 中读取）
     * @return 用户名，未登录或未设置时返回 null
     */
    public static String getCurrentUsername() {
        try {
            if (!cn.dev33.satoken.stp.StpUtil.isLogin()) return null;
            Object username = cn.dev33.satoken.stp.StpUtil.getSession().get("username");
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前租户ID
     * 优先级：Sa-Token Session > SecurityContext 回退
     * @return 租户ID，未登录时返回 null
     */
    public static Long getCurrentTenantId() {
        try {
            if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
                Object tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId");
                if (tenantId != null) {
                    return Long.parseLong(tenantId.toString());
                }
            }
            // 回退到 SecurityContext（使用临时租户ID）
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public static boolean isLoggedIn() {
        try {
            return cn.dev33.satoken.stp.StpUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否拥有指定权限
     * @param permission 权限码
     * @return 是否拥有权限（未登录也返回 false）
     */
    public static boolean hasPermission(String permission) {
        try {
            return cn.dev33.satoken.stp.StpUtil.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否拥有指定角色
     * @param role 角色码
     * @return 是否拥有角色（未登录也返回 false）
     */
    public static boolean hasRole(String role) {
        try {
            return cn.dev33.satoken.stp.StpUtil.hasRole(role);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否拥有所有指定权限（AND 逻辑）
     * @param permissions 权限码数组
     * @return 是否拥有所有权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        try {
            for (String p : permissions) {
                if (!cn.dev33.satoken.stp.StpUtil.hasPermission(p)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否拥有任一指定权限（OR 逻辑）
     * @param permissions 权限码数组
     * @return 是否拥有任一权限
     */
    public static boolean hasAnyPermissions(String... permissions) {
        try {
            for (String p : permissions) {
                if (cn.dev33.satoken.stp.StpUtil.hasPermission(p)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
