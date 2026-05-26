package cn.aiedge.assistant.service;

import cn.aiedge.assistant.model.AssistantRequest;
import cn.aiedge.assistant.model.AssistantResponse;

/**
 * 智能助手服务接口
 * 提供对话式交互的智能助手功能
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface AssistantService {

    /**
     * 处理用户消息并返回助手响应
     *
     * @param request 助手请求
     * @return 助手响应
     */
    AssistantResponse processMessage(AssistantRequest request);

    /**
     * 获取对话历史
     *
     * @param sessionId 会话 ID
     * @param limit     数量限制
     * @return 对话历史列表
     */
    java.util.List<cn.aiedge.assistant.entity.Conversation> getConversationHistory(String sessionId, Integer limit);

    /**
     * 创建新会话
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return 会话 ID
     */
    String createNewSession(Long userId, Long tenantId);

    /**
     * 清除会话历史
     *
     * @param sessionId 会话 ID
     * @return 是否成功
     */
    boolean clearSessionHistory(String sessionId);

    /**
     * 执行助手操作
     *
     * @param sessionId 会话 ID
     * @param action    操作类型
     * @param parameters 操作参数
     * @return 执行结果
     */
    Object executeAction(String sessionId, String action, java.util.Map<String, Object> parameters);

    /**
     * 获取系统功能列表
     *
     * @param tenantId 租户 ID
     * @return 功能列表
     */
    java.util.List<String> getAvailableFunctions(Long tenantId);
}
