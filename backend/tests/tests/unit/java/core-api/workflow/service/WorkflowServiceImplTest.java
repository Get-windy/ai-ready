package cn.aiedge.workflow.service;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.workflow.model.*;
import cn.aiedge.workflow.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 工作流服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工作流服务测试")
class WorkflowServiceImplTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private WorkflowDefinition testDefinition;
    private Map<String, Object> businessData;

    @BeforeEach
    void setUp() {
        businessData = new HashMap<>();
        businessData.put("amount", 10000);
        businessData.put("reason", "测试");
    }

    // ==================== 流程定义管理 ====================

    @Test
    @DisplayName("获取流程定义 - 内置流程")
    void testGetWorkflowDefinition_Builtin() {
        // When
        WorkflowDefinition result = workflowService.getWorkflowDefinition("order_approval");

        // Then
        assertNotNull(result);
        assertEquals("order_approval", result.getDefinitionId());
    }

    @Test
    @DisplayName("获取流程定义 - 自定义流程")
    void testGetWorkflowDefinition_Custom() {
        // Given
        WorkflowDefinition customDef = createTestDefinition();
        when(cacheService.get(anyString(), eq(WorkflowDefinition.class))).thenReturn(customDef);

        // When
        WorkflowDefinition result = workflowService.getWorkflowDefinition("custom_workflow");

        // Then
        assertNotNull(result);
        assertEquals("custom_workflow", result.getDefinitionId());
    }

    @Test
    @DisplayName("获取流程定义 - 不存在")
    void testGetWorkflowDefinition_NotFound() {
        // Given
        when(cacheService.get(anyString(), eq(WorkflowDefinition.class))).thenReturn(null);

        // When
        WorkflowDefinition result = workflowService.getWorkflowDefinition("non_existent");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("获取流程定义列表 - 全部")
    void testGetWorkflowDefinitions_All() {
        // When
        List<WorkflowDefinition> result = workflowService.getWorkflowDefinitions(null, 1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("获取流程定义列表 - 按类型筛选")
    void testGetWorkflowDefinitions_ByType() {
        // When
        List<WorkflowDefinition> result = workflowService.getWorkflowDefinitions("approval", 1L);

        // Then
        assertNotNull(result);
        // 结果应只包含approval类型的流程
    }

    @Test
    @DisplayName("保存流程定义 - 新建")
    void testSaveWorkflowDefinition_Create() {
        // Given
        WorkflowDefinition definition = createTestDefinition();
        definition.setDefinitionId(null);

        // When
        WorkflowDefinition result = workflowService.saveWorkflowDefinition(definition, 1L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getDefinitionId());
        assertEquals(1, result.getVersion());
        verify(cacheService).set(anyString(), any(WorkflowDefinition.class));
    }

    @Test
    @DisplayName("保存流程定义 - 更新")
    void testSaveWorkflowDefinition_Update() {
        // Given
        WorkflowDefinition definition = createTestDefinition();
        definition.setVersion(1);

        // When
        WorkflowDefinition result = workflowService.saveWorkflowDefinition(definition, 1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getVersion());
        verify(cacheService).set(anyString(), any(WorkflowDefinition.class));
    }

    @Test
    @DisplayName("删除流程定义 - 成功")
    void testDeleteWorkflowDefinition_Success() {
        // When
        boolean result = workflowService.deleteWorkflowDefinition("custom_workflow", 1L);

        // Then
        assertTrue(result);
        verify(cacheService).delete(anyString());
    }

    @Test
    @DisplayName("删除流程定义 - 内置流程不能删除")
    void testDeleteWorkflowDefinition_Builtin() {
        // When
        boolean result = workflowService.deleteWorkflowDefinition("order_approval", 1L);

        // Then
        assertFalse(result);
        verify(cacheService, never()).delete(anyString());
    }

    // ==================== 流程实例管理 ====================

    @Test
    @DisplayName("启动流程 - 成功")
    void testStartWorkflow_Success() {
        // Given
        when(cacheService.set(anyString(), any())).thenReturn(true);

        // When
        WorkflowInstance result = workflowService.startWorkflow(
            "order_approval", "order", "ORDER001", businessData, 1L, 1L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getInstanceId());
        assertEquals("order_approval", result.getDefinitionId());
        assertEquals("approving", result.getStatus());
        assertEquals(1L, result.getApplicantId());
        verify(cacheService).set(anyString(), any(WorkflowInstance.class));
    }

    @Test
    @DisplayName("启动流程 - 流程定义不存在")
    void testStartWorkflow_DefinitionNotFound() {
        // Given
        when(cacheService.get(anyString(), eq(WorkflowDefinition.class))).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            workflowService.startWorkflow("non_existent", "order", "ORDER001", businessData, 1L, 1L);
        });
        assertEquals("流程定义不存在: non_existent", exception.getMessage());
    }

    @Test
    @DisplayName("获取流程实例 - 成功")
    void testGetWorkflowInstance_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);

        // When
        WorkflowInstance result = workflowService.getWorkflowInstance("instance001");

        // Then
        assertNotNull(result);
        assertEquals("instance001", result.getInstanceId());
    }

    @Test
    @DisplayName("获取流程实例 - 不存在")
    void testGetWorkflowInstance_NotFound() {
        // Given
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(null);

        // When
        WorkflowInstance result = workflowService.getWorkflowInstance("non_existent");

        // Then
        assertNull(result);
    }

    // ==================== 审批操作 ====================

    @Test
    @DisplayName("审批通过 - 成功")
    void testApproveWorkflow_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("approving");
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);
        when(cacheService.set(anyString(), any())).thenReturn(true);

        // When
        WorkflowInstance result = workflowService.approveWorkflow("instance001", 2L, "同意", 1L);

        // Then
        assertNotNull(result);
        verify(cacheService).set(anyString(), any(WorkflowInstance.class));
    }

    @Test
    @DisplayName("审批通过 - 实例不存在")
    void testApproveWorkflow_InstanceNotFound() {
        // Given
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, ()        {
            workflowService.approveWorkflow("non_existent", 2L, "同意", 1L);
        });
        assertEquals("流程实例不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审批驳回 - 成功")
    void testRejectWorkflow_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("approving");
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);
        when(cacheService.set(anyString(), any())).thenReturn(true);

        // When
        WorkflowInstance result = workflowService.rejectWorkflow("instance001", 2L, "不符合要求", 1L);

        // Then
        assertNotNull(result);
        assertEquals("rejected", result.getStatus());
    }

    @Test
    @DisplayName("转办 - 成功")
    void testTransferWorkflow_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("approving");
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);
        when(cacheService.set(anyString(), any())).thenReturn(true);

        // When
        WorkflowInstance result = workflowService.transferWorkflow("instance001", 2L, 3L, "请处理", 1L);

        // Then
        assertNotNull(result);
        verify(cacheService).set(anyString(), any(WorkflowInstance.class));
    }

    @Test
    @DisplayName("撤销流程 - 成功")
    void testCancelWorkflow_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("approving");
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);
        when(cacheService.set(anyString(), any())).thenReturn(true);

        // When
        WorkflowInstance result = workflowService.cancelWorkflow("instance001", 1L, "撤销原因", 1L);

        // Then
        assertNotNull(result);
        assertEquals("cancelled", result.getStatus());
    }

    @Test
    @DisplayName("撤销流程 - 非申请人不能撤销")
    void testCancelWorkflow_NotApplicant() {
        // Given
        WorkflowInstance instance = createTestInstance();
        instance.setApplicantId(1L);
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            workflowService.cancelWorkflow("instance001", 2L, "撤销原因", 1L);
        });
        assertEquals("只有申请人可以撤销", exception.getMessage());
    }

    // ==================== 待办查询 ====================

    @Test
    @DisplayName("获取我的待办 - 成功")
    void testGetMyPendingApprovals_Success() {
        // Given
        WorkflowInstance instance1 = createTestInstance();
        WorkflowInstance instance2 = createTestInstance();
        instance2.setInstanceId("instance002");
        
        when(cacheService.lRange(anyString(), anyLong(), anyLong())).thenReturn(
            Arrays.asList("instance001", "instance002")
        );
        when(cacheService.get(contains("instance001"), eq(WorkflowInstance.class))).thenReturn(instance1);
        when(cacheService.get(contains("instance002"), eq(WorkflowInstance.class))).thenReturn(instance2);

        // When
        List<WorkflowInstance> result = workflowService.getMyPendingApprovals(2L, 1, 10, 1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("获取我的已办 - 成功")
    void testGetMyApproved_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("approved");
        
        when(cacheService.lRange(anyString(), anyLong(), anyLong())).thenReturn(
            Collections.singletonList("instance001")
        );
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);

        // When
        List<WorkflowInstance> result = workflowService.getMyApproved(2L, 1, 10, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取我的申请 - 成功")
    void testGetMyApplications_Success() {
        // Given
        WorkflowInstance instance = createTestInstance();
        
        when(cacheService.lRange(anyString(), anyLong(), anyLong())).thenReturn(
            Collections.singletonList("instance001")
        );
        when(cacheService.get(anyString(), eq(WorkflowInstance.class))).thenReturn(instance);

        // When
        List<WorkflowInstance> result = workflowService.getMyApplications(1L, 1, 10, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== 历史记录 ====================

    @Test
    @DisplayName("获取流程历史 - 成功")
    void testGetWorkflowHistory_Success() {
        // Given
        WorkflowRecord record1 = createTestRecord("approving", "提交申请");
        WorkflowRecord record2 = createTestRecord("approved", "审批通过");
        
        when(cacheService.lRange(anyString(), anyLong(), anyLong())).thenReturn(
            Arrays.asList(record1, record2)
        );

        // When
        List<WorkflowRecord> result = workflowService.getWorkflowHistory("instance001", 1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("获取流程历史 - 空记录")
    void testGetWorkflowHistory_Empty() {
        // Given
        when(cacheService.lRange(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptyList());

        // When
        List<WorkflowRecord> result = workflowService.getWorkflowHistory("instance001", 1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Helper Methods ====================

    private WorkflowDefinition createTestDefinition() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setDefinitionId("custom_workflow");
        definition.setName("自定义流程");
        definition.setType("approval");
        definition.setVersion(1);
        definition.setTenantId(1L);
        definition.setCreateTime(LocalDateTime.now());
        definition.setUpdateTime(LocalDateTime.now());
        
        // 创建节点
        WorkflowDefinition.WorkflowNode startNode = new WorkflowDefinition.WorkflowNode();
        startNode.setNodeId("start");
        startNode.setNodeName("开始");
        startNode.setNodeType("start");
        
        WorkflowDefinition.WorkflowNode approvalNode = new WorkflowDefinition.WorkflowNode();
        approvalNode.setNodeId("approval");
        approvalNode.setNodeName("审批");
        approvalNode.setNodeType("approval");
        approvalNode.setApproverType("user");
        approvalNode.setApproverId(2L);
        
        WorkflowDefinition.WorkflowNode endNode = new WorkflowDefinition.WorkflowNode();
        endNode.setNodeId("end");
        endNode.setNodeName("结束");
        endNode.setNodeType("end");
        
        definition.setNodes(Arrays.asList(startNode, approvalNode, endNode));
        return definition;
    }

    private WorkflowInstance createTestInstance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setInstanceId("instance001");
        instance.setDefinitionId("order_approval");
        instance.setWorkflowName("订单审批");
        instance.setBusinessType("order");
        instance.setBusinessId("ORDER001");
        instance.setBusinessData(businessData);
        instance.setApplicantId(1L);
        instance.setApplyTime(LocalDateTime.now());
        instance.setStatus("approving");
        instance.setCurrentNodeId("approval");
        instance.setCurrentNodeName("审批");
        instance.setTenantId(1L);
        return instance;
    }

    private WorkflowRecord createTestRecord(String action, String remark) {
        WorkflowRecord record = new WorkflowRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setInstanceId("instance001");
        record.setNodeId("approval");
        record.setNodeName("审批");
        record.setAction(action);
        record.setOperatorId(2L);
        record.setOperatorName("审批人");
        record.setRemark(remark);
        record.setOperateTime(LocalDateTime.now());
        return record;
    }
}
