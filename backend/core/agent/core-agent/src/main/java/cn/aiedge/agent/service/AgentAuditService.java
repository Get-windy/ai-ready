package cn.aiedge.agent.service;

import cn.aiedge.agent.entity.AgentCallLog;
import cn.aiedge.agent.mapper.AgentCallLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent调用审计服务
 */
@Service
public class AgentAuditService {

    private final AgentCallLogMapper callLogMapper;
    private final ObjectMapper objectMapper;

    public AgentAuditService(AgentCallLogMapper callLogMapper, ObjectMapper objectMapper) {
        this.callLogMapper = callLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录调用日志
     */
    public void logCall(
            String requestId,
            Long agentId,
            String capabilityCode,
            Map<String, Object> requestParams,
            Object responseResult,
            Boolean success,
            String errorMessage,
            Long duration,
            String clientIp,
            Long userId,
            Long tenantId
    ) {
        try {
            AgentCallLog log = new AgentCallLog();
            log.setRequestId(requestId);
            log.setAgentId(agentId);
            log.setCapabilityCode(capabilityCode);
            log.setRequestParams(objectMapper.writeValueAsString(maskSensitiveData(requestParams)));
            log.setResponseResult(objectMapper.writeValueAsString(maskSensitiveData(responseResult)));
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);
            log.setDuration(duration);
            log.setCallTime(LocalDateTime.now());
            log.setClientIp(clientIp);
            log.setUserId(userId);
            log.setTenantId(tenantId);
            callLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败不影响主流程
            e.printStackTrace();
        }
    }

    /**
     * 简单的敏感数据脱敏
     */
    private Object maskSensitiveData(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            map.entrySet().forEach(entry -> {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key instanceof String) {
                    String keyStr = (String) key;
                    if (keyStr.toLowerCase().contains("password")
                            || keyStr.toLowerCase().contains("secret")
                            || keyStr.toLowerCase().contains("token")
                            || keyStr.toLowerCase().contains("key")) {
                        ((Map<String, Object>) map).put(keyStr, "******");
                    }
                }
            });
        }
        return data;
    }
}
