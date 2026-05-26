package cn.aiedge.knowledge.config;

import org.springframework.context.annotation.Configuration;

/**
 * 知识库配置类
 */
@Configuration
public class KnowledgeConfig {
    
    // 向量维度配置（默认OpenAI text-embedding-ada-002）
    public static final int DEFAULT_VECTOR_DIMENSION = 1536;
    
    // 分块大小
    public static final int DEFAULT_CHUNK_SIZE = 1000;
    
    // 分块重叠
    public static final int DEFAULT_CHUNK_OVERLAP = 200;
    
    // 默认TopK
    public static final int DEFAULT_TOP_K = 5;
    
    // 默认相似度阈值
    public static final double DEFAULT_SCORE_THRESHOLD = 0.7;
}
