package cn.aiedge.common.core.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtils {
    
    /**
     * 获取当前租户ID
     */
    public static String getTenantId() {
        // 实际实现中应该从当前用户上下文中获取
        return "default";
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return "system";
    }
    
    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return "0";
    }
}