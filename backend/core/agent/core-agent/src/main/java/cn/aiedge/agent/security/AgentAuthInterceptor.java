package cn.aiedge.agent.security;

import cn.aiedge.agent.entity.Agent;
import cn.aiedge.agent.mapper.AgentMapper;
import cn.aiedge.agent.protocol.AgentCallContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AgentAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_AGENT_ID = "X-Agent-Id";
    private static final String PREFIX_BEARER = "Bearer ";

    private final AgentMapper agentMapper;

    public AgentAuthInterceptor(AgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/agent/invoke")) {
            return true;
        }

        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        String agentId = request.getHeader(HEADER_AGENT_ID);

        if (authHeader == null || !authHeader.startsWith(PREFIX_BEARER)) {
            response.setStatus(401);
            response.getWriter().write("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32001,\"message\":\"Missing or invalid Authorization header\"},\"id\":null}");
            return false;
        }

        String apiKey = authHeader.substring(PREFIX_BEARER.length()).trim();
        Agent agent = agentMapper.selectByApiKey(apiKey);

        if (agent == null) {
            response.setStatus(401);
            response.getWriter().write("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32001,\"message\":\"Invalid API Key\"},\"id\":null}");
            return false;
        }

        if (agent.getStatus() != 1) {
            response.setStatus(403);
            response.getWriter().write("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32005,\"message\":\"Agent is disabled or not activated\"},\"id\":null}");
            return false;
        }

        AgentCallContext context = new AgentCallContext();
        context.setAgentId(String.valueOf(agent.getId()));
        context.setAgentCode(agent.getAgentCode());
        context.setTenantId(agent.getTenantId());
        context.setAgentId(agentId);
        AgentCallContextHolder.set(context);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AgentCallContextHolder.clear();
    }
}
