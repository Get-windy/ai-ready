package cn.aiedge.agent.controller;

import cn.aiedge.agent.mapper.AgentMapper;
import cn.aiedge.agent.protocol.JsonRpcErrorDetail;
import cn.aiedge.agent.protocol.JsonRpcErrors;
import cn.aiedge.agent.protocol.JsonRpcRequest;
import cn.aiedge.agent.protocol.JsonRpcResponse;
import cn.aiedge.agent.registry.CapabilityDefinition;
import cn.aiedge.agent.registry.CapabilityRegistry;
import cn.aiedge.agent.security.AgentCallContextHolder;
import cn.aiedge.agent.service.AgentAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/invoke")
public class AgentInvokeController {

    private final CapabilityRegistry registry;
    private final AgentMapper agentMapper;
    private final ObjectMapper objectMapper;
    private final AgentAuditService auditService;

    public AgentInvokeController(CapabilityRegistry registry, AgentMapper agentMapper, ObjectMapper objectMapper, AgentAuditService auditService) {
        this.registry = registry;
        this.agentMapper = agentMapper;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<?> invoke(@RequestBody JsonRpcRequest request, HttpServletRequest httpRequest) {
        if (request.getId() == null) {
            return ResponseEntity.ok(JsonRpcResponse.error(null,
                    JsonRpcErrors.INVALID_REQUEST, "Missing request id", null));
        }
        if (!request.isValid()) {
            return ResponseEntity.ok(JsonRpcResponse.error(request.getId(),
                    JsonRpcErrors.INVALID_REQUEST, "Invalid JSON-RPC 2.0 request", null));
        }

        String method = request.getMethod();
        return switch (method) {
            case "capability.invoke" -> handleCapabilityInvoke(request, httpRequest);
            case "capability.list" -> handleCapabilityList(request);
            case "capability.search" -> handleCapabilitySearch(request);
            case "system.ping" -> handlePing(request);
            default -> ResponseEntity.ok(JsonRpcResponse.error(request.getId(),
                    JsonRpcErrors.METHOD_NOT_FOUND, "Unknown method: " + method, null));
        };
    }

    @PostMapping("/batch")
    public ResponseEntity<?> invokeBatch(@RequestBody List<JsonRpcRequest> requests, HttpServletRequest httpRequest) {
        List<JsonRpcResponse> responses = requests.stream()
                .map(req -> {
                    if (req.getId() == null) {
                        return null;
                    }
                    ResponseEntity<?> resp = invoke(req, httpRequest);
                    return resp.getBody();
                })
                .filter(r -> r != null)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<JsonRpcResponse> handleCapabilityInvoke(JsonRpcRequest request, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        String requestId = String.valueOf(request.getId());
        String capabilityCode = null;
        String clientIp = getClientIp(httpRequest);
        Long agentId = null;
        Long userId = null;
        Long tenantId = null;

        try {
            Map<String, Object> params = request.getParams();
            if (params == null || !params.containsKey("capability_id")) {
                return ResponseEntity.ok(JsonRpcResponse.error(request.getId(),
                        JsonRpcErrors.INVALID_PARAMS, "Missing capability_id in params", null));
            }

            capabilityCode = (String) params.get("capability_id");
            CapabilityDefinition capability = registry.getCapability(capabilityCode);

            if (capability == null) {
                return ResponseEntity.ok(JsonRpcResponse.error(request.getId(),
                        JsonRpcErrors.CAPABILITY_NOT_FOUND, "Capability not found: " + capabilityCode, null));
            }
            if (!capability.isEnabled()) {
                return ResponseEntity.ok(JsonRpcResponse.error(request.getId(),
                        JsonRpcErrors.CAPABILITY_DISABLED, "Capability is disabled: " + capabilityCode, null));
            }

            Map<String, Object> context = params.containsKey("context")
                    ? (Map<String, Object>) params.get("context")
                    : Map.of();

            Map<String, Object> arguments = params.containsKey("arguments")
                    ? (Map<String, Object>) params.get("arguments")
                    : Map.of();

            agentId = AgentCallContextHolder.getContext().getAgentId();
            userId = context.get("user_id") instanceof Number ? ((Number) context.get("user_id")).longValue() : null;
            tenantId = context.get("tenant_id") instanceof Number ? ((Number) context.get("tenant_id")).longValue() : null;

            agentMapper.incrementInvokeCount(capabilityCode);

            Object result;
            if (capability.getTargetMethod() == null) {
                result = Map.of("status", "registered", "capability", capabilityCode);
            } else {
                Method method = capability.getTargetMethod();
                Object targetBean = capability.getTargetBean();

                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 2 && Map.class.isAssignableFrom(paramTypes[0])
                        && Map.class.isAssignableFrom(paramTypes[1])) {
                    result = method.invoke(targetBean, context, arguments);
                } else if (paramTypes.length == 1 && Map.class.isAssignableFrom(paramTypes[0])) {
                    result = method.invoke(targetBean, arguments);
                } else if (paramTypes.length == 0) {
                    result = method.invoke(targetBean);
                } else {
                    Object convertedArgs = objectMapper.convertValue(arguments, paramTypes[0]);
                    result = method.invoke(targetBean, convertedArgs);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            auditService.logCall(requestId, agentId, capabilityCode, arguments, result, true, null, duration, clientIp, userId, tenantId);
            return ResponseEntity.ok(JsonRpcResponse.success(request.getId(), result));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            auditService.logCall(requestId, agentId, capabilityCode, null, null, false, e.getMessage(), duration, clientIp, userId, tenantId);
            return ResponseEntity.ok(JsonRpcResponse.error(request.getId(),
                    JsonRpcErrors.INTERNAL_ERROR,
                    "Capability execution failed: " + e.getMessage(),
                    e.getClass().getSimpleName()));
        }
    }

    private ResponseEntity<JsonRpcResponse> handleCapabilityList(JsonRpcRequest request) {
        Collection<CapabilityDefinition> capabilities = registry.listCapabilities();
        List<Map<String, Object>> result = capabilities.stream()
                .map(c -> Map.<String, Object>of(
                        "code", c.getCode(),
                        "name", c.getName(),
                        "description", c.getDescription(),
                        "tags", c.getTags(),
                        "version", c.getVersion(),
                        "enabled", c.isEnabled()
                ))
                .toList();
        return ResponseEntity.ok(JsonRpcResponse.success(request.getId(),
                Map.of("count", result.size(), "capabilities", result)));
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<JsonRpcResponse> handleCapabilitySearch(JsonRpcRequest request) {
        Map<String, Object> params = request.getParams();
        String tag = params != null ? (String) params.getOrDefault("tag", "") : "";
        List<CapabilityDefinition> found = registry.searchByTag(tag);
        List<Map<String, Object>> result = found.stream()
                .map(c -> Map.<String, Object>of(
                        "code", c.getCode(),
                        "name", c.getName(),
                        "description", c.getDescription(),
                        "tags", c.getTags()
                ))
                .toList();
        return ResponseEntity.ok(JsonRpcResponse.success(request.getId(),
                Map.of("count", result.size(), "capabilities", result)));
    }

    private ResponseEntity<JsonRpcResponse> handlePing(JsonRpcRequest request) {
        return ResponseEntity.ok(JsonRpcResponse.success(request.getId(),
                Map.of("status", "ok", "timestamp", System.currentTimeMillis(),
                        "capabilities", registry.getCapabilityCount())));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
