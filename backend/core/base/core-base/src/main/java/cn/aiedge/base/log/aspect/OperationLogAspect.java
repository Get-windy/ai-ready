package cn.aiedge.base.log.aspect;

import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.SysOperLogService;
import cn.aiedge.base.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * <p>
 * 拦截带有 @OperationLog 注解的方法，异步记录操作日志到 sys_oper_log 表。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperLogService sysOperLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLogAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        long startTime = System.currentTimeMillis();

        Object result = null;
        Exception exception = null;
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            try {
                saveOperLog(operationLogAnnotation, method, request, point.getArgs(), result, exception, executionTime);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    private void saveOperLog(OperationLog annotation, Method method, HttpServletRequest request,
                             Object[] args, Object result, Exception exception, long executionTime) {
        SysOperLog operLog = new SysOperLog();

        // 模块与操作
        operLog.setModule(annotation.module());
        operLog.setAction(annotation.type());
        operLog.setMethod(method.getName());

        // 请求信息
        if (request != null) {
            operLog.setRequestUrl(request.getRequestURI());
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperIp(getClientIp(request));
        }

        // 执行耗时
        operLog.setCostTime(executionTime);
        operLog.setOperTime(LocalDateTime.now());

        // 请求参数
        if (annotation.saveParams() && args != null && args.length > 0) {
            try {
                operLog.setRequestParams(objectMapper.writeValueAsString(args));
            } catch (Exception e) {
                log.warn("序列化请求参数失败", e);
            }
        }

        // 响应结果
        if (annotation.saveResponse() && result != null) {
            try {
                operLog.setResponseResult(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.warn("序列化响应结果失败", e);
            }
        }

        // 操作状态
        if (exception != null) {
            operLog.setStatus(1); // 失败
            operLog.setErrorMsg(exception.getMessage());
        } else {
            operLog.setStatus(0); // 成功
        }

        // 用户信息
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String username = SecurityUtils.getCurrentUsername();
            if (userId != null) {
                operLog.setUserId(userId);
                operLog.setUsername(username);
            }
        } catch (Exception e) {
            log.warn("获取当前用户信息失败", e);
        }

        // 异步保存
        sysOperLogService.recordLogAsync(operLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
