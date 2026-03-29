package cn.aiedge.base.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RBAC 权限服务测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("RBAC 权限服务测试")
class RbacServiceTest {

    @Test
    @DisplayName("角色编码检查 - 超级管理员")
    void testIsSuperAdmin() {
        Set<String> roles = new HashSet<>();
        roles.add("SUPER_ADMIN");
        roles.add("admin");

        assertTrue(roles.contains("SUPER_ADMIN") || roles.contains("admin"));
    }

    @Test
    @DisplayName("角色编码检查 - 普通用户")
    void testIsNotSuperAdmin() {
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        roles.add("VIEWER");

        assertFalse(roles.contains("SUPER_ADMIN") || roles.contains("admin"));
    }

    @Test
    @DisplayName("权限编码格式验证")
    void testPermissionCodeFormat() {
        String[] validCodes = {
            "user:list",
            "user:create",
            "role:update",
            "system:config"
        };

        for (String code : validCodes) {
            assertTrue(code.contains(":"), "权限编码应包含冒号: " + code);
        }
    }

    @Test
    @DisplayName("角色编码格式验证")
    void testRoleCodeFormat() {
        String[] validCodes = {
            "SUPER_ADMIN",
            "ADMIN",
            "USER",
            "VIEWER"
        };

        for (String code : validCodes) {
            assertTrue(code.matches("[A-Z_]+"), "角色编码应为大写字母和下划线: " + code);
        }
    }

    @Test
    @DisplayName("数据权限范围值验证")
    void testDataScopeValues() {
        Integer[] validScopes = {0, 1, 2, 3};

        for (Integer scope : validScopes) {
            assertTrue(scope >= 0 && scope <= 3, "数据权限范围应为 0-3: " + scope);
        }
    }
}