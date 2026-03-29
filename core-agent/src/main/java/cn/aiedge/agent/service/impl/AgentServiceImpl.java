package cn.aiedge.agent.service.impl;

import cn.aiedge.agent.entity.Agent;
import cn.aiedge.agent.mapper.AgentMapper;
import cn.aiedge.agent.service.AgentService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> 
        implements AgentService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(Agent agent) {
        // 检查Agent编码是否已存在
        if (baseMapper.selectByAgentCode(agent.getAgentCode()) != null) {
            throw new RuntimeException("Agent编码已存在: " + agent.getAgentCode());
        }

        // 生成API Key和密钥
        agent.setApiKey(generateApiKey());
        agent.setApiSecret(DigestUtil.sha256Hex(IdUtil.fastSimpleUUID()));
        agent.setStatus(0); // 未激活
        agent.setInvokeCount(0L);
        agent.setCreateTime(LocalDateTime.now());
        agent.setUpdateTime(LocalDateTime.now());
        agent.setCreateBy(StpUtil.getLoginIdAsLong());

        save(agent);
        log.info("注册Agent成功: agentId={}, agentCode={}", agent.getId(), agent.getAgentCode());
        return agent.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(Agent agent) {
        agent.setUpdateTime(LocalDateTime.now());
        updateById(agent);
        log.info("更新Agent成功: agentId={}", agent.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unregister(Long agentId) {
        removeById(agentId);
        log.info("注销Agent成功: agentId={}", agentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long agentId) {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setStatus(1); // 已激活
        agent.setUpdateTime(LocalDateTime.now());
        updateById(agent);
        log.info("激活Agent成功: agentId={}", agentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long agentId) {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setStatus(2); // 已禁用
        agent.setUpdateTime(LocalDateTime.now());
        updateById(agent);
        log.info("禁用Agent成功: agentId={}", agentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(String agentCode) {
        Agent agent = baseMapper.selectByAgentCode(agentCode);
        if (agent == null) {
            throw new RuntimeException("Agent不存在: " + agentCode);
        }

        agent.setLastHeartbeat(LocalDateTime.now());
        updateById(agent);
        log.debug("Agent心跳更新: agentCode={}", agentCode);
    }

    @Override
    public Agent getByAgentCode(String agentCode) {
        return baseMapper.selectByAgentCode(agentCode);
    }

    @Override
    public Agent getByApiKey(String apiKey) {
        return baseMapper.selectByApiKey(apiKey);
    }

    @Override
    public Page<Agent> pageAgents(Page<Agent> page, Long tenantId, String agentName, Integer status) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Agent::getTenantId, tenantId)
                .like(agentName != null, Agent::getAgentName, agentName)
                .eq(status != null, Agent::getStatus, status)
                .orderByDesc(Agent::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<Agent> getActiveAgents() {
        return baseMapper.selectActiveAgents();
    }

    @Override
    public String generateApiKey() {
        return "sk-" + IdUtil.fastSimpleUUID().replace("-", "");
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        Agent agent = getByApiKey(apiKey);
        return agent != null && agent.getStatus() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementInvokeCount(Long agentId) {
        Agent agent = getById(agentId);
        if (agent != null) {
            agent.setInvokeCount(agent.getInvokeCount() + 1);
            updateById(agent);
        }
    }
}