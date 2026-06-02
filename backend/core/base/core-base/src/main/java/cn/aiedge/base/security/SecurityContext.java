package cn.aiedge.base.security;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.SysUserService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具类
 * 提供当前登录用户信息获取
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SecurityContext {

    private final SysUserService userService;

    /**
     * 临时租户ID（ThreadLocal）
     * 用于登录等未认证场景下，暂存当前操作的租户ID，避免多租户拦截器注入 tenant_id=0
     */
    private static final ThreadLocal<Long> TEMP_TENANT_ID = new ThreadLocal<>();

    /**
     * 设置临时租户ID（用于登录流程等未认证场景）
     */
    public void setTempTenantId(Long tenantId) {
        TEMP_TENANT_ID.set(tenantId);
    }

    /**
     * 清除临时租户ID
     */
    public void clearTempTenantId() {
        TEMP_TENANT_ID.remove();
    }

    /**
     * 获取当前登录用户ID
     */
    public Long getCurrentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId != null ? Long.parseLong(loginId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前登录用户
     */
    public SysUser getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userService.getUserDetail(userId);
    }

    /**
     * 获取当前登录用户名
     */
    public String getCurrentUsername() {
        SysUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前租户ID
     * 优先级：临时租户ID（ThreadLocal） > Sa-Token Session > 已登录用户的租户ID
     */
    public Long getCurrentTenantId() {
        // 1. 优先使用临时租户ID（用于登录等未认证场景，避免递归查询）
        Long tempTenantId = TEMP_TENANT_ID.get();
        if (tempTenantId != null) {
            return tempTenantId;
        }
        // 2. 从 Sa-Token Session 获取（登录时存入，避免递归调用 getCurrentUser）
        try {
            if (StpUtil.isLogin()) {
                Object sessionTenantId = StpUtil.getSession().get("tenantId");
                if (sessionTenantId != null) {
                    return Long.parseLong(sessionTenantId.toString());
                }
            }
        } catch (Exception ignored) {
            // session 不可用时忽略
        }
        // 3. 最后尝试从已登录用户获取（注意：可能触发多租户拦截器递归）
        SysUser user = getCurrentUser();
        return user != null ? user.getTenantId() : null;
    }

    /**
     * 判断当前用户是否已登录
     */
    public boolean isAuthenticated() {
        return StpUtil.isLogin();
    }

    /**
     * 判断当前用户是否具有指定角色
     */
    public boolean hasRole(String roleCode) {
        return StpUtil.hasRole(roleCode);
    }

    /**
     * 判断当前用户是否具有指定权限
     */
    public boolean hasPermission(String permissionCode) {
        return StpUtil.hasPermission(permissionCode);
    }

    /**
     * 检查当前用户是否具有指定角色，无则抛出异常
     */
    public void checkRole(String roleCode) {
        StpUtil.checkRole(roleCode);
    }

    /**
     * 检查当前用户是否具有指定权限，无则抛出异常
     */
    public void checkPermission(String permissionCode) {
        StpUtil.checkPermission(permissionCode);
    }

    /**
     * 获取当前用户的 Token
     */
    public String getToken() {
        return StpUtil.getTokenValue();
    }

    /**
     * 刷新当前用户的 Token
     */
    public void refreshToken() {
        // Sa-Token Token 自动续期
        StpUtil.renewTimeout(7200);
    }
}