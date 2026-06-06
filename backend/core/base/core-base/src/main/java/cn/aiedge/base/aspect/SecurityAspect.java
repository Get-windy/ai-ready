package cn.aiedge.base.aspect;

import cn.aiedge.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
public class SecurityAspect {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(select|insert|update|delete|drop|truncate|union|exec|execute)\\b|\\bscript\\b|\\balert\\b)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script[^>]*>|javascript:|\\bon\\w+\\s*=|alert\\(|confirm\\(|prompt\\()",
        Pattern.CASE_INSENSITIVE
    );

    private static final int RATE_LIMIT_REQUESTS = 100;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        try {
            if (redisTemplate != null) {
                checkRateLimit(clientIp, requestUri);
            }
            checkSqlInjection(joinPoint.getArgs());
            checkXssAttack(joinPoint.getArgs());

            if (isSensitiveOperation(requestUri, requestMethod)) {
                logSensitiveOperation(request, joinPoint);
            }

            Object result = joinPoint.proceed();
            log.debug("请求处理成功: {} {} from {}", requestMethod, requestUri, clientIp);
            return result;

        } catch (BusinessException e) {
            log.warn("安全检查拦截: {} {} from {} - {}", requestMethod, requestUri, clientIp, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("安全检查异常: {} {} from {}", requestMethod, requestUri, clientIp, e);
            throw e;
        }
    }

    private void checkRateLimit(String clientIp, String requestUri) {
        if (redisTemplate == null) return;
        
        String rateKey = String.format("rate:limit:%s:%s", clientIp, requestUri);
        String globalRateKey = String.format("rate:limit:%s:global", clientIp);

        Long currentCount = redisTemplate.opsForValue().increment(rateKey);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(rateKey, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        Long globalCount = redisTemplate.opsForValue().increment(globalRateKey);
        if (globalCount != null && globalCount == 1) {
            redisTemplate.expire(globalRateKey, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (currentCount != null && currentCount > RATE_LIMIT_REQUESTS) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }

        if (globalCount != null && globalCount > RATE_LIMIT_REQUESTS * 2) {
            throw new BusinessException(429, "全局请求过于频繁，请稍后再试");
        }
    }

    private void checkSqlInjection(Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            if (arg == null) continue;
            String argStr = arg.toString();
            if (SQL_INJECTION_PATTERN.matcher(argStr).find()) {
                log.warn("检测到SQL注入攻击: {}", argStr);
                throw new BusinessException(400, "请求参数包含非法字符");
            }
        }
    }

    private void checkXssAttack(Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            if (arg == null) continue;
            String argStr = arg.toString();
            if (XSS_PATTERN.matcher(argStr).find()) {
                log.warn("检测到XSS攻击: {}", argStr);
                throw new BusinessException(400, "请求参数包含非法脚本");
            }
        }
    }

    private boolean isSensitiveOperation(String requestUri, String requestMethod) {
        if ("DELETE".equals(requestMethod)) return true;
        String[] sensitivePatterns = {"/password", "/reset", "/assign", "/permission", "/role"};
        for (String pattern : sensitivePatterns) {
            if (requestUri.contains(pattern)) return true;
        }
        return false;
    }

    private void logSensitiveOperation(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        try {
            String clientIp = getClientIp(request);
            String requestUri = request.getRequestURI();
            String requestMethod = request.getMethod();
            log.info("敏感操作审计: method={}, uri={}, ip={}", requestMethod, requestUri, clientIp);
        } catch (Exception e) {
            log.error("记录敏感操作日志失败", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
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
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}