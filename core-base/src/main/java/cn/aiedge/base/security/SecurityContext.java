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
     */
    public Long getCurrentTenantId() {
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