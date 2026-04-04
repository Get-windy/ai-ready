package com.aiready.security.filter;

import cn.hutool.core.util.StrUtil;
import com.aiready.security.ApiSignatureUtil;
import com.aiready.security.SqlInjectionProtectionUtil;
import com.aiready.security.XssProtectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * 安全过滤器
 * 包含XSS防护、SQL注入防护、API签名验证
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "securityFilter")
@Order(1)
public class SecurityFilter implements Filter {

    @Autowired
    private XssProtectionUtil xssProtectionUtil;

    @Autowired
    private SqlInjectionProtectionUtil sqlInjectionProtectionUtil;

    @Autowired
    private ApiSignatureUtil apiSignatureUtil;

    @Value("${security.signature.enabled:false}")
    private Boolean signatureEnabled;

    @Value("${security.xss.enabled:true}")
    private Boolean xssEnabled;

    @Value("${security.sql.enabled:true}")
    private Boolean sqlEnabled;

    // 不需要签名验证的路径
    private static final List<String> SKIP_SIGNATURE_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/public",
        "/actuator/health"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 1. API签名验证
        if (signatureEnabled && !shouldSkipSignature(path)) {
            if (!verifySignature(httpRequest, httpResponse)) {
                return;
            }
        }

        // 2. XSS和SQL注入检查
        if (xssEnabled || sqlEnabled) {
            SecurityRequestWrapper wrappedRequest = new SecurityRequestWrapper(
                httpRequest, xssProtectionUtil, sqlInjectionProtectionUtil, xssEnabled, sqlEnabled
            );
            chain.doFilter(wrappedRequest, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkipSignature(String path) {
        return SKIP_SIGNATURE_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean verifySignature(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        String path = request.getRequestURI();

        if (StrUtil.hasEmpty(timestamp, nonce, signature)) {
            sendErrorResponse(response, 401, "缺少签名参数");
            return false;
        }

        // 构建参数Map
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }

        try {
            Long ts = Long.parseLong(timestamp);
            if (!apiSignatureUtil.verifySignature(ts, nonce, path, params, signature)) {
                sendErrorResponse(response, 401, "签名验证失败");
                return false;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, 401, "无效的时间戳");
            return false;
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }

    /**
     * 安全请求包装器
     */
    public static class SecurityRequestWrapper extends HttpServletRequestWrapper {
        private final XssProtectionUtil xssUtil;
        private final SqlInjectionProtectionUtil sqlUtil;
        private final boolean xssEnabled;
        private final boolean sqlEnabled;

        public SecurityRequestWrapper(HttpServletRequest request, 
                                       XssProtectionUtil xssUtil,
                                       SqlInjectionProtectionUtil sqlUtil,
                                       boolean xssEnabled, 
                                       boolean sqlEnabled) {
            super(request);
            this.xssUtil = xssUtil;
            this.sqlUtil = sqlUtil;
            this.xssEnabled = xssEnabled;
            this.sqlEnabled = sqlEnabled;
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanValue(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;

            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = cleanValue(values[i]);
            }
            return cleanValues;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> originalMap = super.getParameterMap();
            Map<String, String[]> cleanMap = new HashMap<>();

            for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
                String[] values = entry.getValue();
                String[] cleanValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    cleanValues[i] = cleanValue(values[i]);
                }
                cleanMap.put(entry.getKey(), cleanValues);
            }

            return cleanMap;
        }

        private String cleanValue(String value) {
            if (StrUtil.isEmpty(value)) return value;

            String cleanValue = value;

            if (xssEnabled) {
                cleanValue = xssUtil.cleanXSS(cleanValue);
            }

            if (sqlEnabled && sqlUtil.containsSqlInjection(cleanValue)) {
                // 检测到SQL注入，可以选择清除或抛出异常
                cleanValue = sqlUtil.cleanSqlInjection(cleanValue);
            }

            return cleanValue;
        }
    }
}