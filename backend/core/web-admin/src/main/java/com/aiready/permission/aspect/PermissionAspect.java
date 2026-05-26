package com.aiready.permission.aspect;

import com.aiready.permission.annotation.RequirePermission;
import com.aiready.permission.annotation.RequireRole;
import com.aiready.permission.service.PermissionService;
import com.aiready.permission.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 权限控制切面
 * 用于拦截带有@RequirePermission和@RequireRole注解的方法
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;
    private final RoleService roleService;

    /**
     * 定义切点 - 带有@RequirePermission注解的方法
     */
    @Pointcut("@annotation(com.aiready.permission.annotation.RequirePermission)")
    public void permissionPointcut() {}

    /**
     * 定义切点 - 带有@RequireRole注解的方法
     */
    @Pointcut("@annotation(com.aiready.permission.annotation.RequireRole)")
    public void rolePointcut() {}

    /**
     * 权限校验环绕通知
     */
    @Around("permissionPointcut()")
    public Object aroundPermission(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        
        Long userId = getCurrentUserId();
        
        // 校验权限
        String[] permissions = requirePermission.value();
        if (permissions.length > 0) {
            boolean hasPermission;
            if (requirePermission.logical() == RequirePermission.Logical.AND) {
                hasPermission = permissionService.hasAllPermissions(userId, permissions);
            } else {
                hasPermission = permissionService.hasAnyPermission(userId, permissions);
            }
            
            if (!hasPermission) {
                throw new RuntimeException("没有操作权限");
            }
        }
        
        // 校验角色
        String[] roles = requirePermission.roles();
        if (roles.length > 0) {
            boolean hasRole;
            if (requirePermission.logical() == RequirePermission.Logical.AND) {
                hasRole = roleService.hasAllRoles(userId, roles);
            } else {
                hasRole = roleService.hasAnyRole(userId, roles);
            }
            
            if (!hasRole) {
                throw new RuntimeException("没有角色权限");
            }
        }
        
        return point.proceed();
    }

    /**
     * 角色校验环绕通知
     */
    @Around("rolePointcut()")
    public Object aroundRole(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        
        Long userId = getCurrentUserId();
        String[] roles = requireRole.value();
        
        boolean hasRole;
        if (requireRole.logical() == RequireRole.Logical.AND) {
            hasRole = roleService.hasAllRoles(userId, roles);
        } else {
            hasRole = roleService.hasAnyRole(userId, roles);
        }
        
        if (!hasRole) {
            throw new RuntimeException("没有角色权限");
        }
        
        return point.proceed();
    }

    /**
     * 获取当前用户ID
     * 从请求头或Session中获取
     */
    private Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("无法获取当前请求");
        }
        
        HttpServletRequest request = attributes.getRequest();
        String userIdStr = request.getHeader("X-User-Id");
        
        if (userIdStr == null || userIdStr.isEmpty()) {
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj != null) {
                userIdStr = userIdObj.toString();
            }
        }
        
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new RuntimeException("未登录或登录已过期");
        }
        
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("用户ID格式错误");
        }
    }
}
