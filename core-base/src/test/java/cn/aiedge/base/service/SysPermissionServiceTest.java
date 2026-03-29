package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("权限服务测试")
class SysPermissionServiceTest {

    private SysPermission testPermission;

    @BeforeEach
    void setUp() {
        testPermission = new SysPermission();
        testPermission.setId(1L);
        testPermission.setTenantId(1L);
        testPermission.setParentId(0L);
        testPermission.setPermissionName("用户管理");
        testPermission.setPermissionCode("user:manage");
        testPermission.setPermissionType(1);
        testPermission.setSort(1);
        testPermission.setStatus(0);
    }

    @Test
    @DisplayName("权限实体 - 属性设置")
    void testPermissionEntityProperties() {
        assertNotNull(testPermission.getId());
        assertEquals(1L, testPermission.getId());
        assertEquals("用户管理", testPermission.getPermissionName());
        assertEquals("user:manage", testPermission.getPermissionCode());
        assertEquals(1, testPermission.getPermissionType());
        assertEquals(0, testPermission.getStatus());
    }

    @Test
    @DisplayName("权限链式设置")
    void testPermissionChainSetter() {
        SysPermission permission = new SysPermission()
                .setId(2L)
                .setPermissionName("订单管理")
                .setPermissionCode("order:manage")
                .setPermissionType(2)
                .setApiPath("/api/order")
                .setMethod("GET");

        assertEquals(2L, permission.getId());
        assertEquals("订单管理", permission.getPermissionName());
        assertEquals("/api/order", permission.getApiPath());
        assertEquals("GET", permission.getMethod());
    }

    @Test
    @DisplayName("权限编码格式验证")
    void testPermissionCodeFormat() {
        // 权限编码通常使用模块:操作格式
        String permissionCode = testPermission.getPermissionCode();
        assertTrue(permissionCode.contains(":"));
    }

    @Test
    @DisplayName("权限类型验证")
    void testPermissionTypeValidation() {
        // 权限类型: 1-菜单 2-按钮 3-API
        Integer permissionType = testPermission.getPermissionType();
        assertTrue(permissionType >= 1 && permissionType <= 3);
    }
}