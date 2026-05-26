package cn.aiedge.gateway.service;

import java.time.LocalDateTime;

/**
 * 网关日志服务接口
 * 提供请求日志记录、查询、统计等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface GatewayLogService {

    /**
     * 记录访问日志
     * 
     * @param requestId 请求ID
     * @param method HTTP方法
     * @param path 请求路径
     * @param queryString 查询字符串
     * @param remoteAddr 客户端IP
     * @param userAgent 用户代理
     * @param referer 引用页面
     * @param statusCode 响应状态码
     * @param duration 请求耗时(毫秒)
     * @param timestamp 时间戳
     */
    void logAccess(String requestId, String method, String path, String queryString,
                   String remoteAddr, String userAgent, String referer, int statusCode,
                   long duration, LocalDateTime timestamp);

    /**
     * 记录错误日志
     * 
     * @param requestId 请求ID
     * @param path 请求路径
     * @param error 错误信息
     * @param remoteAddr 客户端IP
     * @param timestamp 时间戳
     */
    void logError(String requestId, String path, String error, String remoteAddr, LocalDateTime timestamp);

    /**
     * 记录安全事件
     * 
     * @param requestId 请求ID
     * @param eventType 事件类型
     * @param path 请求路径
     * @param remoteAddr 客户端IP
     * @param details 详细信息
     * @param timestamp 时间戳
     */
    void logSecurityEvent(String requestId, String eventType, String path, String remoteAddr,
                         String details, LocalDateTime timestamp);
}
