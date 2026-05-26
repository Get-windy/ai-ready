package com.aiready.system.aspect;

import com.aiready.system.entity.SystemLog;
import com.aiready.system.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统日志AOP切面
 * 自动记录Controller层的操作日志
 */
@Aspect
@Component
public class SystemLogAspect {

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 定义切点：所有Controller层的方法
     */
    @Pointcut("execution(* com.aiready..controller..*.*(..))")
    public void controllerPointcut() {}

    /**
     * 方法执行后记录日志
     */
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, null, result);
    }

    /**
     * 方法抛出异常后记录日志
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    /**
     * 处理日志记录
     */
    private void handleLog(JoinPoint joinPoint, Exception e, Object result) {
        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // 构建日志对象
            SystemLog log = new SystemLog();
            log.setLogType(1); // 操作日志
            log.setCreateTime(LocalDateTime.now());
            
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            // 设置操作类型和模块名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = signature.getName();
            log.setMethod(className + "." + methodName + "()");
            
            // 根据类名推断模块
            log.setModuleName(extractModuleName(className));
            log.setTitle(extractOperationTitle(methodName));
            log.setOperationType(extractOperationType(methodName));
            
            // 请求信息
            log.setRequestUrl(request.getRequestURI());
            log.setRequestMethod(request.getMethod());
            log.setIpAddress(getIpAddress(request));
            
            // 请求参数
            String requestParams = getRequestParams(joinPoint);
            log.setRequestParams(requestParams);
            
            // 执行时长（简化版）
            log.setExecutionTime(0L);
            
            // 操作状态
            if (e != null) {
                log.setStatus(0); // 失败
                log.setErrorMsg(e.getMessage());
            } else {
                log.setStatus(1); // 成功
            }
            
            // 用户信息（需要集成认证模块后获取）
            log.setUserId(getCurrentUserId());
            log.setUsername(getCurrentUsername());
            
            // 保存日志
            systemLogService.save(log);
            
        } catch (Exception ex) {
            // 日志记录失败不影响主流程
            ex.printStackTrace();
        }
    }

    /**
     * 提取模块名称
     */
    private String extractModuleName(String className) {
        if (className.contains(".party.")) return "往来单位管理";
        if (className.contains(".system.")) return "系统管理";
        if (className.contains(".finance.")) return "财务管理";
        if (className.contains(".permission.")) return "权限管理";
        if (className.contains(".menu.")) return "菜单管理";
        if (className.contains(".dict.")) return "字典管理";
        if (className.contains(".log.")) return "日志管理";
        return "其他";
    }

    /**
     * 提取操作标题
     */
    private String extractOperationTitle(String methodName) {
        if (methodName.startsWith("list") || methodName.startsWith("query") || methodName.startsWith("get")) {
            return "查询操作";
        }
        if (methodName.startsWith("add") || methodName.startsWith("create") || methodName.startsWith("save")) {
            return "新增操作";
        }
        if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")) {
            return "更新操作";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除操作";
        }
        if (methodName.startsWith("export")) {
            return "导出操作";
        }
        if (methodName.startsWith("import")) {
            return "导入操作";
        }
        return "其他操作";
    }

    /**
     * 提取操作类型
     */
    private String extractOperationType(String methodName) {
        if (methodName.startsWith("list") || methodName.startsWith("query") || methodName.startsWith("get")) {
            return "SELECT";
        }
        if (methodName.startsWith("add") || methodName.startsWith("create") || methodName.startsWith("save")) {
            return "INSERT";
        }
        if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")) {
            return "UPDATE";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        }
        if (methodName.startsWith("export")) {
            return "EXPORT";
        }
        if (methodName.startsWith("import")) {
            return "IMPORT";
        }
        return "OTHER";
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            
            Map<String, Object> params = new HashMap<>();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            
            for (int i = 0; i < args.length; i++) {
                if (paramNames != null && i < paramNames.length) {
                    Object arg = args[i];
                    // 过滤掉不需要记录的参数类型
                    if (arg instanceof HttpServletRequest 
                        || arg instanceof HttpServletResponse 
                        || arg instanceof MultipartFile) {
                        continue;
                    }
                    params.put(paramNames[i], arg);
                }
            }
            
            return params.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取当前用户ID（需集成认证模块）
     */
    private Long getCurrentUserId() {
        // TODO: 从Sa-Token或Session中获取当前用户ID
        return 1L;
    }

    /**
     * 获取当前用户名（需集成认证模块）
     */
    private String getCurrentUsername() {
        // TODO: 从Sa-Token或Session中获取当前用户名
        return "admin";
    }
}
