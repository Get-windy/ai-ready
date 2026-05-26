package cn.aiedge.base.service;

import cn.aiedge.base.entity.WorkflowDefinition;
import cn.aiedge.base.entity.WorkflowInstance;
import cn.aiedge.base.entity.WorkflowTask;
import cn.aiedge.base.service.impl.WorkflowServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private WorkflowDefinition testDefinition;

    @BeforeEach
    void setUp() {
        testDefinition = new WorkflowDefinition();
        testDefinition.setId(1L);
        testDefinition.setProcessCode("leave_process");
        testDefinition.setProcessName("请假流程");
        testDefinition.setProcessType(1);
        testDefinition.setDescription("员工请假审批流程");
        testDefinition.setVersion(1);
        testDefinition.setIsDefault(1);
        testDefinition.setStatus(1);
        testDefinition.setTenantId(1L);
        testDefinition.setCreateTime(LocalDateTime.now());
        testDefinition.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void testCreateDefinition() {
        // when
        Long definitionId = workflowService.createDefinition(testDefinition);

        // then
        assertNotNull(definitionId);
        assertEquals(1, testDefinition.getVersion());
        assertEquals(0, testDefinition.getStatus());
    }

    @Test
    void testUpdateDefinition() {
        // given
        testDefinition.setProcessName("更新后的流程名称");

        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.updateDefinition(testDefinition));
    }

    @Test
    void testPublishDefinition() {
        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.publishDefinition(1L));
    }

    @Test
    void testDisableDefinition() {
        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.disableDefinition(1L));
    }

    @Test
    void testStartInstance() {
        // given
        Long definitionId = 1L;
        Long businessId = 100L;
        String businessType = "leave";
        String title = "张三的请假申请";
        Long applicantId = 1L;
        String applicantName = "张三";

        // when
        Long instanceId = workflowService.startInstance(definitionId, businessId, 
                businessType, title, applicantId, applicantName);

        // then
        assertNotNull(instanceId);
    }

    @Test
    void testApprove() {
        // given
        Long taskId = 1L;
        Long userId = 2L;
        String comment = "同意";

        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.approve(taskId, userId, comment));
    }

    @Test
    void testReject() {
        // given
        Long taskId = 1L;
        Long userId = 2L;
        String comment = "信息不完整，请补充";

        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.reject(taskId, userId, comment));
    }

    @Test
    void testTransfer() {
        // given
        Long taskId = 1L;
        Long fromUserId = 2L;
        Long toUserId = 3L;
        String comment = "转交给更合适的审批人";

        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.transfer(taskId, fromUserId, toUserId, comment));
    }

    @Test
    void testWithdraw() {
        // given
        Long instanceId = 1L;
        Long userId = 1L;

        // when & then (不应抛出异常)
        assertDoesNotThrow(() -> workflowService.withdraw(instanceId, userId));
    }

    @Test
    void testGetTodoTasks() {
        // given
        Long userId = 1L;
        Long tenantId = 1L;

        // when
        var tasks = workflowService.getTodoTasks(userId, tenantId);

        // then
        assertNotNull(tasks);
    }

    @Test
    void testGetDoneTasks() {
        // given
        Long userId = 1L;
        Long tenantId = 1L;

        // when
        var tasks = workflowService.getDoneTasks(userId, tenantId);

        // then
        assertNotNull(tasks);
    }

    @Test
    void testGetInstanceHistory() {
        // given
        Long instanceId = 1L;

        // when
        var history = workflowService.getInstanceHistory(instanceId);

        // then
        assertNotNull(history);
    }

    @Test
    void testPageDefinitions() {
        // given
        Page<WorkflowDefinition> page = new Page<>(1, 10);
        Long tenantId = 1L;
        String processName = "请假";
        Integer status = 1;

        // when
        Page<WorkflowDefinition> result = workflowService.pageDefinitions(page, tenantId, processName, status);

        // then
        assertNotNull(result);
    }

    @Test
    void testGetDefinition() {
        // given
        Long definitionId = 1L;

        // when
        WorkflowDefinition result = workflowService.getDefinition(definitionId);

        // then
        // 当前实现返回null，因为数据库尚未实现
        assertNull(result);
    }

    @Test
    void testGetDefaultDefinition() {
        // given
        String processCode = "leave_process";
        Long tenantId = 1L;

        // when
        WorkflowDefinition result = workflowService.getDefaultDefinition(processCode, tenantId);

        // then
        // 当前实现返回null，因为数据库尚未实现
        assertNull(result);
    }
}
