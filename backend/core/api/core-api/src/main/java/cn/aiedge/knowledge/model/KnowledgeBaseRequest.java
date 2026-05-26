package cn.aiedge.knowledge.model;

import lombok.Data;
import java.util.List;

/**
 * 知识库创建请求
 */
@Data
public class KnowledgeBaseRequest {
    private String name;
    private String description;
    private Long tenantId;
    private Integer vectorDimension;
}
