package cn.aiedge.assistant.controller;

import cn.aiedge.assistant.model.AssistantRequest;
import cn.aiedge.assistant.model.AssistantResponse;
import cn.aiedge.assistant.entity.Conversation;
import cn.aiedge.assistant.service.AssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能助手控制器
 * 提供智能助手相关的 REST API 接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Autowired
    private AssistantService assistantService;

    @PostMapping("/chat")
    public AssistantResponse chat(@RequestBody AssistantRequest request) {
        return assistantService.processMessage(request);
    }

    @PostMapping("/session/create")
    public String createSession(@RequestParam Long userId, @RequestParam Long tenantId) {
        return assistantService.createNewSession(userId, tenantId);
    }

    @GetMapping("/history")
    public List<Conversation> getHistory(@RequestParam String sessionId, 
                                         @RequestParam(defaultValue = "20") Integer limit) {
        return assistantService.getConversationHistory(sessionId, limit);
    }

    @DeleteMapping("/session")
    public boolean clearSession(@RequestParam String sessionId) {
        return assistantService.clearSessionHistory(sessionId);
    }

    @PostMapping("/action")
    public Object executeAction(@RequestParam String sessionId,
                               @RequestParam String action,
                               @RequestBody Map<String, Object> parameters) {
        return assistantService.executeAction(sessionId, action, parameters);
    }

    @GetMapping("/functions")
    public List<String> getFunctions(@RequestParam Long tenantId) {
        return assistantService.getAvailableFunctions(tenantId);
    }
}
