package cn.aiedge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识文档实体
 * @author AI-Ready Team
 */
@Data
@TableName("kb_document")
public class KnowledgeDocument {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /** 所属知识库ID */
    private Long knowledgeBaseId;
    
    /** 文档名称 */
    private String name;
    
    /** 文档类型：pdf/docx/txt/md */
    private String fileType;
    
    /** 文件路径 */
    private String filePath;
    
    /** 文件大小（字节） */
    private Long fileSize;
    
    /** 处理状态：pending/processing/completed/failed */
    private String status;
    
    /** 分块数量 */
    private Integer chunkCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
