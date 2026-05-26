package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import cn.aiedge.agent.protocol.AgentCallContext;
import cn.aiedge.agent.security.AgentCallContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@AgentCapability(code = "system", name = "系统能力", tags = {"system"})
public class SystemCapabilityProvider {

    @AgentCapability(
            code = "system.ping",
            name = "服务心跳检测",
            description = "检测Agent调用层服务是否正常运行",
            tags = {"system"},
            requireAuth = false
    )
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "timestamp", System.currentTimeMillis(),
                "version", "1.0.0",
                "service", "智企连·AI-Ready Agent调用层"
        );
    }

    @AgentCapability(
            code = "system.health",
            name = "系统健康检查",
            description = "全面的系统健康状态检查",
            tags = {"system", "monitoring"},
            requireAuth = false
    )
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "uptime", System.getProperty("startup.time", "unknown"),
                "java_version", System.getProperty("java.version"),
                "timestamp", System.currentTimeMillis()
        );
    }
}
