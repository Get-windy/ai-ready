package cn.aiedge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库实体
 * @author AI-Ready Team
 */
@Data
@TableName("kb_knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /** 知识库名称 */
    private String name;
    
    /** 知识库描述 */
    private String description;
    
    /** 租户ID */
    private Long tenantId;
    
    /** 创建者ID */
    private Long creatorId;
    
    /** 向量维度 */
    private Integer vectorDimension;
    
    /** 状态：active/inactive */
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
