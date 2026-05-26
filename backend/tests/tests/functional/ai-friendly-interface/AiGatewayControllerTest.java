package cn.aiedge.erp.ai.controller;

import cn.aiedge.erp.ai.model.request.AiRequest;
import cn.aiedge.erp.ai.model.response.AiResponse;
import cn.aiedge.erp.ai.service.AiGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI网关控制器单元测试
 * 
 * 对应测试用例：TC-UNIT-001, TC-UNIT-002, TC-UNIT-003
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(AiGatewayController.class)
public class AiGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiGatewayService aiGatewayService;

    @Autowired
    private ObjectMapper objectMapper;

    private AiRequest validAiRequest;
    private AiResponse successAiResponse;

    @BeforeEach
    public void setUp() {
        // 创建有效的请求对象
        validAiRequest = AiRequest.builder()
                .message("如何优化库存管理？")
                .context("商贸企业，年营业额5000万")
                .provider("auto")
                .endpoint("chat")
                .build();

        // 创建成功的响应对象
        successAiResponse = AiResponse.builder()
                .success(true)
                .data("{\"answer\": \"建议采用ABC分类法优化库存结构，实施定期盘点制度，建立安全库存机制。\"}")
                .metadata(AiResponse.Metadata.builder()
                        .provider("openai")
                        .model("gpt-3.5-turbo")
                        .latencyMs(245)
                        .costUsd(0.002)
                        .build())
                .build();
    }

    /**
     * TC-UNIT-001: 测试聊天接口正常响应
     * 验证AI聊天接口能正常处理有效请求并返回正确响应
     */
    @Test
    public void testChatEndpointSuccess() throws Exception {
        // 模拟服务层返回成功响应
        when(aiGatewayService.processChat(any(AiRequest.class), any()))
                .thenReturn(successAiResponse);

        // 发送请求并验证响应
        mockMvc.perform(post("/api/ai/gateway/chat")
                .header("X-Request-ID", "test-request-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAiRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.metadata.provider").value("openai"))
                .andExpect(jsonPath("$.metadata.latencyMs").value(245))
                .andExpect(jsonPath("$.metadata.costUsd").value(0.002));
    }

    /**
     * TC-UNIT-002: 测试请求验证失败场景
     * 验证接口对无效请求的正确拒绝
     */
    @Test
    public void testChatEndpointValidationError() throws Exception {
        // 创建无效请求（空消息）
        AiRequest invalidRequest = AiRequest.builder()
                .message("")  // 空消息，违反@NotBlank约束
                .context("测试上下文")
                .provider("openai")
                .build();

        // 发送请求并验证验证错误
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    /**
     * 测试超长消息验证失败
     * 边界测试：消息长度超过8000字符
     */
    @Test
    public void testChatEndpointMessageTooLong() throws Exception {
        // 创建超长消息请求
        String longMessage = "X".repeat(8001);  // 8001字符，超过@Size(max=8000)约束
        AiRequest longMessageRequest = AiRequest.builder()
                .message(longMessage)
                .context("测试上下文")
                .provider("openai")
                .build();

        // 发送请求并验证验证错误
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longMessageRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * 测试无效服务提供商验证
     */
    @Test
    public void testChatEndpointInvalidProvider() throws Exception {
        // 创建无效服务提供商请求
        AiRequest invalidProviderRequest = AiRequest.builder()
                .message("测试消息")
                .context("测试上下文")
                .provider("invalid_provider")  // 不在允许的提供商列表中
                .build();

        // 发送请求并验证验证错误
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProviderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * TC-UNIT-003: 测试服务层异常处理
     * 验证服务层异常能正确转换为API错误响应
     */
    @Test
    public void testChatEndpointServiceError() throws Exception {
        // 模拟服务层抛出异常
        when(aiGatewayService.processChat(any(AiRequest.class), any()))
                .thenThrow(new RuntimeException("AI服务暂时不可用"));

        // 发送请求并验证错误响应
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAiRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("AI服务暂时不可用"))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }

    /**
     * 测试批量聊天接口
     */
    @Test
    public void testBatchChatEndpoint() throws Exception {
        // 创建批量请求对象
        AiRequest batchRequest = AiRequest.builder()
                .message("请分析销售数据")
                .context("批量处理测试")
                .provider("auto")
                .endpoint("batch_chat")
                .build();

        // 模拟批量处理响应
        AiResponse batchResponse = AiResponse.builder()
                .success(true)
                .data("{\"batchId\": \"batch-123\", \"status\": \"processing\"}")
                .metadata(AiResponse.Metadata.builder()
                        .provider("openai")
                        .model("gpt-3.5-turbo")
                        .latencyMs(500)
                        .costUsd(0.01)
                        .build())
                .build();

        when(aiGatewayService.processChat(any(AiRequest.class), any()))
                .thenReturn(batchResponse);

        // 发送批量请求
        mockMvc.perform(post("/api/ai/gateway/batch/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").value("batch-123"))
                .andExpect(jsonPath("$.data.status").value("processing"));
    }

    /**
     * 测试健康检查接口
     */
    @Test
    public void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get("/api/ai/gateway/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试带有X-Request-ID头的请求
     */
    @Test
    public void testRequestWithRequestId() throws Exception {
        when(aiGatewayService.processChat(any(AiRequest.class), any()))
                .thenReturn(successAiResponse);

        String requestId = "test-req-" + System.currentTimeMillis();

        mockMvc.perform(post("/api/ai/gateway/chat")
                .header("X-Request-ID", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAiRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", requestId))
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 测试内容类型验证
     */
    @Test
    public void testInvalidContentType() throws Exception {
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.TEXT_PLAIN)  // 错误的Content-Type
                .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    /**
     * 测试空请求体
     */
    @Test
    public void testEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试JSON格式错误
     */
    @Test
    public void testInvalidJsonFormat() throws Exception {
        mockMvc.perform(post("/api/ai/gateway/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}