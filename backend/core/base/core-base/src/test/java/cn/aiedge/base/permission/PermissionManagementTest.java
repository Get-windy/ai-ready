package cn.aiedge.base.permission;

import cn.aiedge.base.config.TestConfig;
import cn.aiedge.base.entity.*;
import cn.aiedge.base.service.*;
import cn.aiedge.base.util.PermissionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限管理模块测试类
 * 测试权限管理模块的核心功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class PermissionManagementTest {

    @Autowired
    private SysRoleService roleService;
    
    @Autowired
    private SysPermissionService permissionService;
    
    @Autowired
    private PermissionTemplateService templateService;
    
    @Autowired
    private RoleInheritanceService inheritanceService;
    
    @Autowired
    private DataPermissionService dataPermissionService;
    
    @Autowired
    private PermissionUtils permissionUtils;

    /**
     * 测试角色管理功能
     */
    @Test
    void testRoleManagement() {
        // 创建角色
        SysRole role = new SysRole();
        role.setRoleName("测试角色");
        role.setRoleCode("TEST_ROLE");
        role.setRoleType(1); // 自定义角色
        role.setDataScope(0); // 全部数据权限
        role.setStatus(0); // 启用
        role.setTenantId(1L);
        
        Long roleId = roleService.createRole(role);
        assertNotNull(roleId);
        
        // 查询角色
        SysRole savedRole = roleService.getById(roleId);
        assertNotNull(savedRole);
        assertEquals("测试角色", savedRole.getRoleName());
        assertEquals("TEST_ROLE", savedRole.getRoleCode());
        
        // 更新角色
        savedRole.setRoleName("更新测试角色");
        roleService.updateRole(savedRole);
        
        SysRole updatedRole = roleService.getById(roleId);
        assertEquals("更新测试角色", updatedRole.getRoleName());
    }

    /**
     * 测试权限管理功能
     */
    @Test
    void testPermissionManagement() {
        // 创建权限
        SysPermission permission = new SysPermission();
        permission.setPermissionName("测试权限");
        permission.setPermissionCode("test:permission");
        permission.setPermissionType(3); // API权限
        permission.setApiPath("/api/test");
        permission.setMethod("GET");
        permission.setStatus(0); // 启用
        permission.setTenantId(1L);
        
        Long permissionId = permissionService.createPermission(permission);
        assertNotNull(permissionId);
        
        // 查询权限
        SysPermission savedPermission = permissionService.getById(permissionId);
        assertNotNull(savedPermission);
        assertEquals("测试权限", savedPermission.getPermissionName());
        assertEquals("test:permission", savedPermission.getPermissionCode());
        
        // 更新权限
        savedPermission.setPermissionName("更新测试权限");
        permissionService.updatePermission(savedPermission);
        
        SysPermission updatedPermission = permissionService.getById(permissionId);
        assertEquals("更新测试权限", updatedPermission.getPermissionName());
    }

    /**
     * 测试权限模板功能
     */
    @Test
    void testPermissionTemplate() {
        // 创建权限模板
        PermissionTemplate template = new PermissionTemplate();
        template.setTemplateName("测试模板");
        template.setTemplateCode("TEST_TEMPLATE");
        template.setDescription("测试权限模板");
        template.setTemplateType(2); // 自定义模板
        template.setStatus(0); // 启用
        template.setTenantId(1L);
        template.setPermissionConfig(Arrays.asList(1L, 2L, 3L));
        
        Long templateId = templateService.createTemplate(template);
        assertNotNull(templateId);
        
        // 查询模板
        PermissionTemplate savedTemplate = templateService.getTemplateDetail(templateId);
        assertNotNull(savedTemplate);
        assertEquals("测试模板", savedTemplate.getTemplateName());
        assertEquals("TEST_TEMPLATE", savedTemplate.getTemplateCode());
        
        // 更新模板
        savedTemplate.setTemplateName("更新测试模板");
        templateService.updateTemplate(savedTemplate);
        
        PermissionTemplate updatedTemplate = templateService.getTemplateDetail(templateId);
        assertEquals("更新测试模板", updatedTemplate.getTemplateName());
    }

    /**
     * 测试角色继承功能
     */
    @Test
    void testRoleInheritance() {
        // 创建两个测试角色
        SysRole parentRole = new SysRole();
        parentRole.setRoleName("父角色");
        parentRole.setRoleCode("PARENT_ROLE");
        parentRole.setRoleType(1);
        parentRole.setStatus(0);
        parentRole.setTenantId(1L);
        Long parentRoleId = roleService.createRole(parentRole);
        
        SysRole childRole = new SysRole();
        childRole.setRoleName("子角色");
        childRole.setRoleCode("CHILD_ROLE");
        childRole.setRoleType(1);
        childRole.setStatus(0);
        childRole.setTenantId(1L);
        Long childRoleId = roleService.createRole(childRole);
        
        // 设置角色继承关系
        inheritanceService.setRoleInheritance(parentRoleId, childRoleId, 1);
        
        // 验证继承关系
        List<Long> parentIds = inheritanceService.getParentRoleIds(childRoleId);
        assertTrue(parentIds.contains(parentRoleId));
        
        List<Long> childIds = inheritanceService.getChildRoleIds(parentRoleId);
        assertTrue(childIds.contains(childRoleId));
    }

    /**
     * 测试数据权限功能
     */
    @Test
    void testDataPermission() {
        // 创建数据权限
        DataPermission dataPermission = new DataPermission();
        dataPermission.setPermissionName("测试数据权限");
        dataPermission.setPermissionCode("test:data:permission");
        dataPermission.setDataScope(0); // 全部数据
        dataPermission.setScopeType(3); // 全局
        dataPermission.setStatus(0); // 启用
        dataPermission.setTenantId(1L);
        
        Long permissionId = dataPermissionService.createDataPermission(dataPermission);
        assertNotNull(permissionId);
        
        // 查询数据权限
        DataPermission savedPermission = dataPermissionService.getById(permissionId);
        assertNotNull(savedPermission);
        assertEquals("测试数据权限", savedPermission.getPermissionName());
        assertEquals("test:data:permission", savedPermission.getPermissionCode());
    }

    /**
     * 测试权限工具类功能
     */
    @Test
    void testPermissionUtils() {
        // 测试权限编码验证
        assertTrue(permissionUtils.isValidPermissionCode("user:create"));
        assertTrue(permissionUtils.isValidPermissionCode("system_manage"));
        assertFalse(permissionUtils.isValidPermissionCode("user create")); // 包含空格
        
        // 测试角色编码验证
        assertTrue(permissionUtils.isValidRoleCode("ADMIN_USER"));
        assertTrue(permissionUtils.isValidRoleCode("normaluser"));
        assertFalse(permissionUtils.isValidRoleCode("admin user")); // 包含空格
        
        // 测试格式化功能
        assertEquals("user_create", permissionUtils.formatPermissionCode("User Create"));
        assertEquals("ADMIN_USER", permissionUtils.formatRoleCode("admin user"));
    }
}