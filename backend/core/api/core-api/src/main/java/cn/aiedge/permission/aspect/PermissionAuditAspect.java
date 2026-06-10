package cn.aiedge.permission.aspect;

import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.audit.service.AuditLogService;
import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.base.service.SysOperLogService;
import cn.aiedge.permission.annotation.RequirePermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限变更审计切面
 * <p>
 * 自动记录所有权限相关操作的审计日志，包括：
 * <ul>
 *   <li>角色创建、更新、删除</li>
 *   <li>权限分配、菜单分配</li>
 *   <li>单据类型权限分配</li>
 *   <li>用户角色分配</li>
 *   <li>权限模板应用</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAuditAspect {

    private final SysOperLogService operLogService;
    private final AuditLogService auditLogService;
    private final SecurityContext securityContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 切点定义 ====================

    /** 旧版角色管理操作（base 模块） */
    @Pointcut("execution(* cn.aiedge.base.controller.SysRoleController.*(..))")
    public void roleOperations() {}

    /** 新版角色管理操作（core-api 模块） */
    @Pointcut("execution(* cn.aiedge.role.controller.RoleController.*(..))")
    public void newRoleOperations() {}

    /** 旧版权限管理操作（PermissionControllerExt） */
    @Pointcut("execution(* cn.aiedge.permission.controller.PermissionControllerExt.*(..))")
    public void permissionOperations() {}

    /** 旧版权限管理操作（PermissionController - 含授权 API） */
    @Pointcut("execution(* cn.aiedge.permission.controller.PermissionController.assignRolePermissions(..)) || " +
              "execution(* cn.aiedge.permission.controller.PermissionController.assignUserRoles(..)) || " +
              "execution(* cn.aiedge.permission.controller.PermissionController.clearRolePermissions(..)) || " +
              "execution(* cn.aiedge.permission.controller.PermissionController.clearUserRoles(..))")
    public void grantOperations() {}

    /** 旧版用户角色分配 */
    @Pointcut("execution(* cn.aiedge.base.controller.SysUserController.assignRoles(..))")
    public void userRoleAssignment() {}

    /** 新版用户角色分配 */
    @Pointcut("execution(* cn.aiedge.user.controller.UserController.assignRoles(..))")
    public void newUserRoleAssignment() {}

    /** 菜单管理操作 */
    @Pointcut("execution(* cn.aiedge.base.controller.SysMenuController.*(..))")
    public void menuOperations() {}

    /** 角色继承管理 */
    @Pointcut("execution(* cn.aiedge.base.controller.RoleInheritanceController.*(..))")
    public void roleInheritanceOperations() {}

    /** 权限模板操作 */
    @Pointcut("execution(* cn.aiedge.base.controller.PermissionTemplateController.*(..))")
    public void permissionTemplateOperations() {}

    /** 单据类型权限分配 */
    @Pointcut("execution(* cn.aiedge.base.controller.RoleBillTypeController.assignBillTypes(..))")
    public void billTypeAssignment() {}

    /** 数据权限操作 */
    @Pointcut("execution(* cn.aiedge.base.controller.DataPermissionController.*(..))")
    public void dataPermissionOperations() {}

    /** 会话管理（强制下线等） */
    @Pointcut("execution(* cn.aiedge.base.controller.SessionController.kickout*(..)) || " +
              "execution(* cn.aiedge.base.controller.SessionController.disable*(..)) || " +
              "execution(* cn.aiedge.base.controller.SessionController.enable*(..))")
    public void sessionManagement() {}

    // ==================== 通知 ====================

    @AfterReturning(pointcut = "roleOperations() || newRoleOperations() || " +
                               "permissionOperations() || grantOperations() || " +
                               "userRoleAssignment() || newUserRoleAssignment() || " +
                               "menuOperations() || roleInheritanceOperations() || " +
                               "permissionTemplateOperations() || billTypeAssignment() || " +
                               "dataPermissionOperations() || sessionManagement()",
                    returning = "result")
    public void auditPermissionChange(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            // 跳过查询类操作（get/list/page/tree/check）
            if (methodName.startsWith("get") || methodName.startsWith("list") ||
                methodName.startsWith("page") || methodName.startsWith("tree") ||
                methodName.startsWith("check") || methodName.startsWith("export")) {
                return;
            }

            // 判断是否授权类操作
            boolean isGrantOp = methodName.startsWith("assign") ||
                                methodName.startsWith("batch") ||
                                methodName.startsWith("clear");

            // 构建 SysOperLog 日志
            SysOperLog operLog = new SysOperLog();
            operLog.setTenantId(securityContext.getCurrentTenantId());
            operLog.setUserId(securityContext.getCurrentUserId());
            operLog.setUsername(securityContext.getCurrentUsername());
            operLog.setModule(getModule(className));
            operLog.setAction(getAction(methodName, className));
            operLog.setMethod(className + "." + methodName);
            operLog.setRequestUrl(getRequestUrl());
            operLog.setOperIp(getClientIpFromContext());
            operLog.setStatus(0); // 成功
            operLog.setOperTime(LocalDateTime.now());
            operLog.setCostTime(0L);
            operLog.setRequestParams(buildRequestParams(joinPoint));

            // 异步写入 SysOperLog
            operLogService.recordLogAsync(operLog);

            // 对授权类操作，额外记录详细审计日志（含 beforeData/afterData）
            if (isGrantOp) {
                AuditLog auditLog = buildAuditLog(joinPoint, className, methodName);
                auditLogService.recordAsync(auditLog);
            }

        } catch (Exception e) {
            log.warn("记录权限审计日志失败: {}", e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    private String getModule(String className) {
        return switch (className) {
            case "SysRoleController", "RoleController" -> "角色管理";
            case "PermissionControllerExt", "PermissionController" -> "权限管理";
            case "SysUserController", "UserController" -> "用户管理";
            case "SysMenuController" -> "菜单管理";
            case "RoleInheritanceController" -> "角色继承";
            case "PermissionTemplateController" -> "权限模板";
            case "RoleBillTypeController" -> "单据类型权限";
            case "DataPermissionController" -> "数据权限";
            case "SessionController" -> "会话管理";
            default -> "权限系统";
        };
    }

    private String getAction(String methodName, String className) {
        if (methodName.startsWith("create") || methodName.startsWith("add")) return "新增";
        if (methodName.startsWith("update") || methodName.startsWith("edit")) return "修改";
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) return "删除";
        if (methodName.startsWith("assign") || methodName.startsWith("batch")) return "分配";
        if (methodName.startsWith("clear")) return "清除";
        if (methodName.startsWith("kickout")) return "强制下线";
        if (methodName.startsWith("disable")) return "禁用账号";
        if (methodName.startsWith("enable")) return "解除禁用";
        if (methodName.startsWith("apply")) return "应用";
        if (methodName.startsWith("set")) return "设置";
        if (methodName.startsWith("reset")) return "重置";
        return methodName;
    }

    private String getApiPrefix(String className) {
        return switch (className) {
            case "SysRoleController", "RoleController" -> "role";
            case "PermissionControllerExt", "PermissionController" -> "permission";
            case "SysUserController", "UserController" -> "user";
            case "SysMenuController" -> "menu";
            case "RoleInheritanceController" -> "role-inheritance";
            case "PermissionTemplateController" -> "permission-template";
            case "RoleBillTypeController" -> "role-bill-type";
            case "DataPermissionController" -> "data-permission";
            case "SessionController" -> "session";
            default -> "permission";
        };
    }

    private String buildRequestParams(JoinPoint joinPoint) {
        try {
            Map<String, Object> params = new HashMap<>();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] paramValues = joinPoint.getArgs();

            if (paramNames == null || paramValues == null) return "{}";

            for (int i = 0; i < Math.min(paramNames.length, paramValues.length); i++) {
                Object val = paramValues[i];
                // 对密码等敏感信息脱敏
                if (paramNames[i].toLowerCase().contains("password") ||
                    paramNames[i].toLowerCase().contains("secret")) {
                    params.put(paramNames[i], "****");
                } else if (val != null) {
                    params.put(paramNames[i], val.toString().length() > 500
                            ? val.toString().substring(0, 500) + "..."
                            : val.toString());
                }
            }
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{\"error\": \"failed to serialize params\"}";
        }
    }

    /**
     * 构建详细的权限变更审计日志（含 beforeData/afterData）
     */
    private AuditLog buildAuditLog(JoinPoint joinPoint, String className, String methodName) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(securityContext.getCurrentTenantId());
        auditLog.setAuditType(AuditLog.AuditType.GRANT.getCode());
        auditLog.setModule(getModule(className));
        auditLog.setAction(getAction(methodName, className));
        auditLog.setUserId(securityContext.getCurrentUserId());
        auditLog.setUsername(securityContext.getCurrentUsername());
        auditLog.setOperTime(LocalDateTime.now());
        auditLog.setResult("SUCCESS");
        auditLog.setRequestMethod(getRequestMethod());
        auditLog.setRequestUrl(getRequestUrl());
        auditLog.setRequestParams(buildRequestParams(joinPoint));
        auditLog.setOperIp(getClientIpFromContext());

        // 提取参数中的目标信息
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            // 第一个参数通常是目标ID（角色ID/用户ID）
            if (args[0] != null) {
                auditLog.setTargetId(String.valueOf(args[0]));
                auditLog.setTargetName(className + ":" + methodName);
            }
            // 如果有第二个参数而且是 List，记录为 afterData
            if (args.length > 1 && args[1] instanceof List) {
                try {
                    auditLog.setAfterData(objectMapper.writeValueAsString(args[1]));
                } catch (Exception e) {
                    log.warn("序列化afterData失败", e);
                }
            }
            // 如果是 PermissionController.assignRolePermissions，第二个参数是 tenantId，第三个是 permissionIds
            if (args.length > 2 && args[2] instanceof List) {
                try {
                    auditLog.setAfterData(objectMapper.writeValueAsString(args[2]));
                } catch (Exception e) {
                    log.warn("序列化permissionIds失败", e);
                }
            }
        }

        auditLog.setTargetType(className.contains("Role") || className.contains("role")
                ? "ROLE" : className.contains("User") || className.contains("user")
                ? "USER" : "PERMISSION");

        return auditLog;
    }

    private String getClientIpFromContext() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getRequestUrl() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getRequestURI();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getRequestMethod() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getMethod();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
