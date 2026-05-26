package cn.aiedge.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Agent能力实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("ai_agent_capability")
public class AgentCapability {

    /**
     * 能力ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 能力名称
     */
    private String capabilityName;

    /**
     * 能力编码
     */
    private String capabilityCode;

    /**
     * 能力类型（1-API 2-工具 3-工作流）
     */
    private Integer capabilityType;

    /**
     * 能力描述
     */
    private String description;

    /**
     * 输入参数定义（JSON Schema）
     */
    private String inputSchema;

    /**
     * 输出参数定义（JSON Schema）
     */
    private String outputSchema;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}