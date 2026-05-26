package cn.aiedge.storage.permission;

import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

/**
 * 文件权限检查切面
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FilePermissionAspect {

    private final FilePermissionService permissionService;
    private final FileStorageService storageService;

    @Around("@annotation(requireFilePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, 
                                  RequireFilePermission requireFilePermission) throws Throwable {
        // 获取文件ID
        String fileId = extractFileId(joinPoint, requireFilePermission.fileIdParam());
        if (fileId == null) {
            throw new SecurityException("无法获取文件ID");
        }

        // 获取当前用户信息
        Long userId = getCurrentUserId();
        List<Long> roleIds = getCurrentUserRoleIds();
        List<Long> deptIds = getCurrentUserDeptIds();

        // 获取文件信息
        StorageFile fileInfo = storageService.getFileInfo(fileId);
        if (fileInfo == null) {
            throw new SecurityException("文件不存在");
        }

        // 检查是否是所有者
        if (permissionService.isOwner(fileInfo, userId)) {
            return joinPoint.proceed();
        }

        // 检查权限
        boolean hasPermission = permissionService.hasPermission(
                Long.parseLong(fileId), userId, requireFilePermission.value(), roleIds, deptIds);

        if (!hasPermission) {
            log.warn("Permission denied: userId={}, fileId={}, required={}",
                    userId, fileId, requireFilePermission.value());
            throw new SecurityException(requireFilePermission.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 从方法参数中提取文件ID
     */
    private String extractFileId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object arg = args[i];
                if (arg instanceof String) {
                    return (String) arg;
                } else if (arg instanceof Long) {
                    return arg.toString();
                }
            }
        }

        // 尝试从路径变量中获取
        for (int i = 0; i < parameters.length; i++) {
            org.springframework.web.bind.annotation.PathVariable pathVariable = 
                    parameters[i].getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
            if (pathVariable != null && 
                (pathVariable.value().isEmpty() || pathVariable.value().equals(paramName))) {
                Object arg = args[i];
                if (arg instanceof String) {
                    return (String) arg;
                } else if (arg instanceof Long) {
                    return arg.toString();
                }
            }
        }

        return null;
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 实际应从UserDetails中获取
            try {
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 获取当前用户角色ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> getCurrentUserRoleIds() {
        // 实际应从UserDetails或权限服务获取
        return Collections.emptyList();
    }

    /**
     * 获取当前用户部门ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> getCurrentUserDeptIds() {
        // 实际应从UserDetails或组织服务获取
        return Collections.emptyList();
    }
}
