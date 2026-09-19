package cn.aiedge.workflow.service.impl;

import cn.aiedge.workflow.entity.WorkflowDefinitionEntity;
import cn.aiedge.workflow.entity.WorkflowInstanceEntity;
import cn.aiedge.workflow.entity.WorkflowNodeEntity;
import cn.aiedge.workflow.entity.WorkflowTaskEntity;
import cn.aiedge.workflow.model.ApprovalRecord;
import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowDefinition.WorkflowNode;
import cn.aiedge.workflow.model.WorkflowInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 工作流模型↔实体转换器
 * 模型层（HTTP契约，字符串ID/字符串枚举）与实体层（DB，bigint/int枚举）之间的双向映射。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public final class WorkflowConverter {

    private WorkflowConverter() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ==================== 实例状态（0-审批中 1-已通过 2-已驳回 3-已撤回 4-已取消 5-已挂起 6-已终止） ====================

    public static final int INST_APPROVING = 0;
    public static final int INST_APPROVED = 1;
    public static final int INST_REJECTED = 2;
    public static final int INST_WITHDRAWN = 3;
    public static final int INST_CANCELLED = 4;
    public static final int INST_SUSPENDED = 5;
    public static final int INST_TERMINATED = 6;

    public static String instanceStatusToString(Integer status) {
        if (status == null) return "approving";
        return switch (status) {
            case INST_APPROVED -> "approved";
            case INST_REJECTED -> "rejected";
            case INST_WITHDRAWN -> "withdrawn";
            case INST_CANCELLED -> "cancelled";
            case INST_SUSPENDED -> "suspended";
            case INST_TERMINATED -> "terminated";
            default -> "approving";
        };
    }

    public static Integer instanceStatusToInt(String status) {
        if (status == null) return null;
        return switch (status) {
            case "approving", "pending", "running" -> INST_APPROVING;
            case "approved", "completed" -> INST_APPROVED;
            case "rejected" -> INST_REJECTED;
            case "withdrawn" -> INST_WITHDRAWN;
            case "cancelled" -> INST_CANCELLED;
            case "suspended" -> INST_SUSPENDED;
            case "terminated" -> INST_TERMINATED;
            default -> null;
        };
    }

    /**
     * 监控页状态词汇映射（instance-monitor 前端使用 running/completed/terminated/suspended）
     */
    public static String toMonitorStatus(String canonical) {
        if (canonical == null) return null;
        return switch (canonical) {
            case "approving", "pending" -> "running";
            case "approved" -> "completed";
            default -> canonical;
        };
    }

    // ==================== 节点类型（1-开始 2-审批 3-抄送 4-条件 5-结束） ====================

    public static Integer nodeTypeToInt(String nodeType) {
        if (nodeType == null) return 2;
        return switch (nodeType) {
            case "start" -> 1;
            case "cc" -> 3;
            case "condition" -> 4;
            case "end" -> 5;
            default -> 2;
        };
    }

    public static String nodeTypeToString(Integer nodeType) {
        if (nodeType == null) return "approval";
        return switch (nodeType) {
            case 1 -> "start";
            case 3 -> "cc";
            case 4 -> "condition";
            case 5 -> "end";
            default -> "approval";
        };
    }

    // ==================== 审批人类型（1-用户 2-角色 3-部门 4-上级/部门负责人 5-申请人本人） ====================

    public static Integer approverTypeToInt(String approverType) {
        if (approverType == null) return 1;
        return switch (approverType) {
            case "role" -> 2;
            case "dept" -> 3;
            case "leader", "dept_leader" -> 4;
            case "applicant_self" -> 5;
            default -> 1;
        };
    }

    // ==================== 超时处理（1-自动通过 2-自动驳回 3-提醒） ====================

    public static Integer timeoutActionToInt(String timeoutAction) {
        if (timeoutAction == null) return 3;
        return switch (timeoutAction) {
            case "autoApprove" -> 1;
            case "autoReject" -> 2;
            default -> 3;
        };
    }

    // ==================== 流程类型（1-请假 2-报销 3-采购 4-订单/销售 5-财务/费用 6-自定义） ====================

    public static Integer processTypeToInt(String type) {
        if (type == null) return null;
        return switch (type) {
            case "leave" -> 1;
            case "expense" -> 2;
            case "purchase" -> 3;
            case "order" -> 4;
            case "finance", "reimbursement" -> 5;
            case "custom" -> 6;
            default -> null;
        };
    }

    public static String processTypeToString(Integer processType) {
        if (processType == null) return "custom";
        return switch (processType) {
            case 1 -> "leave";
            case 2 -> "expense";
            case 3 -> "purchase";
            case 4 -> "order";
            case 5 -> "finance";
            default -> "custom";
        };
    }

    // ==================== 任务动作（1-同意 2-驳回 3-转交 4-提交 5-撤回 6-取消 7-干预 8-退回） ====================
    //
    // ⚠️ 8-退回 是本轮（2026-09-18）新增的动作值：
    //    此前「退回」被实现为 reject，只把目标节点拼进 comment → 流程被**终止**而非回退，
    //    且已办台账里这些记录的动作全是「驳回」，看不出「退回」意图。
    //    改为真实节点回退（见 WorkflowServiceImpl.returnTask）后，需要一个能如实表达该语义的动作码。

    public static final int ACTION_APPROVE = 1;
    public static final int ACTION_REJECT = 2;
    public static final int ACTION_TRANSFER = 3;
    public static final int ACTION_SUBMIT = 4;
    public static final int ACTION_WITHDRAW = 5;
    public static final int ACTION_CANCEL = 6;
    public static final int ACTION_INTERVENE = 7;
    public static final int ACTION_RETURN = 8;

    public static final int TASK_TYPE_RECORD = 0;
    public static final int TASK_TYPE_APPROVAL = 1;
    public static final int TASK_TYPE_CC = 2;

    public static final int TASK_STATUS_PENDING = 0;
    public static final int TASK_STATUS_DONE = 1;
    public static final int TASK_STATUS_TRANSFERRED = 2;

    public static String taskActionToString(Integer action) {
        if (action == null) return "pending";
        return switch (action) {
            case ACTION_APPROVE -> "approve";
            case ACTION_REJECT -> "reject";
            case ACTION_TRANSFER -> "transfer";
            case ACTION_SUBMIT -> "submit";
            case ACTION_WITHDRAW -> "withdraw";
            case ACTION_CANCEL -> "cancel";
            case ACTION_INTERVENE -> "intervene";
            case ACTION_RETURN -> "return";
            default -> "pending";
        };
    }

    // ==================== 定义转换 ====================

    /**
     * 实体 → 模型。节点列表从 process_config JSON 还原（保真）。
     */
    public static WorkflowDefinition toModel(WorkflowDefinitionEntity entity) {
        WorkflowDefinition model = new WorkflowDefinition();
        model.setDefinitionId(String.valueOf(entity.getId()));
        model.setName(entity.getProcessName());
        model.setCode(entity.getProcessCode());
        model.setType(processTypeToString(entity.getProcessType()));
        model.setDescription(entity.getDescription());
        model.setVersion(entity.getVersion() != null ? entity.getVersion() : 1);
        model.setEnabled(entity.getStatus() != null && entity.getStatus() == 1);
        model.setNodes(parseNodes(entity.getProcessConfig()));
        model.setCreateTime(entity.getCreateTime());
        model.setUpdateTime(entity.getUpdateTime());
        return model;
    }

    /**
     * 模型节点列表 → process_config JSON
     */
    public static String nodesToJson(List<WorkflowNode> nodes) {
        try {
            return MAPPER.writeValueAsString(nodes != null ? nodes : List.of());
        } catch (Exception e) {
            log.warn("[workflow] 节点列表序列化失败: {}", e.getMessage());
            return "[]";
        }
    }

    public static List<WorkflowNode> parseNodes(String processConfig) {
        if (processConfig == null || processConfig.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(processConfig, new TypeReference<List<WorkflowNode>>() {
            });
        } catch (Exception e) {
            log.warn("[workflow] 节点列表反序列化失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 模型节点 → 节点实体（结构化镜像）。
     * 字符串流转关系（nextNodeId/branches）存入 branch_nodes JSON。
     */
    @SuppressWarnings("unchecked")
    public static WorkflowNodeEntity nodeToEntity(Long definitionId, WorkflowNode node, int order, Long tenantId) {
        WorkflowNodeEntity entity = new WorkflowNodeEntity();
        entity.setDefinitionId(definitionId);
        entity.setNodeCode(node.getNodeId());
        entity.setNodeName(node.getNodeName());
        entity.setNodeType(nodeTypeToInt(node.getNodeType()));
        entity.setNodeOrder(order);
        Integer assigneeType = approverTypeToInt(node.getApproverType());
        entity.setAssigneeType(assigneeType);
        String idsJson = toJson(node.getApproverIds());
        if (assigneeType == 2) {
            entity.setRoleIds(idsJson);
        } else {
            entity.setAssigneeIds(idsJson);
        }
        entity.setConditionExpr(node.getConditionExpression());
        Map<String, Object> branch = new HashMap<>();
        branch.put("nextNodeId", node.getNextNodeId());
        branch.put("branches", node.getBranches());
        branch.put("approveMode", node.getApproveMode());
        entity.setBranchNodes(toJson(branch));
        entity.setCanReject(1);
        entity.setCanTransfer(1);
        entity.setTimeLimit(node.getTimeoutHours() > 0 ? node.getTimeoutHours() : null);
        entity.setTimeoutAction(timeoutActionToInt(node.getTimeoutAction()));
        entity.setTenantId(tenantId);
        return entity;
    }

    // ==================== 实例转换 ====================

    /**
     * 实例实体 → 模型
     *
     * @param definitionName 流程名称（可由调用方查表传入，null 则不设置）
     * @param nodeCodeResolver 节点行ID → 节点编码（模型层 nodeId）解析器
     */
    public static WorkflowInstance toModel(WorkflowInstanceEntity entity, String definitionName,
                                    Function<Long, String> nodeCodeResolver) {
        WorkflowInstance model = new WorkflowInstance();
        model.setInstanceId(String.valueOf(entity.getId()));
        model.setDefinitionId(String.valueOf(entity.getDefinitionId()));
        model.setWorkflowName(definitionName != null ? definitionName : entity.getTitle());
        model.setBusinessType(entity.getBusinessType());
        model.setBusinessId(entity.getBusinessId() != null ? String.valueOf(entity.getBusinessId()) : null);
        model.setBusinessData(parseMap(entity.getFormData()));
        if (entity.getCurrentNodeId() != null && nodeCodeResolver != null) {
            model.setCurrentNodeId(nodeCodeResolver.apply(entity.getCurrentNodeId()));
        }
        model.setCurrentNodeName(entity.getCurrentNodeName());
        model.setStatus(instanceStatusToString(entity.getStatus()));
        model.setApplicantId(entity.getApplicantId());
        model.setApplicantName(entity.getApplicantName());
        model.setApplyTime(entity.getCreateTime());
        model.setCompleteTime(entity.getFinishTime());
        model.setTenantId(entity.getTenantId());
        return model;
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[workflow] 业务数据反序列化失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public static String toJson(Object value) {
        if (value == null) return null;
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[workflow] JSON序列化失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 任务 → 审批记录 ====================

    public static ApprovalRecord toRecord(WorkflowTaskEntity task) {
        ApprovalRecord record = new ApprovalRecord();
        record.setRecordId(task.getId());
        record.setInstanceId(String.valueOf(task.getInstanceId()));
        record.setNodeId(task.getNodeId() != null ? String.valueOf(task.getNodeId()) : null);
        record.setNodeName(task.getNodeName());
        record.setApproverId(task.getAssigneeId());
        record.setApproverName(task.getAssigneeName());
        String action = taskActionToString(task.getAction());
        record.setAction(action);
        record.setComment(task.getComment());
        record.setApproveTime(task.getHandleTime() != null ? task.getHandleTime() : task.getCreateTime());
        record.setStatus(switch (action) {
            case "approve" -> "approved";
            case "reject" -> "rejected";
            case "transfer" -> "transferred";
            case "submit" -> "submitted";
            case "withdraw" -> "withdrawn";
            case "cancel" -> "cancelled";
            case "return" -> "returned";
            default -> "pending";
        });
        return record;
    }
}
