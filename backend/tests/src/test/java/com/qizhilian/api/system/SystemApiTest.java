package com.qizhilian.api.system;

import com.qizhilian.api.base.BaseApiTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 系统管理接口测试
 * 包含用户、角色、部门管理
 */
@Feature("系统管理模块")
@Story("系统管理API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemApiTest extends BaseApiTest {
    
    private static Long roleId;
    private static Long deptId;
    private static Long userId;
    
    // ==================== 角色管理 ====================
    
    @Test
    @Order(1)
    @DisplayName("创建角色 - 正向测试")
    @Description("创建新的系统角色")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateRole() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "测试角色" + System.currentTimeMillis());
        requestBody.put("code", "ROLE_TEST_" + System.currentTimeMillis());
        requestBody.put("description", "自动化测试创建的角色");
        requestBody.put("status", "active");
        
        // 权限列表
        java.util.List<Long> permissions = java.util.Arrays.asList(1L, 2L, 3L);
        requestBody.put("permissions", permissions);
        
        Response response = post("/system/roles", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.roleId", notNullValue())
            .body("data.name", equalTo(requestBody.get("name")));
        
        roleId = response.jsonPath().getLong("data.roleId");
    }
    
    @Test
    @Order(2)
    @DisplayName("获取角色列表")
    @Description("分页查询角色列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetRoleList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        params.put("status", "active");
        
        Response response = get("/system/roles", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue())
            .body("data.total", greaterThanOrEqualTo(0));
    }
    
    @Test
    @Order(3)
    @DisplayName("获取角色详情")
    @Description("根据ID获取角色详情")
    @Severity(SeverityLevel.NORMAL)
    void testGetRoleDetail() {
        if (roleId == null) {
            testCreateRole();
        }
        
        Response response = get("/system/roles/" + roleId);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.roleId", equalTo(roleId.intValue()))
            .body("data.permissions", notNullValue());
    }
    
    @Test
    @Order(4)
    @DisplayName("更新角色")
    @Description("更新角色信息和权限")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateRole() {
        if (roleId == null) {
            testCreateRole();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("description", "更新后的描述");
        requestBody.put("permissions", java.util.Arrays.asList(1L, 2L, 3L, 4L, 5L));
        
        Response response = put("/system/roles/" + roleId, requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
    }
    
    // ==================== 部门管理 ====================
    
    @Test
    @Order(10)
    @DisplayName("创建部门")
    @Description("创建新的部门")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateDepartment() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", dataGenerator.generateDepartmentName());
        requestBody.put("code", "DEPT_" + System.currentTimeMillis());
        requestBody.put("parentId", 0);
        requestBody.put("sort", 1);
        requestBody.put("managerName", dataGenerator.generateName());
        requestBody.put("status", "active");
        
        Response response = post("/system/departments", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.deptId", notNullValue());
        
        deptId = response.jsonPath().getLong("data.deptId");
    }
    
    @Test
    @Order(11)
    @DisplayName("获取部门树")
    @Description("获取部门树形结构")
    @Severity(SeverityLevel.NORMAL)
    void testGetDepartmentTree() {
        Response response = get("/system/departments/tree");
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data", notNullValue());
    }
    
    @Test
    @Order(12)
    @DisplayName("更新部门")
    @Description("更新部门信息")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateDepartment() {
        if (deptId == null) {
            testCreateDepartment();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("managerName", dataGenerator.generateName());
        requestBody.put("sort", 2);
        
        Response response = put("/system/departments/" + deptId, requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
    }
    
    // ==================== 用户管理 ====================
    
    @Test
    @Order(20)
    @DisplayName("创建系统用户")
    @Description("创建新的系统用户")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateUser() {
        if (deptId == null) {
            testCreateDepartment();
        }
        if (roleId == null) {
            testCreateRole();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", dataGenerator.generateUsername());
        requestBody.put("password", "User@123456");
        requestBody.put("realName", dataGenerator.generateName());
        requestBody.put("email", dataGenerator.generateEmail());
        requestBody.put("phone", dataGenerator.generatePhone());
        requestBody.put("deptId", deptId);
        requestBody.put("roleIds", java.util.Arrays.asList(roleId));
        requestBody.put("status", "active");
        
        Response response = post("/system/users", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.userId", notNullValue());
        
        userId = response.jsonPath().getLong("data.userId");
    }
    
    @Test
    @Order(21)
    @DisplayName("获取用户列表")
    @Description("分页查询用户列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetUserList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        params.put("status", "active");
        
        Response response = get("/system/users", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue())
            .body("data.total", greaterThanOrEqualTo(0));
    }
    
    @Test
    @Order(22)
    @DisplayName("获取用户详情")
    @Description("根据ID获取用户详情")
    @Severity(SeverityLevel.NORMAL)
    void testGetUserDetail() {
        if (userId == null) {
            testCreateUser();
        }
        
        Response response = get("/system/users/" + userId);
        
        assertStatusCode(response, 200);
        response.then()