package cn.aiedge.workflow.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.workflow.model.*;
import cn.aiedge.workflow.model.WorkflowDefinition.WorkflowNode;
import cn.aiedge.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 审批流程服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final CacheService cacheService;

    // 缓存Key前缀
    private static final String DEF_KEY = "workflow:definition:";
    private static final String INSTANCE_KEY = "workflow:instance:";
    private static final String RECORD_KEY = "workflow:record:";
    private static final String PENDING_KEY = "workflow:pending:";
    private static final String APPROVED_KEY = "workflow:approved:";
    private static final String MY_APP_KEY = "workflow:myapp:";

    // 内置流程定义
    private static final Map<String, WorkflowDefinition> BUILTIN_WORKFLOWS = new LinkedHashMap<>();

    static {
        // 订单审批流程
        BUILTIN_WORKFLOWS.put("order_approval", createOrderApprovalWorkflow());
        // 请假审批流程
        BUILTIN_WORKFLOWS.put("leave_approval", createLeaveApprovalWorkflow());
        // 报销审批流程
        BUILTIN_WORKFLOWS.put("expense_approval", createExpenseApprovalWorkflow());
    }

    // ==================== 流程定义管理 ====================

    @Override
    public WorkflowDefinition getWorkflowDefinition(String definitionId) {
        WorkflowDefinition def = BUILTIN_WORKFLOWS.get(definitionId);
        if (def != null) return def;
        return cacheService.get(DEF_KEY + definitionId, WorkflowDefinition.class);
    }

    @Override
    public List<WorkflowDefinition> getWorkflowDefinitions(String type, Long tenantId) {
        List<WorkflowDefinition> result = new ArrayList<>();
        for (WorkflowDefinition def : BUILTIN_WORKFLOWS.values()) {
            if (type == null || type.equals(def.getType())) {
                result.add(def);
            }
        }
        return result;
    }

    @Override
    public WorkflowDefinition saveWorkflowDefinition(WorkflowDefinition definition, Long tenantId) {
        if (definition.getDefinitionId() == null) {
            definition.setDefinitionId(UUID.randomUUID().toString());
        }
        definition.setVersion(definition.getVersion() + 1);
        definition.setUpdateTime(LocalDateTime.now());
        
        cacheService.set(DEF_KEY + definition.getDefinitionId(), definition);
        log.info("保存流程定义: {}", definition.getDefinitionId());
        return definition;
    }

    @Override
    public boolean deleteWorkflowDefinition(String definitionId, Long tenantId) {
        if (BUILTIN_WORKFLOWS.containsKey(definitionId)) {
            log.warn("不能删除内置流程定义: {}", definitionId);
            return false;
        }
        cacheService.delete(DEF_KEY + definitionId);
        return true;
    }

    // ==================== 流程实例管理 ====================

    @Override
    public WorkflowInstance startWorkflow(String definitionId, String businessType, 
            String businessId, Map<String, Object> businessData, Long applicantId, Long tenantId) {
        
        WorkflowDefinition definition = getWorkflowDefinition(definitionId);
        if (definition == null) {
            throw new RuntimeException("流程定义不存在: " + definitionId);
        }

        // 创建流程实例
        WorkflowInstance instance = new WorkflowInstance();
        instance.setInstanceId(UUID.randomUUID().toString());
        instance.setDefinitionId(definitionId);
        instance.setWorkflowName(definition.getName());
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setBusinessData(businessData);
        instance.setApplicantId(applicantId);
        instance.setApplyTime(LocalDateTime.now());
        instance.setStatus("approving");
        instance.setTenantId(tenantId);

        // 设置当前节点（第一个审批节点）
        WorkflowNode firstNode = findFirstApprovalNode(definition);
        if (firstNode != null) {
            instance.setCurrentNodeId(firstNode.getNodeId());
            instance.setCurrentNodeName(firstNode.getNodeName());
        }

        // 保存实例
        cacheService.set(INSTANCE_KEY + instance.getInstanceId(), instance);

        // 添加到待办列表
        addToPendingList(instance, firstNode);

        // 记录初始审批记录
        createInitialRecord(instance);

        log.info("发起流程: instanceId={}, definitionId={}", instance.getInstanceId(), definitionId);
        return instance;
    }

    @Override
    public WorkflowInstance getWorkflowInstance(String instanceId) {
        return cacheService.get(INSTANCE_KEY + instanceId, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> getMyPendingApprovals(Long userId, int page, int pageSize, Long tenantId) {
        String key = PENDING_KEY + userId;
        List<Object> ids = cacheService.lRange(key, (page - 1) * pageSize, page * pageSize - 1);
        return convertToInstances(ids);
    }

    @Override
    public List<WorkflowInstance> getMyApproved(Long userId, int page, int pageSize, Long tenantId) {
        String key = APPROVED_KEY + userId;
        List<Object> ids = cacheService.lRange(key, (page - 1) * pageSize, page * pageSize - 1);
        return convertToInstances(ids);
    }

    @Override
    public List<WorkflowInstance> getMyApplications(Long userId, int page, int pageSize, Long tenantId) {
        String key = MY_APP_KEY + userId;
        List<Object> ids = cacheService.lRange(key, (page - 1) * pageSize, page * pageSize - 1);
        return convertToInstances(ids);
    }

    @Override
    public boolean cancelWorkflow(String instanceId, Long userId, String reason) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null || !instance.getApplicantId().equals(userId)) {
            return false;
        }
        if (!"approving".equals(instance.getStatus())) {
            return false;
        }

        instance.setStatus("cancelled");
        instance.setCompleteTime(LocalDateTime.now());
        cacheService.set(INSTANCE_KEY + instanceId, instance);

        // 添加取消记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(instance.getCurrentNodeId());
        record.setNodeName(instance.getCurrentNodeName());
        record.setApproverId(userId);
        record.setAction("cancel");
        record.setComment(reason);
        record.setApproveTime(LocalDateTime.now());
        record.setStatus("cancelled");
        addApprovalRecord(instanceId, record);

        log.info("取消流程: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    // ==================== 审批操作 ====================

    @Override
    public boolean approve(String instanceId, Long userId, String comment) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null || !"approving".equals(instance.getStatus())) {
            return false;
        }

        // 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(instance.getCurrentNodeId());
        record.setNodeName(instance.getCurrentNodeName());
        record.setApproverId(userId);
        record.setAction("approve");
        record.setComment(comment);
        record.setApproveTime(LocalDateTime.now());
        record.setStatus("approved");
        addApprovalRecord(instanceId, record);

        // 移动到下一个节点或完成流程
        WorkflowDefinition definition = getWorkflowDefinition(instance.getDefinitionId());
        WorkflowNode nextNode = findNextNode(definition, instance.getCurrentNodeId());

        if (nextNode == null || "end".equals(nextNode.getNodeType())) {
            // 流程结束
            instance.setStatus("approved");
            instance.setCompleteTime(LocalDateTime.now());
        } else {
            // 移动到下一个节点
            instance.setCurrentNodeId(nextNode.getNodeId());
            instance.setCurrentNodeName(nextNode.getNodeName());
        }

        cacheService.set(INSTANCE_KEY + instanceId, instance);

        // 从待办移到已办
        moveFromPendingToApproved(userId, instanceId);

        log.info("审批通过: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    @Override
    public boolean reject(String instanceId, Long userId, String comment) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null || !"approving".equals(instance.getStatus())) {
            return false;
        }

        // 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(instance.getCurrentNodeId());
        record.setNodeName(instance.getCurrentNodeName());
        record.setApproverId(userId);
        record.setAction("reject");
        record.setComment(comment);
        record.setApproveTime(LocalDateTime.now());
        record.setStatus("rejected");
        addApprovalRecord(instanceId, record);

        // 更新流程状态
        instance.setStatus("rejected");
        instance.setCompleteTime(LocalDateTime.now());
        cacheService.set(INSTANCE_KEY + instanceId, instance);

        // 从待办移到已办
        moveFromPendingToApproved(userId, instanceId);

        log.info("审批拒绝: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    @Override
    public boolean transfer(String instanceId, Long fromUserId, Long toUserId, String comment) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) return false;

        // 创建转交记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(instance.getCurrentNodeId());
        record.setNodeName(instance.getCurrentNodeName());
        record.setApproverId(fromUserId);
        record.setAction("transfer");
        record.setComment("转交给用户" + toUserId + ": " + comment);
        record.setApproveTime(LocalDateTime.now());
        record.setStatus("transferred");
        addApprovalRecord(instanceId, record);

        // 从待办移到已办
        moveFromPendingToApproved(fromUserId, instanceId);
        // 添加到新审批人的待办
        cacheService.lPush(PENDING_KEY + toUserId, instanceId);

        log.info("转交审批: instanceId={}, from={}, to={}", instanceId, fromUserId, toUserId);
        return true;
    }

    @Override
    public boolean withdraw(String instanceId, Long userId, String reason) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null || !instance.getApplicantId().equals(userId)) {
            return false;
        }
        if (!"approving".equals(instance.getStatus())) {
            return false;
        }

        instance.setStatus("withdrawn");
        instance.setCompleteTime(LocalDateTime.now());
        cacheService.set(INSTANCE_KEY + instanceId, instance);

        // 创建撤回记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setApproverId(userId);
        record.setAction("withdraw");
        record.setComment(reason);
        record.setApproveTime(LocalDateTime.now());
        record.setStatus("withdrawn");
        addApprovalRecord(instanceId, record);

        log.info("撤回流程: instanceId={}, userId={}", instanceId, userId);
        return true;
    }

    // ==================== 审批记录 ====================

    @Override
    public List<ApprovalRecord> getApprovalRecords(String instanceId) {
        String key = RECORD_KEY + instanceId;
        List<Object> records = cacheService.lRange(key, 0, -1);
        List<ApprovalRecord> result = new ArrayList<>();
        for (Object obj : records) {
            if (obj instanceof ApprovalRecord) {
                result.add((ApprovalRecord) obj);
            }
        }
        return result;
    }

    @Override
    public List<ApprovalRecord> getApprovalHistory(String businessType, String businessId) {
        // 根据业务类型和ID查找实例
        // 简化实现：返回空列表
        return new ArrayList<>();
    }

    // ==================== 统计查询 ====================

    @Override
    public int getPendingCount(Long userId, Long tenantId) {
        String key = PENDING_KEY + userId;
        Long size = cacheService.lSize(key);
        return size != null ? size.intValue() : 0;
    }

    @Override
    public Map<String, Object> getWorkflowStatus(String instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            return Map.of("exists", false);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("exists", true);
        status.put("instanceId", instanceId);
        status.put("status", instance.getStatus());
        status.put("currentNode", instance.getCurrentNodeName());
        status.put("applicantId", instance.getApplicantId());
        status.put("applyTime", instance.getApplyTime());
        status.put("completeTime", instance.getCompleteTime());

        // 获取审批记录
        List<ApprovalRecord> records = getApprovalRecords(instanceId);
        status.put("approvalCount", records.size());
        status.put("records", records);

        return status;
    }

    // ==================== 私有方法 ====================

    private void addApprovalRecord(String instanceId, ApprovalRecord record) {
        String key = RECORD_KEY + instanceId;
        record.setRecordId(System.currentTimeMillis());
        cacheService.lPush(key, record);
        cacheService.expire(key, 365, TimeUnit.DAYS);
    }

    private void addToPendingList(WorkflowInstance instance, WorkflowNode node) {
        if (node != null && node.getApproverIds() != null) {
            for (String approverId : node.getApproverIds()) {
                cacheService.lPush(PENDING_KEY + approverId, instance.getInstanceId());
            }
        }
        // 添加到我发起的列表
        cacheService.lPush(MY_APP_KEY + instance.getApplicantId(), instance.getInstanceId());
    }

    private void moveFromPendingToApproved(Long userId, String instanceId) {
        cacheService.lPush(APPROVED_KEY + userId, instanceId);
        // 从待办列表移除（简化实现，实际需要精确移除）
    }

    private void createInitialRecord(WorkflowInstance instance) {
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instance.getInstanceId());
        record.setNodeId("start");
        record.setNodeName("发起申请");
        record.setApproverId(instance.getApplicantId());
        record.setAction("submit");
        record.setComment("提交审批申请");
        record.setApproveTime(instance.getApplyTime());
        record.setStatus("submitted");
        addApprovalRecord(instance.getInstanceId(), record);
    }

    private WorkflowNode findFirstApprovalNode(WorkflowDefinition definition) {
        for (WorkflowNode node : definition.getNodes()) {
            if ("approval".equals(node.getNodeType())) {
                return node;
            }
        }
        return null;
    }

    private WorkflowNode findNextNode(WorkflowDefinition definition, String currentNodeId) {
        for (WorkflowNode node : definition.getNodes()) {
            if (node.getNodeId().equals(currentNodeId)) {
                String nextId = node.getNextNodeId();
                if (nextId != null) {
                    for (WorkflowNode next : definition.getNodes()) {
                        if (next.getNodeId().equals(nextId)) {
                            return next;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<WorkflowInstance> convertToInstances(List<Object> ids) {
        List<WorkflowInstance> result = new ArrayList<>();
        if (ids == null) return result;
        for (Object id : ids) {
            WorkflowInstance instance = getWorkflowInstance(id.toString());
            if (instance != null) {
                result.add(instance);
            }
        }
        return result;
    }

    // ==================== 分页查询 ====================

    @Override
    public Map<String, Object> pageInstances(int pageNum, int pageSize, String processName, String status, Long tenantId) {
        List<WorkflowInstance> allInstances = new ArrayList<>();
        try {
            // 尝试从缓存中扫描所有流程实例
            Set<String> keys = cacheService.keys(INSTANCE_KEY + "*");
            if (keys != null) {
                for (String key : keys) {
                    WorkflowInstance instance = cacheService.get(key, WorkflowInstance.class);
                    if (instance != null) {
                        allInstances.add(instance);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[workflow] 扫描流程实例缓存失败: {}", e.getMessage());
        }

        // 过滤
        if (processName != null && !processName.isEmpty()) {
            allInstances.removeIf(i -> i.getWorkflowName() == null || !i.getWorkflowName().contains(processName));
        }
        if (status != null && !status.isEmpty()) {
            allInstances.removeIf(i -> !status.equals(i.getStatus()));
        }
        if (tenantId != null) {
            allInstances.removeIf(i -> !tenantId.equals(i.getTenantId()));
        }

        // 按申请时间降序排序
        allInstances.sort((a, b) -> {
            if (a.getApplyTime() == null || b.getApplyTime() == null) return 0;
            return b.getApplyTime().compareTo(a.getApplyTime());
        });

        int total = allInstances.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<WorkflowInstance> records = (fromIndex >= total) ? List.of() : allInstances.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    // ==================== 流程监控 ====================

    @Override
    public Map<String, Object> getInstanceDiagram(String instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("svg", "");
            empty.put("imageUrl", "");
            return empty;
        }

        WorkflowDefinition definition = getWorkflowDefinition(instance.getDefinitionId());
        if (definition == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("svg", "");
            empty.put("imageUrl", "");
            return empty;
        }

        // 生成简单 SVG 流程图
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 ")
                .append(definition.getNodes().size() * 80 + 40).append("\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#fafafa\"/>");
        svg.append("<text x=\"200\" y=\"24\" text-anchor=\"middle\" font-size=\"14\" font-weight=\"bold\">")
                .append(escapeXml(definition.getName())).append("</text>");

        int y = 50;
        for (WorkflowNode node : definition.getNodes()) {
            boolean isCurrent = node.getNodeId().equals(instance.getCurrentNodeId());
            String fillColor = isCurrent ? "#1890ff" : "#ffffff";
            String strokeColor = isCurrent ? "#1890ff" : "#d9d9d9";
            String textColor = isCurrent ? "#ffffff" : "#333333";

            if ("start".equals(node.getNodeType()) || "end".equals(node.getNodeType())) {
                // 椭圆形
                svg.append("<ellipse cx=\"200\" cy=\"").append(y + 20)
                        .append("\" rx=\"60\" ry=\"20\" fill=\"").append(fillColor)
                        .append("\" stroke=\"").append(strokeColor).append("\" stroke-width=\"2\"/>");
            } else {
                // 矩形
                svg.append("<rect x=\"120\" y=\"").append(y)
                        .append("\" width=\"160\" height=\"40\" rx=\"4\" fill=\"").append(fillColor)
                        .append("\" stroke=\"").append(strokeColor).append("\" stroke-width=\"2\"/>");
            }
            svg.append("<text x=\"200\" y=\"").append(y + (isCurrent ? 26 : 24))
                    .append("\" text-anchor=\"middle\" font-size=\"12\" fill=\"").append(textColor).append("\">")
                    .append(escapeXml(node.getNodeName())).append("</text>");

            y += 80;

            // 箭头
            if (node.getNextNodeId() != null && !node.getNextNodeId().isEmpty()) {
                int arrowY = y - 60;
                svg.append("<line x1=\"200\" y1=\"").append(arrowY)
                        .append("\" x2=\"200\" y2=\"").append(arrowY + 20)
                        .append("\" stroke=\"#999\" stroke-width=\"1.5\" marker-end=\"url(#arrow)\"/>");
            }
        }

        svg.append("<defs><marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"10\" refY=\"5\"")
                .append(" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto\">")
                .append("<path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#999\"/></marker></defs>");
        svg.append("</svg>");

        Map<String, Object> result = new HashMap<>();
        result.put("svg", svg.toString());
        result.put("imageUrl", "");
        return result;
    }

    @Override
    public boolean interveneInstance(String instanceId, String action, Long userId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            log.warn("[workflow] 干预失败，实例不存在: {}", instanceId);
            return false;
        }

        log.info("[workflow] 流程干预: instanceId={}, action={}, userId={}", instanceId, action, userId);

        switch (action) {
            case "terminate":
                if (!"approving".equals(instance.getStatus()) && !"suspended".equals(instance.getStatus())) {
                    log.warn("[workflow] 终止失败，当前状态不允许终止: {}", instance.getStatus());
                    return false;
                }
                instance.setStatus("terminated");
                instance.setCompleteTime(LocalDateTime.now());
                break;

            case "suspend":
                if (!"approving".equals(instance.getStatus())) {
                    log.warn("[workflow] 挂起失败，当前状态不允许挂起: {}", instance.getStatus());
                    return false;
                }
                instance.setStatus("suspended");
                break;

            case "resume":
                if (!"suspended".equals(instance.getStatus())) {
                    log.warn("[workflow] 恢复失败，当前状态不允许恢复: {}", instance.getStatus());
                    return false;
                }
                instance.setStatus("approving");
                break;

            default:
                log.warn("[workflow] 未知干预动作: {}", action);
                return false;
        }

        cacheService.set(INSTANCE_KEY + instanceId, instance);

        // 记录干预操作
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(instance.getCurrentNodeId());
        record.setNodeName(instance.getCurrentNodeName());
        record.setApproverId(userId);
        record.setAction(action);
        record.setComment("流程干预: " + action);
        record.setApproveTime(LocalDateTime.now());
        record.setStatus(action);
        addApprovalRecord(instanceId, record);

        log.info("[workflow] 流程干预成功: instanceId={}, action={}", instanceId, action);
        return true;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    @Override
    public Map<String, Object> pageTasks(String tab, Long userId, int pageNum, int pageSize, Long tenantId) {
        List<WorkflowInstance> records;
        int total;

        if ("done".equals(tab)) {
            records = getMyApproved(userId, pageNum, pageSize, tenantId);
            total = records.size(); // 简化：无总计数
        } else {
            // default: todo
            records = getMyPendingApprovals(userId, pageNum, pageSize, tenantId);
            total = getPendingCount(userId, tenantId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    // ==================== 内置流程定义 ====================

    private static WorkflowDefinition createOrderApprovalWorkflow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setDefinitionId("order_approval");
        def.setName("订单审批流程");
        def.setCode("ORDER_APPROVAL");
        def.setType("order");
        def.setVersion(1);
        def.setEnabled(true);

        List<WorkflowNode> nodes = new ArrayList<>();
        
        // 开始节点
        WorkflowNode start = new WorkflowNode();
        start.setNodeId("start");
        start.setNodeName("开始");
        start.setNodeType("start");
        start.setNextNodeId("manager_approval");
        nodes.add(start);

        // 经理审批
        WorkflowNode managerApproval = new WorkflowNode();
        managerApproval.setNodeId("manager_approval");
        managerApproval.setNodeName("部门经理审批");
        managerApproval.setNodeType("approval");
        managerApproval.setApproverType("role");
        managerApproval.setApproverIds(List.of("manager"));
        managerApproval.setApproveMode("any");
        managerApproval.setTimeoutHours(24);
        managerApproval.setNextNodeId("end");
        nodes.add(managerApproval);

        // 结束节点
        WorkflowNode end = new WorkflowNode();
        end.setNodeId("end");
        end.setNodeName("结束");
        end.setNodeType("end");
        nodes.add(end);

        def.setNodes(nodes);
        return def;
    }

    private static WorkflowDefinition createLeaveApprovalWorkflow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setDefinitionId("leave_approval");
        def.setName("请假审批流程");
        def.setCode("LEAVE_APPROVAL");
        def.setType("leave");
        def.setVersion(1);
        def.setEnabled(true);

        List<WorkflowNode> nodes = new ArrayList<>();
        
        WorkflowNode start = new WorkflowNode();
        start.setNodeId("start");
        start.setNodeName("开始");
        start.setNodeType("start");
        start.setNextNodeId("dept_approval");
        nodes.add(start);

        WorkflowNode deptApproval = new WorkflowNode();
        deptApproval.setNodeId("dept_approval");
        deptApproval.setNodeName("部门主管审批");
        deptApproval.setNodeType("approval");
        deptApproval.setApproverType("leader");
        deptApproval.setApproveMode("any");
        deptApproval.setTimeoutHours(8);
        deptApproval.setNextNodeId("end");
        nodes.add(deptApproval);

        WorkflowNode end = new WorkflowNode();
        end.setNodeId("end");
        end.setNodeName("结束");
        end.setNodeType("end");
        nodes.add(end);

        def.setNodes(nodes);
        return def;
    }

    private static WorkflowDefinition createExpenseApprovalWorkflow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setDefinitionId("expense_approval");
        def.setName("报销审批流程");
        def.setCode("EXPENSE_APPROVAL");
        def.setType("expense");
        def.setVersion(1);
        def.setEnabled(true);

        List<WorkflowNode> nodes = new ArrayList<>();
        
        WorkflowNode start = new WorkflowNode();
        start.setNodeId("start");
        start.setNodeName("开始");
        start.setNodeType("start");
        start.setNextNodeId("finance_approval");
        nodes.add(start);

        WorkflowNode financeApproval = new WorkflowNode();
        financeApproval.setNodeId("finance_approval");
        financeApproval.setNodeName("财务审批");
        financeApproval.setNodeType("approval");
        financeApproval.setApproverType("role");
        financeApproval.setApproverIds(List.of("finance"));
        financeApproval.setApproveMode("any");
        financeApproval.setTimeoutHours(48);
        financeApproval.setNextNodeId("end");
        nodes.add(financeApproval);

        WorkflowNode end = new WorkflowNode();
        end.setNodeId("end");
        end.setNodeName("结束");
        end.setNodeType("end");
        nodes.add(end);

        def.setNodes(nodes);
        return def;
    }
}
