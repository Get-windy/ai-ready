package cn.aiedge.agent.controller;

import cn.aiedge.agent.entity.Agent;
import cn.aiedge.agent.service.AgentService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "Agent管理", description = "Agent注册与管理接口")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @Operation(summary = "注册Agent")
    @PostMapping("/register")
    @SaCheckPermission("agent:register")
    public Agent register(@RequestBody Agent agent) {
        Long agentId = agentService.register(agent);
        agent.setId(agentId);
        return agent;
    }

    @Operation(summary = "更新Agent")
    @PutMapping("/{id}")
    @SaCheckPermission("agent:update")
    public void updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        agent.setId(id);
        agentService.updateAgent(agent);
    }

    @Operation(summary = "注销Agent")
    @DeleteMapping("/{id}")
    @SaCheckPermission("agent:delete")
    public void unregister(@PathVariable Long id) {
        agentService.unregister(id);
    }

    @Operation(summary = "激活Agent")
    @PostMapping("/{id}/activate")
    @SaCheckPermission("agent:activate")
    public void activate(@PathVariable Long id) {
        agentService.activate(id);
    }

    @Operation(summary = "禁用Agent")
    @PostMapping("/{id}/deactivate")
    @SaCheckPermission("agent:deactivate")
    public void deactivate(@PathVariable Long id) {
        agentService.deactivate(id);
    }

    @Operation(summary = "心跳")
    @PostMapping("/heartbeat")
    public void heartbeat(@RequestParam String agentCode) {
        agentService.heartbeat(agentCode);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("agent:list")
    public Page<Agent> pageAgents(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) Integer status) {
        Page<Agent> page = new Page<>(current, size);
        return agentService.pageAgents(page, tenantId, agentName, status);
    }

    @Operation(summary = "获取Agent详情")
    @GetMapping("/{id}")
    @SaCheckPermission("agent:detail")
    public Agent getAgent(@PathVariable Long id) {
        return agentService.getById(id);
    }

    @Operation(summary = "获取活跃Agent列表")
    @GetMapping("/active")
    public List<Agent> getActiveAgents() {
        return agentService.getActiveAgents();
    }

    @Operation(summary = "验证API Key")
    @GetMapping("/validate")
    public boolean validateApiKey(@RequestParam String apiKey) {
        return agentService.validateApiKey(apiKey);
    }
}