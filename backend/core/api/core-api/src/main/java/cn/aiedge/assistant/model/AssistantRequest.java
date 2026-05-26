package cn.aiedge.assistant.model;

import lombok.Data;

/**
 * 智能助手请求模型
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class AssistantRequest {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 上下文数据（JSON 格式）
     */
    private String context;

    /**
     * 功能类型：GENERAL(通用问答), SEARCH(搜索), RECOMMENDATION(推荐), ACTION(操作执行)
     */
    private String functionType;

    /**
     * 是否需要流式响应
     */
    private Boolean stream = false;

    /**
     * 最大响应长度
     */
    private Integer maxTokens = 500;

    /**
     * 温度参数（控制生成随机性）
     */
    private Double temperature = 0.7;
}
