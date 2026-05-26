package com.aiready.log.aspect;

import com.aiready.log.annotation.OperationLog;
import com.aiready.log.service.OperationLogService;
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
 * 拦截带有@OperationLog注解的方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLogAnnotation) throws Throwable {
        // 获取方法信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 执行目标方法
        Object result = null;
        Exception exception = null;
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 保存操作日志
            try {
                saveOperationLog(operationLogAnnotation, method, request, point.getArgs(), result, exception, executionTime);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }
    
    /**
     * 保存操作日志
     */
    private void saveOperationLog(OperationLog annotation, Method method, HttpServletRequest request,
                                   Object[] args, Object result, Exception exception, long executionTime) {
        // 使用完全限定名来避免命名冲突
        com.aiready.log.entity.OperationLog operationLog = new com.aiready.log.entity.OperationLog();
        
        // 设置操作信息
        operationLog.setModule(annotation.module());
        operationLog.setOperationType(annotation.type());
        operationLog.setOperationDesc(annotation.desc());
        operationLog.setExecutionTime(executionTime);
        operationLog.setCreateTime(LocalDateTime.now());
        
        // 设置请求信息
        if (request != null) {
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setOperatorIp(getClientIp(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));
        }
        
        // 设置请求参数
        if (annotation.saveParams() && args != null && args.length > 0) {
            try {
                operationLog.setRequestParams(objectMapper.writeValueAsString(args));
            } catch (Exception e) {
                log.warn("序列化请求参数失败", e);
            }
        }
        
        // 设置响应结果
        if (annotation.saveResponse() && result != null) {
            try {
                operationLog.setResponseData(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.warn("序列化响应结果失败", e);
            }
        }
        
        // 设置操作状态
        if (exception != null) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(exception.getMessage());
        } else {
            operationLog.setStatus(1);
        }
        
        // TODO: 从SecurityContext获取当前用户信息
        // operationLog.setOperatorId(currentUser.getId());
        // operationLog.setOperatorName(currentUser.getUsername());
        
        // 异步保存日志
        operationLogService.saveLogAsync(operationLog);
    }
    
    /**
     * 获取客户端IP
     */
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
        // 多个代理情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}