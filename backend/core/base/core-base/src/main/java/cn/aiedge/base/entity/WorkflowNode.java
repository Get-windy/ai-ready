package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流节点实体
 * 流程节点定义（审批节点、抄送节点、条件分支等）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_node")
public class WorkflowNode {

    /**
     * 节点ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 流程定义ID
     */
    private Long definitionId;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型（1-开始节点 2-审批节点 3-抄送节点 4-条件分支 5-结束节点）
     */
    private Integer nodeType;

    /**
     * 节点顺序
     */
    private Integer nodeOrder;

    /**
     * 审批人类型（1-指定人员 2-部门负责人 3-上级领导 4-申请人自选 5-流程发起人）
     */
    private Integer assigneeType;

    /**
     * 审批人ID列表（JSON数组，assigneeType=1时使用）
     */
    private String assigneeIds;

    /**
     * 审批人角色ID列表（JSON数组）
     */
    private String roleIds;

    /**
     * 条件表达式（JSON，nodeType=4时使用）
     */
    private String conditionExpr;

    /**
     * 下一节点ID（单一流转时）
     */
    private Long nextNodeId;

    /**
     * 分支节点列表（JSON数组，条件分支时使用）
     */
    private String branchNodes;

    /**
     * 是否可驳回
     */
    private Integer canReject;

    /**
     * 是否可转交
     */
    private Integer canTransfer;

    /**
     * 审批时限（小时）
     */
    private Integer timeLimit;

    /**
     * 超时处理方式（1-自动通过 2-自动驳回 3-提醒）
     */
    private Integer timeoutAction;

    /**
     * 节点描述
     */
    private String description;

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