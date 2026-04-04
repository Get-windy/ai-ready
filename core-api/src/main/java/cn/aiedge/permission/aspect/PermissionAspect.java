package cn.aiedge.permission.aspect;

import cn.aiedge.permission.annotation.RequirePermission;
import cn.aiedge.permission.annotation.RequireRole;
import cn.aiedge.permission.exception.PermissionDeniedException;
import cn.aiedge.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 权限验证切面
 * 拦截带有 @RequirePermission 或 @RequireRole 注解的方法进行权限验证
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    /**
     * 权限验证切面
     */
    @Around("@annotation(cn.aiedge.permission.annotation.RequirePermission) || " +
            "@within(cn.aiedge.permission.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequirePermission.class);
        }
        
        if (annotation != null) {
            checkPermission(annotation);
        }
        
        return joinPoint.proceed();
    }

    /**
     * 角色验证切面
     */
    @Around("@annotation(cn.aiedge.permission.annotation.RequireRole) || " +
            "@within(cn.aiedge.permission.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RequireRole annotation = method.getAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
        }
        
        if (annotation != null) {
            checkRole(annotation);
        }
        
        return joinPoint.proceed();
    }

    /**
     * 检查权限
     */
    private void checkPermission(RequirePermission annotation) {
        String[] permissions = annotation.value();
        if (permissions.length == 0) {
            return;
        }

        // 获取当前用户权限
        Set<String> userPermissions = permissionService.getCurrentUserPermissions();
        
        // 超级管理员跳过检查
        if (permissionService.isSuperAdmin()) {
            return;
        }

        // 根据逻辑关系验证
        boolean hasPermission;
        if (annotation.logical() == RequirePermission.Logical.AND) {
            hasPermission = Arrays.stream(permissions)
                    .allMatch(userPermissions::contains);
        } else {
            hasPermission = Arrays.stream(permissions)
                    .anyMatch(userPermissions::contains);
        }

        if (!hasPermission) {
            log.warn("权限不足，需要权限: {}, 用户权限: {}", 
                    Arrays.toString(permissions), userPermissions);
            throw new PermissionDeniedException(
                    "权限不足，需要权限: " + Arrays.toString(permissions));
        }
    }

    /**
     * 检查角色
     */
    private void checkRole(RequireRole annotation) {
        String[] roles = annotation.value();
        if (roles.length == 0) {
            return;
        }

        // 获取当前用户角色
        Set<String> userRoles = permissionService.getCurrentUserRoles();
        
        // 超级管理员跳过检查
        if (permissionService.isSuperAdmin()) {
            return;
        }

        // 根据逻辑关系验证
        boolean hasRole;
        if (annotation.logical() == RequirePermission.Logical.AND) {
            hasRole = Arrays.stream(roles)
                    .allMatch(userRoles::contains);
        } else {
            hasRole = Arrays.stream(roles)
                    .anyMatch(userRoles::contains);
        }

        if (!hasRole) {
            log.warn("角色不足，需要角色: {}, 用户角色: {}", 
                    Arrays.toString(roles), userRoles);
            throw new PermissionDeniedException(
                    "角色不足，需要角色: " + Arrays.toString(roles));
        }
    }
}
