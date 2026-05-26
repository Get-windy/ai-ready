package cn.aiedge.knowledge.model;

import lombok.Data;

/**
 * 向量检索结果项
 */
@Data
public class VectorSearchResult {
    private Long chunkId;
    private Long documentId;
    private String documentName;
    private String content;
    private Double score;
}
