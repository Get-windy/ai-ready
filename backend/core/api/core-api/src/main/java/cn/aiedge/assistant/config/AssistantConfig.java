package cn.aiedge.assistant.config;

import org.springframework.context.annotation.Configuration;

/**
 * 智能助手模块配置
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class AssistantConfig {

    public static class AssistantSettings {
        public static final int MAX_CONTEXT_MESSAGES = 10;
        public static final int MAX_RESPONSE_TOKENS = 500;
        public static final double DEFAULT_TEMPERATURE = 0.7;
    }
}
