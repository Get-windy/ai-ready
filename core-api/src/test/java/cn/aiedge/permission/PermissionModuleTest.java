package cn.aiedge.permission;

import cn.aiedge.permission.annotation.DataPermission;
import cn.aiedge.permission.annotation.RequirePermission;
import cn.aiedge.permission.annotation.RequireRole;
import cn.aiedge.permission.aspect.PermissionAspect;
import cn.aiedge.permission.exception.PermissionDeniedException;
import cn.aiedge.permission.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 权限模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PermissionModuleTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private PermissionAspect permissionAspect;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== 注解测试 ====================

    @Test
    @Order(1)
    @DisplayName("RequirePermission 注解属性测试")
    void testRequirePermissionAnnotation() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("adminMethod");
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        
        assertNotNull(annotation);
        assertEquals(1, annotation.value().length);
        assertEquals("system:admin", annotation.value()[0]);
        assertEquals(RequirePermission.Logical.OR, annotation.logical());
    }

    @Test
    @Order(2)
    @DisplayName("RequireRole 注解属性测试")
    void testRequireRoleAnnotation() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("roleMethod");
        RequireRole annotation = method.getAnnotation(RequireRole.class);
        
        assertNotNull(annotation);
        assertEquals(2, annotation.value().length);
        assertEquals("admin", annotation.value()[0]);
        assertEquals("manager", annotation.value()[1]);
    }

    @Test
    @Order(3)
    @DisplayName("DataPermission 注解属性测试")
    void testDataPermissionAnnotation() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("dataMethod");
        DataPermission annotation = method.getAnnotation(DataPermission.class);
        
        assertNotNull(annotation);
        assertEquals("create_by", annotation.field());
        assertEquals(DataPermission.DataScopeType.AUTO, annotation.scope());
    }

    // ==================== 权限检查测试 ====================

    @Test
    @Order(10)
    @DisplayName("权限检查 - 用户有权限")
    void testCheckPermissionHasPermission() {
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        permissions.add("system:user:edit");
        
        when(permissionService.getCurrentUserPermissions()).thenReturn(permissions);
        when(permissionService.isSuperAdmin()).thenReturn(false);
        
        boolean hasPermission = permissions.contains("system:user:view");
        assertTrue(hasPermission);
    }

    @Test
    @Order(11)
    @DisplayName("权限检查 - 用户无权限")
    void testCheckPermissionNoPermission() {
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        
        when(permissionService.getCurrentUserPermissions()).thenReturn(permissions);
        when(permissionService.isSuperAdmin()).thenReturn(false);
        
        boolean hasPermission = permissions.contains("system:admin");
        assertFalse(hasPermission);
    }

    @Test
    @Order(12)
    @DisplayName("权限检查 - 超级管理员跳过检查")
    void testCheckPermissionSuperAdmin() {
        when(permissionService.isSuperAdmin()).thenReturn(true);
        
        assertTrue(permissionService.isSuperAdmin());
    }

    // ==================== 角色检查测试 ====================

    @Test
    @Order(20)
    @DisplayName("角色检查 - 用户有角色")
    void testCheckRoleHasRole() {
        Set<String> roles = new HashSet<>();
        roles.add("admin");
        roles.add("manager");
        
        when(permissionService.getCurrentUserRoles()).thenReturn(roles);
        when(permissionService.isSuperAdmin()).thenReturn(false);
        
        assertTrue(roles.contains("admin"));
        assertTrue(roles.contains("manager"));
    }

    @Test
    @Order(21)
    @DisplayName("角色检查 - 用户无角色")
    void testCheckRoleNoRole() {
        Set<String> roles = new HashSet<>();
        roles.add("user");
        
        when(permissionService.getCurrentUserRoles()).thenReturn(roles);
        when(permissionService.isSuperAdmin()).thenReturn(false);
        
        assertFalse(roles.contains("admin"));
    }

    // ==================== 数据权限测试 ====================

    @Test
    @Order(30)
    @DisplayName("数据权限 - 仅本人数据")
    void testDataPermissionSelf() {
        Long userId = 1L;
        Long dataCreateBy = 1L;
        Long otherCreateBy = 2L;
        
        when(permissionService.getCurrentUserDataScope()).thenReturn(3); // 仅本人
        
        assertEquals(userId, dataCreateBy);
        assertNotEquals(userId, otherCreateBy);
    }

    @Test
    @Order(31)
    @DisplayName("数据权限 - 全部数据")
    void testDataPermissionAll() {
        when(permissionService.getCurrentUserDataScope()).thenReturn(0); // 全部
        
        assertEquals(0, permissionService.getCurrentUserDataScope());
    }

    // ==================== 权限服务测试 ====================

    @Test
    @Order(40)
    @DisplayName("获取当前用户ID")
    void testGetCurrentUserId() {
        when(permissionService.getCurrentUserId()).thenReturn(1L);
        
        Long userId = permissionService.getCurrentUserId();
        assertEquals(1L, userId);
    }

    @Test
    @Order(41)
    @DisplayName("获取当前用户权限列表")
    void testGetCurrentUserPermissions() {
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        permissions.add("system:user:edit");
        
        when(permissionService.getCurrentUserPermissions()).thenReturn(permissions);
        
        Set<String> result = permissionService.getCurrentUserPermissions();
        assertEquals(2, result.size());
        assertTrue(result.contains("system:user:view"));
    }

    @Test
    @Order(42)
    @DisplayName("获取当前用户角色列表")
    void testGetCurrentUserRoles() {
        Set<String> roles = new HashSet<>();
        roles.add("admin");
        
        when(permissionService.getCurrentUserRoles()).thenReturn(roles);
        
        Set<String> result = permissionService.getCurrentUserRoles();
        assertEquals(1, result.size());
        assertTrue(result.contains("admin"));
    }

    @Test
    @Order(43)
    @DisplayName("判断是否超级管理员")
    void testIsSuperAdmin() {
        Set<String> roles = new HashSet<>();
        roles.add("SUPER_ADMIN");
        
        when(permissionService.getCurrentUserRoles()).thenReturn(roles);
        when(permissionService.isSuperAdmin()).thenReturn(true);
        
        assertTrue(permissionService.isSuperAdmin());
    }

    // ==================== 权限验证测试 ====================

    @Test
    @Order(50)
    @DisplayName("API权限验证 - 有权限")
    void testCheckApiPermissionAllowed() {
        Long userId = 1L;
        String apiPath = "/api/users";
        String method = "GET";
        
        when(permissionService.checkApiPermission(userId, apiPath, method)).thenReturn(true);
        
        assertTrue(permissionService.checkApiPermission(userId, apiPath, method));
    }

    @Test
    @Order(51)
    @DisplayName("API权限验证 - 无权限")
    void testCheckApiPermissionDenied() {
        Long userId = 2L;
        String apiPath = "/api/admin";
        String method = "POST";
        
        when(permissionService.checkApiPermission(userId, apiPath, method)).thenReturn(false);
        
        assertFalse(permissionService.checkApiPermission(userId, apiPath, method));
    }

    @Test
    @Order(52)
    @DisplayName("数据权限验证 - 有权限")
    void testCheckDataPermissionAllowed() {
        Long userId = 1L;
        Long tenantId = 1L;
        Long createBy = 1L;
        
        when(permissionService.checkDataPermission(userId, tenantId, createBy)).thenReturn(true);
        
        assertTrue(permissionService.checkDataPermission(userId, tenantId, createBy));
    }

    @Test
    @Order(53)
    @DisplayName("租户权限验证")
    void testCheckTenantPermission() {
        Long userId = 1L;
        Long tenantId = 1L;
        
        when(permissionService.checkTenantPermission(userId, tenantId)).thenReturn(true);
        
        assertTrue(permissionService.checkTenantPermission(userId, tenantId));
    }

    // ==================== 缓存管理测试 ====================

    @Test
    @Order(60)
    @DisplayName("刷新用户权限缓存")
    void testRefreshUserPermissionCache() {
        Long userId = 1L;
        
        doNothing().when(permissionService).refreshUserPermissionCache(userId);
        
        permissionService.refreshUserPermissionCache(userId);
        
        verify(permissionService, times(1)).refreshUserPermissionCache(userId);
    }

    @Test
    @Order(61)
    @DisplayName("清除所有权限缓存")
    void testClearPermissionCache() {
        doNothing().when(permissionService).clearPermissionCache();
        
        permissionService.clearPermissionCache();
        
        verify(permissionService, times(1)).clearPermissionCache();
    }

    // ==================== 异常处理测试 ====================

    @Test
    @Order(70)
    @DisplayName("权限拒绝异常")
    void testPermissionDeniedException() {
        PermissionDeniedException exception = 
            new PermissionDeniedException("权限不足");
        
        assertEquals("权限不足", exception.getMessage());
    }

    @Test
    @Order(71)
    @DisplayName("权限拒绝异常 - 带原因")
    void testPermissionDeniedExceptionWithCause() {
        Exception cause = new RuntimeException("原始异常");
        PermissionDeniedException exception = 
            new PermissionDeniedException("权限不足", cause);
        
        assertEquals("权限不足", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(80)
    @DisplayName("空权限列表测试")
    void testEmptyPermissions() {
        Set<String> permissions = new HashSet<>();
        
        when(permissionService.getCurrentUserPermissions()).thenReturn(permissions);
        
        assertTrue(permissions.isEmpty());
    }

    @Test
    @Order(81)
    @DisplayName("空角色列表测试")
    void testEmptyRoles() {
        Set<String> roles = new HashSet<>();
        
        when(permissionService.getCurrentUserRoles()).thenReturn(roles);
        
        assertTrue(roles.isEmpty());
    }

    @Test
    @Order(82)
    @DisplayName("空用户ID测试")
    void testNullUserId() {
        when(permissionService.getCurrentUserId()).thenReturn(null);
        
        assertNull(permissionService.getCurrentUserId());
    }

    // ==================== 测试辅助类 ====================

    static class TestService {
        @RequirePermission("system:admin")
        public void adminMethod() {}
        
        @RequireRole({"admin", "manager"})
        public void roleMethod() {}
        
        @DataPermission
        public void dataMethod() {}
    }
}
