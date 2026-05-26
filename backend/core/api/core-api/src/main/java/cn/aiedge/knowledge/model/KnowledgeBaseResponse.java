package cn.aiedge.knowledge.model;

import lombok.Data;
import java.util.List;

/**
 * 知识库响应
 */
@Data
public class KnowledgeBaseResponse {
    private Long id;
    private String name;
    private String description;
    private Long tenantId;
    private Integer vectorDimension;
    private String status;
    private Integer documentCount;
}
