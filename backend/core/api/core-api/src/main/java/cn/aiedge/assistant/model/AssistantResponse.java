package cn.aiedge.assistant.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 智能助手响应模型
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssistantResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 功能类型
     */
    private String functionType;

    /**
     * 执行状态：SUCCESS(成功), ERROR(错误), PENDING(处理中)
     */
    private String status;

    /**
     * 执行结果（JSON 格式）
     */
    private Object result;

    /**
     * 建议操作列表
     */
    private SuggestedAction[] suggestedActions;

    /**
     * 是否需要用户确认
     */
    private Boolean requiresConfirmation = false;

    /**
     * 建议操作
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuggestedAction {
        /**
         * 操作类型
         */
        private String actionType;

        /**
         * 操作描述
         */
        private String description;

        /**
         * 操作参数（JSON 格式）
         */
        private Object parameters;

        /**
         * 显示文本
         */
        private String displayText;
    }
}
