package cn.aiedge.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流任务实体（core-api 工作流引擎持久层）
 * 对应表: workflow_task（V5.0.0 列集）
 * 说明: 审批任务与审批记录共用本表——task_type=0 为操作记录（提交/撤回/取消/干预），
 * task_type=1 为审批任务，task_type=2 为抄送。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_task")
public class WorkflowTaskEntity {

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
     * 节点行ID（workflow_node.id）
     */
    private Long nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 任务类型（0-操作记录 1-审批 2-抄送）
     */
    private Integer taskType;

    /**
     * 审批人ID（角色/负责人未解析时为 null）
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
     * 审批动作（1-同意 2-驳回 3-转交 4-提交 5-撤回 6-取消 7-干预 8-退回）
     * 8-退回 为本轮新增：退回改为真实节点回退后，需要一个能如实表达该语义的动作码
     * （此前退回被实现为驳回，已办台账看不出「退回」意图）。
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
     * 任务优先级（high/medium/low）
     * 迁移 V11.405.0 新增的可空列；本系统当前**没有**优先级写入来源
     * （流程定义 / 节点 / 发起流程均未提供录入），故一律为 NULL，
     * 接口如实返回 null，前端显示「-」（不允许再硬编码成 "medium"）。
     */
    private String priority;

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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
