package cn.aiedge.permission;

import cn.aiedge.permission.controller.PermissionController;
import cn.aiedge.permission.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 权限控制器测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PermissionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    private ObjectMapper objectMapper;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController).build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== 当前用户权限查询测试 ====================

    @Test
    @Order(1)
    @DisplayName("获取当前用户权限 - 成功")
    void testGetCurrentUserPermissionsSuccess() throws Exception {
        Set<String> permissions = new HashSet<>(Arrays.asList("system:user:view", "system:user:edit"));
        
        when(permissionService.getCurrentUserPermissions()).thenReturn(permissions);

        mockMvc.perform(get("/api/permission/current/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("获取当前用户角色 - 成功")
    void testGetCurrentUserRolesSuccess() throws Exception {
        Set<String> roles = new HashSet<>(Arrays.asList("admin", "manager"));
        
        when(permissionService.getCurrentUserRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/permission/current/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("检查当前用户权限 - 有权限")
    void testCheckCurrentUserPermissionHasPermission() throws Exception {
        when(permissionService.hasPermission("system:user:view")).thenReturn(true);

        mockMvc.perform(get("/api/permission/current/check-permission")
                .param("permissionCode", "system:user:view"))
                .andExpect(status().isOk())
                .andExpect(result -> "true".equals(result.getResponse().getContentAsString()));
    }

    @Test
    @Order(4)
    @DisplayName("检查当前用户角色 - 有角色")
    void testCheckCurrentUserRoleHasRole() throws Exception {
        when(permissionService.hasRole("admin")).thenReturn(true);

        mockMvc.perform(get("/api/permission/current/check-role")
                .param("roleCode", "admin"))
                .andExpect(status().isOk());
    }

    // ==================== 用户权限管理测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取用户权限列表 - 成功")
    void testGetUserPermissionsSuccess() throws Exception {
        Long userId = 1L;
        List<String> permissions = Arrays.asList("system:user:view", "system:user:edit");
        
        when(permissionService.getUserPermissionCodes(userId)).thenReturn(permissions);

        mockMvc.perform(get("/api/permission/user/{userId}/permissions", userId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("获取用户角色列表 - 成功")
    void testGetUserRolesSuccess() throws Exception {
        Long userId = 1L;
        List<String> roles = Arrays.asList("admin");
        
        when(permissionService.getUserRoleCodes(userId)).thenReturn(roles);

        mockMvc.perform(get("/api/permission/user/{userId}/roles", userId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(12)
    @DisplayName("获取用户角色ID列表 - 成功")
    void testGetUserRoleIdsSuccess() throws Exception {
        Long userId = 1L;
        List<Long> roleIds = Arrays.asList(1L, 2L);
        
        when(permissionService.getUserRoleIds(userId)).thenReturn(roleIds);

        mockMvc.perform(get("/api/permission/user/{userId}/role-ids", userId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(13)
    @DisplayName("分配用户角色 - 成功")
    void testAssignUserRolesSuccess() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", 1L);
        request.put("roleIds", Arrays.asList(1L, 2L));
        
        doNothing().when(permissionService).assignUserRoles(anyLong(), anyList());

        mockMvc.perform(post("/api/permission/user/assign-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(14)
    @DisplayName("清除用户角色 - 成功")
    void testClearUserRolesSuccess() throws Exception {
        Long userId = 1L;
        
        doNothing().when(permissionService).clearUserRoles(userId);

        mockMvc.perform(delete("/api/permission/user/{userId}/roles", userId))
                .andExpect(status().isOk());
    }

    // ==================== 角色权限管理测试 ====================

    @Test
    @Order(20)
    @DisplayName("获取角色权限列表 - 成功")
    void testGetRolePermissionsSuccess() throws Exception {
        Long roleId = 1L;
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        
        when(permissionService.getRolePermissionIds(roleId)).thenReturn(permissionIds);

        mockMvc.perform(get("/api/permission/role/{roleId}/permissions", roleId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(21)
    @DisplayName("分配角色权限 - 成功")
    void testAssignRolePermissionsSuccess() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("roleId", 1L);
        request.put("permissionIds", Arrays.asList(1L, 2L));
        
        doNothing().when(permissionService).assignRolePermissions(anyLong(), anyList());

        mockMvc.perform(post("/api/permission/role/assign-permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(22)
    @DisplayName("清除角色权限 - 成功")
    void testClearRolePermissionsSuccess() throws Exception {
        Long roleId = 1L;
        
        doNothing().when(permissionService).clearRolePermissions(roleId);

        mockMvc.perform(delete("/api/permission/role/{roleId}/permissions", roleId))
                .andExpect(status().isOk());
    }

    // ==================== 权限验证测试 ====================

    @Test
    @Order(30)
    @DisplayName("验证API访问权限 - 成功")
    void testCheckApiPermissionSuccess() throws Exception {
        when(permissionService.checkApiPermission(1L, "/api/users", "GET")).thenReturn(true);

        mockMvc.perform(get("/api/permission/check/api")
                .param("userId", "1")
                .param("apiPath", "/api/users")
                .param("method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(31)
    @DisplayName("验证数据访问权限 - 成功")
    void testCheckDataPermissionSuccess() throws Exception {
        when(permissionService.checkDataPermission(1L, 1L, 1L)).thenReturn(true);

        mockMvc.perform(get("/api/permission/check/data")
                .param("userId", "1")
                .param("dataTenantId", "1")
                .param("dataCreateBy", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(32)
    @DisplayName("验证租户访问权限 - 成功")
    void testCheckTenantPermissionSuccess() throws Exception {
        when(permissionService.checkTenantPermission(1L, 1L)).thenReturn(true);

        mockMvc.perform(get("/api/permission/check/tenant")
                .param("userId", "1")
                .param("tenantId", "1"))
                .andExpect(status().isOk());
    }

    // ==================== 缓存管理测试 ====================

    @Test
    @Order(40)
    @DisplayName("刷新用户权限缓存 - 成功")
    void testRefreshUserPermissionCacheSuccess() throws Exception {
        Long userId = 1L;
        
        doNothing().when(permissionService).refreshUserPermissionCache(userId);

        mockMvc.perform(post("/api/permission/cache/refresh/{userId}", userId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(41)
    @DisplayName("清除所有权限缓存 - 成功")
    void testClearPermissionCacheSuccess() throws Exception {
        doNothing().when(permissionService).clearPermissionCache();

        mockMvc.perform(delete("/api/permission/cache/clear"))
                .andExpect(status().isOk());
    }

    // ==================== 异常场景测试 ====================

    @Test
    @Order(50)
    @DisplayName("获取用户权限 - 空列表")
    void testGetUserPermissionsEmpty() throws Exception {
        Long userId = 999L;
        
        when(permissionService.getUserPermissionCodes(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/permission/user/{userId}/permissions", userId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(51)
    @DisplayName("检查权限 - 无权限")
    void testCheckPermissionDenied() throws Exception {
        when(permissionService.hasPermission("system:admin")).thenReturn(false);

        mockMvc.perform(get("/api/permission/current/check-permission")
                .param("permissionCode", "system:admin"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(52)
    @DisplayName("检查角色 - 无角色")
    void testCheckRoleDenied() throws Exception {
        when(permissionService.hasRole("super_admin")).thenReturn(false);

        mockMvc.perform(get("/api/permission/current/check-role")
                .param("roleCode", "super_admin"))
                .andExpect(status().isOk());
    }
}
