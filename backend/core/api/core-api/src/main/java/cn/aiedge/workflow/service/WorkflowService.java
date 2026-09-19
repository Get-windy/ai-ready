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

    /**
     * 更新流程定义（含节点，版本+1）
     */
    WorkflowDefinition updateWorkflowDefinition(String definitionId, WorkflowDefinition definition, Long tenantId);

    /**
     * 发布流程定义
     */
    boolean publishWorkflowDefinition(String definitionId, Long tenantId);

    /**
     * 停用流程定义
     */
    boolean disableWorkflowDefinition(String definitionId, Long tenantId);

    /**
     * 统计引用指定定义的流程实例数
     */
    long countDefinitionInstances(String definitionId);

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

    /**
     * 按业务单据取消全部在途（审批中/已挂起）实例——防重场景使用：
     * 同一 businessType+businessId 重新发起审批前调用，避免堆积多条进行中实例。
     * 与 {@link #cancelWorkflow} 不同，本方法不校验申请人（由门面侧系统调用）。
     *
     * @return 实际取消的实例数
     */
    int cancelInFlightByBusiness(String businessType, Long businessId, Long operatorId, String reason);

    /**
     * 查询业务单据的在途实例（approving/suspended，取最新一条）——第三期影子转正式：
     * 单据审批端点据此判断是否由引擎驱动审批。
     *
     * @return 含 instanceId 与 currentTaskId（无待办任务时为 null）的 Map；无在途实例返回 null
     */
    Map<String, Object> findInFlightByBusiness(String businessType, Long businessId);

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

    // ==================== 分页查询 ====================

    /**
     * 分页查询流程实例（供监控页面使用）
     *
     * @param startDate 开始时间下限（YYYY-MM-DD，含当天；为空则不过滤）
     * @param endDate   开始时间上限（YYYY-MM-DD，含当天；为空则不过滤）
     */
    Map<String, Object> pageInstances(int pageNum, int pageSize, String processName, String status,
                                      String startDate, String endDate, Long tenantId);

    /**
     * 流程实例统计（监控页 4 张统计卡的服务端全量聚合，与列表同筛选条件）
     * 返回 total / running / completed / rejected / withdrawn / cancelled / suspended / terminated
     */
    Map<String, Object> statInstances(String processName, String status,
                                      String startDate, String endDate, Long tenantId);

    /**
     * 分页查询任务（待办/已办，供任务管理页面使用）
     */
    Map<String, Object> pageTasks(String tab, Long userId, int pageNum, int pageSize, Long tenantId);

    /**
     * 分页查询任务（带任务名/流程名过滤）
     */
    Map<String, Object> pageTasks(String tab, Long userId, int pageNum, int pageSize, Long tenantId,
                                  String taskName, String processName);

    /**
     * 分页查询任务（全量筛选条件；待办/已办任务管理页）
     *
     * @param priority  优先级（high/medium/low；取自 workflow_task.priority 可空列）
     * @param startDate 创建时间下限（YYYY-MM-DD，含当天；为空则不过滤）
     * @param endDate   创建时间上限（YYYY-MM-DD，含当天；为空则不过滤）
     */
    Map<String, Object> pageTasks(String tab, Long userId, int pageNum, int pageSize, Long tenantId,
                                  String taskName, String processName, String priority,
                                  String startDate, String endDate);

    /**
     * 任务统计（待办/已办页统计卡的**服务端全量真聚合**，与列表同筛选条件）
     * 返回：total / pending / done / transferred / priorityHigh / priorityMedium / priorityLow /
     * priorityUnset / overdue / today / actionApprove / actionReject / actionReturn /
     * actionTransfer / actionSubmit / actionWithdraw / actionCancel / actionIntervene / actionUnset
     */
    Map<String, Object> statTasks(String tab, Long userId, Long tenantId, String taskName,
                                  String processName, String priority, String startDate, String endDate);

    // ==================== 流程监控 ====================

    /**
     * 获取流程图数据
     */
    Map<String, Object> getInstanceDiagram(String instanceId);

    /**
     * 流程干预（终止/挂起/恢复）
     *
     * @param reason 干预理由（必填，落 workflow_task.comment 供审计；业界对管理员的强审计动作要求）
     */
    boolean interveneInstance(String instanceId, String action, Long userId, String reason);

    // ==================== 任务级操作（task-management 页面） ====================

    /**
     * 获取流程实例详情（监控页，含审批记录与业务数据）
     */
    Map<String, Object> getInstanceDetail(String instanceId);

    /**
     * 获取任务详情（含所属流程与业务数据）
     */
    Map<String, Object> getTaskDetail(String taskId);

    /**
     * 按任务审批（action: approve/reject/return）
     * return 分支现已实现**真实的节点回退**（回退至发起人 / 上一节点并重建待办），见 returnTask
     */
    boolean approveTask(String taskId, Long userId, String action, String comment, String returnNode);

    /**
     * 按任务退回（真实节点回退，非终止）
     * 语义：结束当前待办（动作记为「退回」）→ 实例回到目标节点并**保持「审批中」** →
     * 为目标节点重建待办。无法定位回退目标时退化为「驳回」（终止），并把原因写入意见，
     * 保证「界面提示」与「实际结果」一致。
     *
     * @param returnNode start=退回至发起人 / previous=退回至上一节点
     */
    boolean returnTask(String taskId, Long userId, String returnNode, String comment);

    /**
     * 按任务转交（本系统只有「转办」一种语义：任务所有权转移给目标用户；
     * 业界与之并列的「委派（delegate，代处理并回交原处理人）」需要 owner 字段，本表无此列，
     * 故前端不再暴露「委托」，避免给出点了没用的按钮）
     */
    boolean transferTask(String taskId, Long userId, Long targetUserId, String comment);

    // ==================== 流程分析（process-analysis 页面） ====================

    /**
     * 流程分析汇总（状态分布/流程耗时/节点耗时，基于真实数据聚合）
     *
     * @param processName 可选：仅过滤「节点耗时」段（null / 空串 = 全部流程）；
     *                    统计卡与流程耗时段始终为全量口径
     */
    Map<String, Object> getAnalysisSummary(String processName, Long tenantId);

    /**
     * 审批效率报表（按日聚合，日期格式 YYYY-MM-DD，闭区间）
     */
    Map<String, Object> getAnalysisReport(String startDate, String endDate, Long tenantId);
}
