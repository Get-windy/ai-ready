package cn.aiedge.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话记录实体类
 * 存储用户与智能助手的对话历史
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("assistant_conversation")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 会话 ID（用于标识一次完整的对话）
     */
    private String sessionId;

    /**
     * 消息类型：USER(用户消息), ASSISTANT(助手回复)
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息状态：PENDING(处理中), COMPLETED(完成), FAILED(失败)
     */
    private String status;

    /**
     * 上下文数据（JSON 格式，存储对话上下文信息）
     */
    private String contextData;

    /**
     * 功能类型：GENERAL(通用问答), SEARCH(搜索), RECOMMENDATION(推荐), ACTION(操作执行)
     */
    private String functionType;

    /**
     * 执行结果（JSON 格式，存储操作执行结果）
     */
    private String executionResult;

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
