package cn.aiedge.base.log.aspect;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.log.entity.SystemLog;
import cn.aiedge.base.log.service.SystemLogService;
import cn.aiedge.base.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 拦截带有@OperationLog注解的方法，记录操作日志
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

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
        SystemLog systemLog = new SystemLog();
        
        // 设置日志基本信息
        systemLog.setLogType(1); // 操作日志
        systemLog.setLogLevel(exception != null ? "ERROR" : "INFO");
        systemLog.setTitle(annotation.desc().isEmpty() ? "操作日志" : annotation.desc());
        systemLog.setOperationType(annotation.type());
        systemLog.setModuleName(annotation.module());
        systemLog.setExecutionTime(executionTime);
        systemLog.setCreateTime(LocalDateTime.now());

        // 设置请求信息
        if (request != null) {
            systemLog.setRequestMethod(request.getMethod());
            systemLog.setRequestUrl(request.getRequestURI());
            systemLog.setIpAddress(getClientIp(request));
            systemLog.setBrowser(request.getHeader("User-Agent"));
        }

        // 设置业务信息
        if (!annotation.businessKey().isEmpty()) {
            systemLog.setBusinessKey(evaluateSpelExpression(annotation.businessKey(), method, args));
        }
        if (!annotation.businessType().isEmpty()) {
            systemLog.setBusinessType(evaluateSpelExpression(annotation.businessType(), method, args));
        }

        // 设置请求参数
        if (annotation.saveParams() && args != null && args.length > 0) {
            try {
                systemLog.setRequestParams(objectMapper.writeValueAsString(args));
            } catch (Exception e) {
                log.warn("序列化请求参数失败", e);
            }
        }

        // 设置响应结果
        if (annotation.saveResponse() && result != null) {
            try {
                systemLog.setResponseResult(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.warn("序列化响应结果失败", e);
            }
        }

        // 设置操作状态
        if (exception != null) {
            systemLog.setStatus(0); // 失败
            systemLog.setErrorMsg(exception.getMessage());
        } else {
            systemLog.setStatus(1); // 成功
        }

        // 设置用户信息
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String username = SecurityUtils.getCurrentUsername();
            if (userId != null) systemLog.setUserId(userId);
            if (username != null) systemLog.setUsername(username);
        } catch (Exception e) {
            log.warn("获取当前用户信息失败", e);
        }

        // 异步保存日志
        systemLogService.saveLogAsync(systemLog);
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

    /**
     * 解析SpEL表达式
     */
    private String evaluateSpelExpression(String expression, Method method, Object[] args) {
        try {
            Expression spelExpression = spelExpressionParser.parseExpression(expression);
            EvaluationContext context = SimpleEvaluationContext
                    .forReadOnlyDataBinding()
                    .withInstanceMethods()
                    .build();

            // 将方法参数绑定到上下文
            for (int i = 0; i < method.getParameterCount(); i++) {
                String paramName = "arg" + i;
                context.setVariable(paramName, args[i]);
            }

            return spelExpression.getValue(context, String.class);
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败: {}", expression, e);
            return expression;
        }
    }
}