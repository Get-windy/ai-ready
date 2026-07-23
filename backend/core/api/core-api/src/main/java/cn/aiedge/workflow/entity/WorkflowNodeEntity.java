package cn.aiedge.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作流节点实体（core-api 工作流引擎持久层）
 * 对应表: workflow_node（V5.0.0 列集）
 * 说明: 节点明细的结构化镜像，定义的完整保真数据存于 workflow_definition.process_config；
 * 节点的流转关系（nextNodeId 字符串编码）保存在 branch_nodes JSON 中。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_node")
public class WorkflowNodeEntity {

    /**
     * 节点行ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 流程定义ID
     */
    private Long definitionId;

    /**
     * 节点编码（对应模型层的 nodeId，如 start/manager_approval/end）
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型（1-开始 2-审批 3-抄送 4-条件 5-结束）
     */
    private Integer nodeType;

    /**
     * 节点顺序
     */
    private Integer nodeOrder;

    /**
     * 审批人类型（1-指定用户 2-指定角色 3-部门 4-上级/部门负责人 5-申请人本人）
     */
    private Integer assigneeType;

    /**
     * 审批人ID列表（JSON数组，assigneeType=1时使用）
     */
    private String assigneeIds;

    /**
     * 审批角色ID/编码列表（JSON数组，assigneeType=2时使用）
     */
    private String roleIds;

    /**
     * 条件表达式（nodeType=4时使用）
     */
    private String conditionExpr;

    /**
     * 下一节点行ID（单一流转且可解析时；模型层以字符串 nodeCode 为准，见 branch_nodes）
     */
    private Long nextNodeId;

    /**
     * 分支/流转信息JSON：{"nextNodeId":"end","branches":[...]}
     */
    private String branchNodes;

    /**
     * 是否可驳回（0-否 1-是）
     */
    private Integer canReject;

    /**
     * 是否可转交（0-否 1-是）
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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
