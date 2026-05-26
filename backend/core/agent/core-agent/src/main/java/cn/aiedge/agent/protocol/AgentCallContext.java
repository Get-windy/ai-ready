package cn.aiedge.agent.protocol;

import lombok.Data;

import java.util.Map;

@Data
public class AgentCallContext {

    private String sessionId;

    private Long userId;

    private String agentId;

    private String agentCode;

    private Long tenantId;

    private Map<String, Object> extra;
}
