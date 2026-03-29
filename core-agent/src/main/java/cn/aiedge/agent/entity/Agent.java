package cn.aiedge.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Agent实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("ai_agent")
public class Agent {

    /**
     * Agent ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * Agent编码（唯一标识）
     */
    private String agentCode;

    /**
     * Agent类型（1-对话型 2-工具型 3-工作流型）
     */
    private Integer agentType;

    /**
     * Agent描述
     */
    private String description;

    /**
     * Agent版本
     */
    private String version;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API密钥（加密存储）
     */
    private String apiSecret;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 状态（0-未激活 1-已激活 2-已禁用）
     */
    private Integer status;

    /**
     * 能力列表（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String capabilities;

    /**
     * 配置信息（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String config;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 调用次数
     */
    private Long invokeCount;

    /**
     * 扩展信息
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

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

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}