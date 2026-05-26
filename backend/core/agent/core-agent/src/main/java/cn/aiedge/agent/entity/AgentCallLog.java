package cn.aiedge.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Agent调用审计日志实体
 * 记录每一次能力调用，用于审计和调试
 */
@Data
@Accessors(chain = true)
@TableName("ai_agent_call_log")
public class AgentCallLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 请求ID（JSON-RPC 的 id）
     */
    private String requestId;

    /**
     * Agent ID（从 API Key 获取）
     */
    private Long agentId;

    /**
     * 能力编码
     */
    private String capabilityCode;

    /**
     * 请求参数（JSON，脱敏）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String requestParams;

    /**
     * 响应结果（JSON，脱敏）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String responseResult;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 调用时间
     */
    private LocalDateTime callTime;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户ID（如果有）
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;
}
