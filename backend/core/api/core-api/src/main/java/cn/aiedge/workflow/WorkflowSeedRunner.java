package cn.aiedge.workflow;

import cn.aiedge.workflow.entity.WorkflowDefinitionEntity;
import cn.aiedge.workflow.entity.WorkflowNodeEntity;
import cn.aiedge.workflow.mapper.WorkflowDefinitionMapper;
import cn.aiedge.workflow.mapper.WorkflowNodeMapper;
import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowDefinition.WorkflowNode;
import cn.aiedge.workflow.service.impl.WorkflowConverter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置流程定义种子数据加载器
 * 启动时将 3 个内置流程写入 workflow_definition / workflow_node，
 * 按 process_code 判重（IF NOT EXISTS 语义），重复启动不产生重复数据。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowSeedRunner implements ApplicationRunner {

    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowNodeMapper nodeMapper;

    @Override
    public void run(ApplicationArguments args) {
        int seeded = 0;
        for (WorkflowDefinition definition : builtinWorkflows()) {
            Long count = definitionMapper.selectCount(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                    .eq(WorkflowDefinitionEntity::getProcessCode, definition.getCode()));
            if (count != null && count > 0) {
                continue;
            }
            WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
            entity.setProcessCode(definition.getCode());
            entity.setProcessName(definition.getName());
            entity.setProcessType(WorkflowConverter.processTypeToInt(definition.getType()));
            entity.setDescription(definition.getDescription());
            entity.setProcessConfig(WorkflowConverter.nodesToJson(definition.getNodes()));
            entity.setVersion(1);
            entity.setIsDefault(1);
            entity.setStatus(1);
            entity.setTenantId(0L);
            definitionMapper.insert(entity);

            int order = 0;
            for (WorkflowNode node : definition.getNodes()) {
                nodeMapper.insert(WorkflowConverter.nodeToEntity(entity.getId(), node, order++, 0L));
            }
            seeded++;
            log.info("[workflow-seed] 内置流程已写入: {} ({})", definition.getCode(), definition.getName());
        }
        if (seeded > 0) {
            log.info("[workflow-seed] 共写入 {} 个内置流程定义", seeded);
        }
    }

    private static List<WorkflowDefinition> builtinWorkflows() {
        List<WorkflowDefinition> list = new ArrayList<>();
        list.add(createOrderApprovalWorkflow());
        list.add(createLeaveApprovalWorkflow());
        list.add(createExpenseApprovalWorkflow());
        return list;
    }

    // ==================== 内置流程定义 ====================

    private static WorkflowDefinition createOrderApprovalWorkflow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setName("订单审批流程");
        def.setCode("order_approval");
        def.setType("order");
        def.setDescription("销售订单提交后由部门经理审批");
        def.setVersion(1);
        def.setEnabled(true);

        List<WorkflowNode> nodes = new ArrayList<>();

        WorkflowNode start = new WorkflowNode();
        start.setNodeId("start");
        start.setNodeName("开始");
        start.setNodeType("start");
        start.setNextNodeId("manager_approval");
        nodes.add(start);

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
        def.setName("请假审批流程");
        def.setCode("leave_approval");
        def.setType("leave");
        def.setDescription("员工请假申请由部门主管审批");
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
        def.setName("报销审批流程");
        def.setCode("expense_approval");
        def.setType("expense");
        def.setDescription("费用报销提交后由财务审批");
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
