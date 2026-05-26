package com.qizhilian.api.workflow;

import com.qizhilian.api.base.ApiBaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流引擎API测试类
 * 覆盖流程定义、流程实例、审批任务等核心接口
 */
@Feature("工作流引擎")
@Epic("企智连API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WorkflowApiTest extends ApiBaseTest {

    private static Long processDefinitionId;
    private static Long processInstanceId;
    private static Long taskId;

    @BeforeAll
    public static void setup() {
        // 初始化测试数据
    }

    @Test
    @Order(1)
    @Story("流程定义")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建流程定义")
    void testCreateProcessDefinition() {
        Map<String, Object> definitionBody = new HashMap<>();
        definitionBody.put("name", "采购审批流程");
        definitionBody.put("key", "purchase_approval");
        definitionBody.put("description", "采购申请审批流程");
        definitionBody.put("category", "PURCHASE");

        Map<String, Object> node1 = new HashMap<>();
        node1.put("id", "start");
        node1.put("type", "START");
        node1.put("name", "开始");

        Map<String, Object> node2 = new HashMap<>();
        node2.put("id", "approval");
        node2.put("type", "APPROVAL");
        node2.put("name", "主管审批");
        node2.put("assigneeType", "ROLE");
        node2.put("assigneeValue", "MANAGER");

        Map<String, Object> node3 = new HashMap<>();
        node3.put("id", "end");
        node3.put("type", "END");
        node3.put("name", "结束");

        definitionBody.put("nodes", new Map[]{node1, node2, node3});

        Response response = post("/workflow/definitions", definitionBody);

        assertSuccess(response);
        processDefinitionId = response.jsonPath().getLong("data.id");
        assertNotNull(processDefinitionId, "流程定义ID不应为空");
    }

    @Test
    @Order(2)
    @Story("流程定义")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询流程定义列表")
    void testGetProcessDefinitionList() {
        Response response = get("/workflow/definitions?page=1&size=20");

        assertSuccess(response);
        assertFieldExists(response, "data.list");
        assertFieldExists(response, "data.total");
    }

    @Test
    @Order(3)
    @Story("流程定义")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询指定流程定义")
    void testGetProcessDefinitionById() {
        Assumptions.assumeTrue(processDefinitionId != null, "需要先创建流程定义");

        Response response = get("/workflow/definitions/" + processDefinitionId);

        assertSuccess(response);
        assertFieldEquals(response, "data.id", processDefinitionId.intValue());
    }

    @Test
    @Order(4)
    @Story("流程实例")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("启动流程实例")
    void testStartProcessInstance() {
        Assumptions.assumeTrue(processDefinitionId != null, "需要先创建流程定义");

        Map<String, Object> instanceBody = new HashMap<>();
        instanceBody.put("definitionId", processDefinitionId);
        instanceBody.put("businessKey", "PO-2024-001");
        instanceBody.put("variables", Map.of(
                "amount", 50000,
                "applicant", "张三",
                "department", "采购部"
        ));

        Response response = post("/workflow/instances", instanceBody);

        assertSuccess(response);
        processInstanceId = response.jsonPath().getLong("data.id");
        assertNotNull(processInstanceId, "流程实例ID不应为空");
    }

    @Test
    @Order(5)
    @Story("流程实例")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询流程实例列表")
    void testGetProcessInstanceList() {
        Response response = get("/workflow/instances?page=1&size=20");

        assertSuccess(response);
        assertFieldExists(response, "data.list");
    }

    @Test
    @Order(6)
    @Story("流程实例")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询流程实例详情")
    void testGetProcessInstanceDetail() {
        Assumptions.assumeTrue(processInstanceId != null, "需要先启动流程实例");

        Response response = get("/workflow/instances/" + processInstanceId);

        assertSuccess(response);
        assertFieldExists(response, "data.currentNode");
        assertFieldExists(response, "data.status");
    }

    @Test
    @Order(7)
    @Story("审批任务")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("查询待办任务列表")
    void testGetTodoTaskList() {
        Response response = get("/workflow/tasks/todo?page=1&size=20");

        assertSuccess(response);
        assertFieldExists(response, "data.list");

        // 获取第一个任务的ID
        if (response.jsonPath().getList("data.list").size() > 0) {
            taskId = response.jsonPath().getLong("data.list[0].id");
        }
    }

    @Test
    @Order(8)
    @Story("审批任务")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("审批任务通过")
    void testApproveTask() {
        // 如果没有待办任务，先尝试获取
        if (taskId == null) {
            Response todoResponse = get("/workflow/tasks/todo?page=1&size=1");
            if (todoResponse.jsonPath().getList("data.list").size() > 0) {
                taskId = todoResponse.jsonPath().getLong("data.list[0].id");
            }
        }

        Assumptions.assumeTrue(taskId != null, "没有可审批的任务");

        Map<String, Object> approveBody = new HashMap<>();
        approveBody.put("action", "APPROVE");
        approveBody.put("comment", "同意，符合预算要求");
        approveBody.put("variables", Map.of("approvedAmount", 50000));

        Response response = post("/workflow/tasks/" + taskId + "/complete", approveBody);

        assertSuccess(response);
    }

    @Test
    @Order(9)
    @Story("审批任务")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("审批任务驳回")
    void testRejectTask() {
        // 此测试需要一个新的流程实例和任务
        Map<String, Object> instanceBody = new HashMap<>();
        instanceBody.put("definitionId", processDefinitionId);
        instanceBody.put("businessKey", "PO-2024-002");

        Response startResponse = post("/workflow/instances", instanceBody);
        assertSuccess(startResponse);

        // 等待任务生成并获取
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Response todoResponse = get("/workflow/tasks/todo?page=1&size=1");
        if (todoResponse.jsonPath().getList("data.list").size() > 0) {
            Long newTaskId = todoResponse.jsonPath().getLong("data.list[0].id");

            Map<String, Object> rejectBody = new HashMap<>();
            rejectBody.put("action", "REJECT");
            rejectBody.put("comment", "金额超出预算，请重新申请");

            Response response = post("/workflow/tasks/" + newTaskId + "/complete", rejectBody);
            assertSuccess(response);
        }
    }

    @Test
    @Order(10)
    @Story("流程实例")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询流程历史记录")
    void testGetProcessHistory() {
        Assumptions.assumeTrue(processInstanceId != null, "需要先启动流程实例");

        Response response = get("/workflow/instances/" + processInstanceId + "/history");

        assertSuccess(response);
        assertFieldExists(response, "data.activities");
        assertFieldExists(response, "data.variables");
    }

    @Test
    @Order(11)
    @Story("流程实例")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("终止流程实例")
    void testTerminateProcessInstance() {
        // 创建一个新的流程实例用于终止测试
        Map<String, Object> instanceBody = new HashMap<>
    @Test
    @Order(12)
    @Story("流程定义")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("删除流程定义")
    void testDeleteProcessDefinition() {
        Assumptions.assumeTrue(processDefinitionId != null, "需要先创建流程定义");

        Response response = delete("/workflow/definitions/" + processDefinitionId);

        assertSuccess(response);
    }
}
