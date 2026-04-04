package cn.aiedge.base.config;

import cn.aiedge.base.security.StpInterfaceImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.aiedge.base.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 安全拦截器
 * 检查Token黑名单，增强安全性
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityInterceptor implements HandlerInterceptor {

    private final StpInterfaceImpl stpInterface;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Token
        String token = StpUtil.getTokenValue();
        
        if (StrUtil.isNotBlank(token)) {
            // 检查Token是否在黑名单中（已登出）
            if (stpInterface.isBlacklisted(token)) {
                log.warn("检测到黑名单Token访问: uri={}, token={}", request.getRequestURI(), maskToken(token));
                
                // 返回401错误
                writeErrorResponse(response, 401, "Token已失效，请重新登录");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        
        PrintWriter writer = response.getWriter();
        writer.write(JSONUtil.toJsonStr(Result.fail(code, message)));
        writer.flush();
    }
    
    /**
     * 脱敏Token
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 16) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }
}
