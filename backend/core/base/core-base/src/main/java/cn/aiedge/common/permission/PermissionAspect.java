package cn.aiedge.common.permission;

import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 权限校验切面
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class PermissionAspect {

    /**
     * 权限注解切面
     */
    @Around("@annotation(cn.aiedge.common.permission.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        
        if (annotation == null) {
            return joinPoint.proceed();
        }
        
        // 检查登录
        if (!StpUtil.isLogin()) {
            throw BusinessException.unauthorized("用户未登录");
        }
        
        String[] permissions = annotation.value();
        if (permissions.length == 0) {
            return joinPoint.proceed();
        }
        
        // 获取用户权限列表
        List<String> userPermissions = StpUtil.getPermissionList();
        
        // 通配符 * 表示拥有所有权限（超级管理员）
        boolean isWildcard = userPermissions.contains("*");

        boolean hasPermission;
        if (annotation.logical() == RequiresPermission.Logical.AND) {
            // AND逻辑：必须拥有所有权限
            hasPermission = isWildcard || Arrays.stream(permissions)
                .allMatch(userPermissions::contains);
        } else {
            // OR逻辑：拥有任一权限即可
            hasPermission = isWildcard || Arrays.stream(permissions)
                .anyMatch(userPermissions::contains);
        }
        
        if (!hasPermission) {
            log.warn("权限不足: userId={}, required={}, actual={}", 
                StpUtil.getLoginId(), Arrays.toString(permissions), userPermissions);
            throw BusinessException.forbidden("权限不足");
        }
        
        return joinPoint.proceed();
    }

    /**
     * 角色注解切面
     */
    @Around("@annotation(cn.aiedge.common.permission.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);
        
        if (annotation == null) {
            return joinPoint.proceed();
        }
        
        // 检查登录
        if (!StpUtil.isLogin()) {
            throw BusinessException.unauthorized("用户未登录");
        }
        
        String[] roles = annotation.value();
        if (roles.length == 0) {
            return joinPoint.proceed();
        }
        
        // 获取用户角色列表
        List<String> userRoles = StpUtil.getRoleList();
        
        boolean hasRole;
        if (annotation.logical() == RequiresRole.Logical.AND) {
            // AND逻辑：必须拥有所有角色
            hasRole = Arrays.stream(roles)
                .allMatch(userRoles::contains);
        } else {
            // OR逻辑：拥有任一角色即可
            hasRole = Arrays.stream(roles)
                .anyMatch(userRoles::contains);
        }
        
        if (!hasRole) {
            log.warn("角色不足: userId={}, required={}, actual={}", 
                StpUtil.getLoginId(), Arrays.toString(roles), userRoles);
            throw BusinessException.forbidden("角色不足");
        }
        
        return joinPoint.proceed();
    }
}
