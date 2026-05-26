package cn.aiedge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档分块实体
 * @author AI-Ready Team
 */
@Data
@TableName("kb_document_chunk")
public class DocumentChunk {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /** 文档ID */
    private Long documentId;
    
    /** 知识库ID */
    private Long knowledgeBaseId;
    
    /** 分块序号 */
    private Integer chunkIndex;
    
    /** 分块内容 */
    private String content;
    
    /** 向量ID（向量数据库中的ID） */
    private String vectorId;
    
    /** Token数量 */
    private Integer tokenCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
