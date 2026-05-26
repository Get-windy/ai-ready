package cn.aiedge.base.aspect;

import cn.aiedge.base.security.RbacService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 权限验证切面
 * 提供细粒度的权限验证功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionVerificationAspect {

    private final RbacService rbacService;

    /**
     * 围绕数据权限验证的切面方法
     * 在方法执行前后进行权限验证
     */
    @Around("@annotation(cn.aiedge.base.annotation.DataPermissionCheck)")
    public Object aroundDataPermissionCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 这里可以根据具体业务逻辑实现数据权限验证
        // 例如：检查用户是否有访问特定数据的权限
        
        log.debug("执行数据权限验证: userId={}", userId);
        
        // 继续执行原方法
        return joinPoint.proceed();
    }

    /**
     * 围绕业务权限验证的切面方法
     */
    @Around("@annotation(cn.aiedge.base.annotation.BusinessPermissionCheck)")
    public Object aroundBusinessPermissionCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 这里可以根据具体业务逻辑实现业务权限验证
        log.debug("执行业务权限验证: userId={}", userId);
        
        // 继续执行原方法
        return joinPoint.proceed();
    }
}