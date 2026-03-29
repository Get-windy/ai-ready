package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("角色服务测试")
class SysRoleServiceTest {

    private SysRole testRole;

    @BeforeEach
    void setUp() {
        testRole = new SysRole();
        testRole.setId(1L);
        testRole.setTenantId(1L);
        testRole.setRoleName("管理员");
        testRole.setRoleCode("admin");
        testRole.setStatus(0);
        testRole.setSort(1);
    }

    @Test
    @DisplayName("角色实体 - 属性设置")
    void testRoleEntityProperties() {
        assertNotNull(testRole.getId());
        assertEquals(1L, testRole.getId());
        assertEquals("管理员", testRole.getRoleName());
        assertEquals("admin", testRole.getRoleCode());
        assertEquals(0, testRole.getStatus());
        assertEquals(1, testRole.getSort());
    }

    @Test
    @DisplayName("角色链式设置")
    void testRoleChainSetter() {
        SysRole role = new SysRole()
                .setId(2L)
                .setRoleName("测试角色")
                .setRoleCode("test_role")
                .setSort(2);

        assertEquals(2L, role.getId());
        assertEquals("测试角色", role.getRoleName());
        assertEquals("test_role", role.getRoleCode());
        assertEquals(2, role.getSort());
    }

    @Test
    @DisplayName("角色编码格式验证")
    void testRoleCodeFormat() {
        // 角色编码通常使用小写和下划线
        String roleCode = testRole.getRoleCode();
        assertTrue(roleCode.matches("[a-z_]+"));
    }
}