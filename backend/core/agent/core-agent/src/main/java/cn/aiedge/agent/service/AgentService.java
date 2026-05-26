package cn.aiedge.agent.service;

import cn.aiedge.agent.entity.Agent;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Agent服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface AgentService extends IService<Agent> {

    /**
     * 注册Agent
     */
    Long register(Agent agent);

    /**
     * 更新Agent信息
     */
    void updateAgent(Agent agent);

    /**
     * 注销Agent
     */
    void unregister(Long agentId);

    /**
     * 激活Agent
     */
    void activate(Long agentId);

    /**
     * 禁用Agent
     */
    void deactivate(Long agentId);

    /**
     * 刷新心跳
     */
    void heartbeat(String agentCode);

    /**
     * 根据Agent编码查询
     */
    Agent getByAgentCode(String agentCode);

    /**
     * 根据API Key查询
     */
    Agent getByApiKey(String apiKey);

    /**
     * 分页查询
     */
    Page<Agent> pageAgents(Page<Agent> page, Long tenantId, String agentName, Integer status);

    /**
     * 获取活跃Agent列表
     */
    List<Agent> getActiveAgents();

    /**
     * 生成API Key
     */
    String generateApiKey();

    /**
     * 验证API Key
     */
    boolean validateApiKey(String apiKey);

    /**
     * 增加调用次数
     */
    void incrementInvokeCount(Long agentId);
}