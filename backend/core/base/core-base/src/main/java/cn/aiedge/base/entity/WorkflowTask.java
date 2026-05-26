package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流任务实体
 * 审批任务节点
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_task")
public class WorkflowTask {

    /**
     * 任务ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 流程实例ID
     */
    private Long instanceId;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 任务类型（1-审批 2-抄送）
     */
    private Integer taskType;

    /**
     * 审批人ID
     */
    private Long assigneeId;

    /**
     * 审批人名称
     */
    private String assigneeName;

    /**
     * 任务状态（0-待处理 1-已处理 2-已转交）
     */
    private Integer status;

    /**
     * 审批动作（1-同意 2-驳回 3-转交）
     */
    private Integer action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 租户ID
     */
    private Long tenantId;

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
}
