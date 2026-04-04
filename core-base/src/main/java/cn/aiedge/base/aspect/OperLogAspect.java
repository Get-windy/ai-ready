package cn.aiedge.base.aspect;

import cn.aiedge.base.annotation.OperLog;
import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.service.SysOperLogService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 操作日志切面
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogService operLogService;
    
    // 敏感字段
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "pwd", "oldPassword", "newPassword", "token", "secret");

    @Pointcut("@annotation(cn.aiedge.base.annotation.OperLog)")
    public void operLogPointcut() {}

    @AfterReturning(pointcut = "operLogPointcut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, null, result);
    }

    @AfterThrowing(pointcut = "operLogPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    private void handleLog(JoinPoint joinPoint, Exception e, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperLog operLogAnnotation = method.getAnnotation(OperLog.class);
            
            if (operLogAnnotation == null) {
                return;
            }

            HttpServletRequest request = getRequest();
            if (request == null) {
                return;
            }

            long startTime = (Long) request.getAttribute("startTime");
            long costTime = startTime > 0 ? System.currentTimeMillis() - startTime : 0;

            SysOperLog operLog = new SysOperLog();
            operLog.setModule(operLogAnnotation.module());
            operLog.setAction(operLogAnnotation.action());
            operLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName());
            operLog.setRequestUrl(request.getRequestURI());
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperTime(LocalDateTime.now());
            operLog.setCostTime(costTime);
            operLog.setOperIp(getClientIp(request));
            operLog.setStatus(e == null ? 0 : 1);
            
            if (e != null) {
                operLog.setErrorMsg(truncate(e.getMessage(), 500));
            }

            // 请求参数
            if (operLogAnnotation.recordParams()) {
                String params = getRequestParams(joinPoint, operLogAnnotation.ignoreSensitive());
                operLog.setRequestParams(truncate(params, 2000));
            }

            // 响应结果
            if (operLogAnnotation.recordResult() && result != null) {
                operLog.setResponseResult(truncate(JSONUtil.toJsonStr(result), 2000));
            }

            // 异步保存日志
            operLogService.recordLogAsync(operLog);

        } catch (Exception ex) {
            log.error("记录操作日志失败", ex);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getRequestParams(JoinPoint joinPoint, boolean ignoreSensitive) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args == null) {
            return "";
        }

        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Object value = args[i];

            // 跳过特殊类型
            if (value instanceof HttpServletRequest || 
                value instanceof HttpServletResponse || 
                value instanceof MultipartFile) {
                continue;
            }

            // 敏感字段脱敏
            if (ignoreSensitive && SENSITIVE_FIELDS.contains(name.toLowerCase())) {
                params.put(name, "******");
            } else {
                params.put(name, value);
            }
        }

        return JSONUtil.toJsonStr(params);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
