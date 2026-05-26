package cn.aiedge.gateway.service.impl;

import cn.aiedge.gateway.entity.GatewayLog;
import cn.aiedge.gateway.mapper.GatewayLogMapper;
import cn.aiedge.gateway.service.GatewayLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 网关日志服务实现
 * 实现请求日志记录、查询、统计等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class GatewayLogServiceImpl implements GatewayLogService {

    @Autowired
    private GatewayLogMapper gatewayLogMapper;

    @Override
    public void logAccess(String requestId, String method, String path, String queryString,
                          String remoteAddr, String userAgent, String referer, int statusCode,
                          long duration, LocalDateTime timestamp) {
        GatewayLog log = new GatewayLog();
        log.setRequestId(requestId);
        log.setLogType("ACCESS"); // 访问日志
        log.setMethod(method);
        log.setPath(path);
        log.setQueryString(queryString);
        log.setRemoteAddr(remoteAddr);
        log.setUserAgent(userAgent);
        log.setReferer(referer);
        log.setStatusCode(statusCode);
        log.setDuration(duration);
        log.setTimestamp(timestamp);
        log.setCreateTime(LocalDateTime.now());

        // 保存到数据库
        gatewayLogMapper.insert(log);
    }

    @Override
    public void logError(String requestId, String path, String error, String remoteAddr, LocalDateTime timestamp) {
        GatewayLog log = new GatewayLog();
        log.setRequestId(requestId);
        log.setLogType("ERROR"); // 错误日志
        log.setPath(path);
        log.setErrorMessage(error);
        log.setRemoteAddr(remoteAddr);
        log.setTimestamp(timestamp);
        log.setCreateTime(LocalDateTime.now());

        // 保存到数据库
        gatewayLogMapper.insert(log);
    }

    @Override
    public void logSecurityEvent(String requestId, String eventType, String path, String remoteAddr,
                                String details, LocalDateTime timestamp) {
        GatewayLog log = new GatewayLog();
        log.setRequestId(requestId);
        log.setLogType("SECURITY"); // 安全日志
        log.setEventType(eventType);
        log.setPath(path);
        log.setRemoteAddr(remoteAddr);
        log.setDetails(details);
        log.setTimestamp(timestamp);
        log.setCreateTime(LocalDateTime.now());

        // 保存到数据库
        gatewayLogMapper.insert(log);
    }
}
