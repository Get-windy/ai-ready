package cn.aiedge.workflow.service;

import cn.aiedge.workflow.model.ApprovalRecord;
import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;

import java.util.List;
import java.util.Map;

/**
 * 审批流程服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface WorkflowService {

    // ==================== 流程定义管理 ====================

    /**
     * 获取流程定义
     */
    WorkflowDefinition getWorkflowDefinition(String definitionId);

    /**
     * 获取流程定义列表
     */
    List<WorkflowDefinition> getWorkflowDefinitions(String type, Long tenantId);

    /**
     * 保存流程定义
     */
    WorkflowDefinition saveWorkflowDefinition(WorkflowDefinition definition, Long tenantId);

    /**
     * 删除流程定义
     */
    boolean deleteWorkflowDefinition(String definitionId, Long tenantId);

    // ==================== 流程实例管理 ====================

    /**
     * 发起流程
     */
    WorkflowInstance startWorkflow(String definitionId, String businessType, 
            String businessId, Map<String, Object> businessData, Long applicantId, Long tenantId);

    /**
     * 获取流程实例
     */
    WorkflowInstance getWorkflowInstance(String instanceId);

    /**
     * 获取我的待办
     */
    List<WorkflowInstance> getMyPendingApprovals(Long userId, int page, int pageSize, Long tenantId);

    /**
     * 获取我的已办
     */
    List<WorkflowInstance> getMyApproved(Long userId, int page, int pageSize, Long tenantId);

    /**
     * 获取我发起的流程
     */
    List<WorkflowInstance> getMyApplications(Long userId, int page, int pageSize, Long tenantId);

    /**
     * 取消流程
     */
    boolean cancelWorkflow(String instanceId, Long userId, String reason);

    // ==================== 审批操作 ====================

    /**
     * 审批通过
     */
    boolean approve(String instanceId, Long userId, String comment);

    /**
     * 审批拒绝
     */
    boolean reject(String instanceId, Long userId, String comment);

    /**
     * 转交他人
     */
    boolean transfer(String instanceId, Long fromUserId, Long toUserId, String comment);

    /**
     * 撤回（申请人撤回）
     */
    boolean withdraw(String instanceId, Long userId, String reason);

    // ==================== 审批记录 ====================

    /**
     * 获取审批记录
     */
    List<ApprovalRecord> getApprovalRecords(String instanceId);

    /**
     * 获取审批历史
     */
    List<ApprovalRecord> getApprovalHistory(String businessType, String businessId);

    // ==================== 统计查询 ====================

    /**
     * 获取待办数量
     */
    int getPendingCount(Long userId, Long tenantId);

    /**
     * 获取流程状态
     */
    Map<String, Object> getWorkflowStatus(String instanceId);
}
