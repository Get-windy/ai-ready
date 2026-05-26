package cn.aiedge.knowledge.model;

import lombok.Data;
import java.util.List;

/**
 * 向量检索请求
 */
@Data
public class VectorSearchRequest {
    /** 查询文本 */
    private String query;
    
    /** 知识库ID */
    private Long knowledgeBaseId;
    
    /** 返回数量 */
    private Integer topK = 5;
    
    /** 相似度阈值 */
    private Double scoreThreshold = 0.7;
}
