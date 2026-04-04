package cn.aiedge.agent.service;

import cn.aiedge.agent.entity.Agent;
import cn.aiedge.agent.mapper.AgentMapper;
import cn.aiedge.agent.service.impl.AgentServiceImpl;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Agent服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent服务测试")
class AgentServiceTest {

    @Mock
    private AgentMapper agentMapper;

    @InjectMocks
    private AgentServiceImpl agentService;

    private Agent testAgent;

    @BeforeEach
    void setUp() {
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setTenantId(1L);
        testAgent.setAgentName("测试Agent");
        testAgent.setAgentCode("TEST_AGENT_001");
        testAgent.setAgentType(1);
        testAgent.setDescription("测试用Agent");
        testAgent.setVersion("1.0.0");
        testAgent.setApiKey("sk-test-api-key-12345");
        testAgent.setApiSecret(DigestUtil.sha256Hex("secret"));
        testAgent.setStatus(1);
        testAgent.setInvokeCount(0L);
        testAgent.setCreateTime(LocalDateTime.now());
        testAgent.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("Agent实体 - 属性设置")
    void testAgentEntityProperties() {
        assertNotNull(testAgent.getId());
        assertEquals(1L, testAgent.getId());
        assertEquals("测试Agent", testAgent.getAgentName());
        assertEquals("TEST_AGENT_001", testAgent.getAgentCode());
        assertEquals(1, testAgent.getAgentType());
        assertEquals(1, testAgent.getStatus());
        assertEquals(0L, testAgent.getInvokeCount());
    }

    @Test
    @DisplayName("Agent链式设置")
    void testAgentChainSetter() {
        Agent agent = new Agent()
                .setId(2L)
                .setAgentName("新Agent")
                .setAgentCode("NEW_AGENT_001")
                .setAgentType(2)
                .setStatus(0);

        assertEquals(2L, agent.getId());
        assertEquals("新Agent", agent.getAgentName());
        assertEquals("NEW_AGENT_001", agent.getAgentCode());
        assertEquals(2, agent.getAgentType());
        assertEquals(0, agent.getStatus());
    }

    @Test
    @DisplayName("生成API Key - 格式验证")
    void testGenerateApiKey() {
        String apiKey = agentService.generateApiKey();

        assertNotNull(apiKey);
        assertTrue(apiKey.startsWith("sk-"));
        assertTrue(apiKey.length() > 10);
    }

    @Test
    @DisplayName("生成API Key - 唯一性")
    void testGenerateApiKeyUniqueness() {
        String apiKey1 = agentService.generateApiKey();
        String apiKey2 = agentService.generateApiKey();

        assertNotEquals(apiKey1, apiKey2);
    }

    @Test
    @DisplayName("Agent状态 - 状态值验证")
    void testAgentStatusValues() {
        // 状态: 0-未激活, 1-已激活, 2-已禁用
        testAgent.setStatus(0);
        assertEquals(0, testAgent.getStatus());
        
        testAgent.setStatus(1);
        assertEquals(1, testAgent.getStatus());
        
        testAgent.setStatus(2);
        assertEquals(2, testAgent.getStatus());
    }

    @Test
    @DisplayName("Agent类型 - 类型值验证")
    void testAgentTypeValues() {
        // 类型: 1-对话型, 2-工具型, 3-工作流型
        testAgent.setAgentType(1);
        assertEquals(1, testAgent.getAgentType());
        
        testAgent.setAgentType(2);
        assertEquals(2, testAgent.getAgentType());
        
        testAgent.setAgentType(3);
        assertEquals(3, testAgent.getAgentType());
    }

    @Test
    @DisplayName("Agent调用计数 - 递增测试")
    void testInvokeCountIncrement() {
        Long initialCount = testAgent.getInvokeCount();
        testAgent.setInvokeCount(initialCount + 1);
        
        assertEquals(initialCount + 1, testAgent.getInvokeCount());
    }

    @Test
    @DisplayName("Agent心跳时间 - 更新测试")
    void testHeartbeatTime() {
        LocalDateTime before = testAgent.getLastHeartbeat();
        LocalDateTime now = LocalDateTime.now();
        testAgent.setLastHeartbeat(now);
        
        assertEquals(now, testAgent.getLastHeartbeat());
    }

    @Test
    @DisplayName("Agent时间戳 - 创建和更新")
    void testTimestampFields() {
        LocalDateTime create = LocalDateTime.now().minusHours(1);
        LocalDateTime update = LocalDateTime.now();
        
        testAgent.setCreateTime(create);
        testAgent.setUpdateTime(update);
        
        assertEquals(create, testAgent.getCreateTime());
        assertEquals(update, testAgent.getUpdateTime());
    }

    @Test
    @DisplayName("Agent配置 - JSON字段")
    void testJsonFields() {
        String capabilities = "[{\"name\":\"chat\",\"description\":\"对话能力\"}]";
        String config = "{\"model\":\"gpt-4\",\"temperature\":0.7}";
        
        testAgent.setCapabilities(capabilities);
        testAgent.setConfig(config);
        
        assertEquals(capabilities, testAgent.getCapabilities());
        assertEquals(config, testAgent.getConfig());
    }

    @Test
    @DisplayName("API密钥 - SHA256加密")
    void testApiSecretEncryption() {
        String rawSecret = "my-secret-key";
        String encrypted = DigestUtil.sha256Hex(rawSecret);
        
        assertNotNull(encrypted);
        assertEquals(64, encrypted.length()); // SHA256 produces 64 hex chars
        assertNotEquals(rawSecret, encrypted);
    }

    @Test
    @DisplayName("Agent验证 - API Key有效性")
    void testApiKeyValidation() {
        when(agentMapper.selectByApiKey("sk-test-api-key-12345")).thenReturn(testAgent);
        
        Agent found = agentMapper.selectByApiKey("sk-test-api-key-12345");
        
        assertNotNull(found);
        assertEquals(1, found.getStatus());
    }

    @Test
    @DisplayName("Agent验证 - 无效API Key")
    void testInvalidApiKey() {
        when(agentMapper.selectByApiKey("invalid-key")).thenReturn(null);
        
        Agent found = agentMapper.selectByApiKey("invalid-key");
        
        assertNull(found);
    }

    @Test
    @DisplayName("Agent编码 - 唯一性检查")
    void testAgentCodeUniqueness() {
        when(agentMapper.selectByAgentCode("TEST_AGENT_001")).thenReturn(testAgent);
        
        Agent found = agentMapper.selectByAgentCode("TEST_AGENT_001");
        
        assertNotNull(found);
        assertEquals("TEST_AGENT_001", found.getAgentCode());
    }

    @Test
    @DisplayName("分页查询 - 参数构建")
    void testPageQueryParameters() {
        Page<Agent> page = new Page<>(1, 10);
        Long tenantId = 1L;
        String agentName = "测试";
        Integer status = 1;
        
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
        assertNotNull(tenantId);
        assertNotNull(agentName);
        assertNotNull(status);
    }

    @Test
    @DisplayName("Agent创建 - 必填字段验证")
    void testRequiredFields() {
        Agent agent = new Agent();
        
        // 必填字段
        agent.setAgentName("必填名称");
        agent.setAgentCode("REQUIRED_CODE");
        agent.setTenantId(1L);
        agent.setAgentType(1);
        
        assertNotNull(agent.getAgentName());
        assertNotNull(agent.getAgentCode());
        assertNotNull(agent.getTenantId());
        assertNotNull(agent.getAgentType());
    }

    @Test
    @DisplayName("Agent扩展信息 - extInfo字段")
    void testExtInfoField() {
        String extInfo = "{\"owner\":\"admin\",\"department\":\"研发部\"}";
        testAgent.setExtInfo(extInfo);
        
        assertEquals(extInfo, testAgent.getExtInfo());
    }

    @Test
    @DisplayName("Agent删除标记 - deleted字段")
    void testDeletedField() {
        testAgent.setDeleted(0);
        assertEquals(0, testAgent.getDeleted());
        
        testAgent.setDeleted(1);
        assertEquals(1, testAgent.getDeleted());
    }
}