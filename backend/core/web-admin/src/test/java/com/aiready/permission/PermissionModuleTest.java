package com.aiready.permission;

import com.aiready.permission.entity.Permission;
import com.aiready.permission.entity.Role;
import com.aiready.permission.entity.RolePermission;
import com.aiready.permission.entity.UserRole;
import com.aiready.permission.service.PermissionService;
import com.aiready.permission.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限模块单元测试
 */
@SpringBootTest
@Transactional
public class PermissionModuleTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    /**
     * 测试权限CRUD
     */
    @Test
    public void testPermissionCRUD() {
        // 创建权限
        Permission permission = new Permission();
        permission.setPermissionCode("test:permission");
        permission.setPermissionName("测试权限");
        permission.setType(3);
        permission.setPath("/test");
        permission.setSortOrder(1);
        permission.setStatus(1);
        
        boolean saveResult = permissionService.save(permission);
        assertTrue(saveResult);
        assertNotNull(permission.getId());
        
        // 查询权限
        Permission savedPermission = permissionService.getById(permission.getId());
        assertNotNull(savedPermission);
        assertEquals("test:permission", savedPermission.getPermissionCode());
        
        // 更新权限
        savedPermission.setPermissionName("更新后的权限名称");
        boolean updateResult = permissionService.updateById(savedPermission);
        assertTrue(updateResult);
        
        Permission updatedPermission = permissionService.getById(permission.getId());
        assertEquals("更新后的权限名称", updatedPermission.getPermissionName());
        
        // 删除权限
        boolean deleteResult = permissionService.removeById(permission.getId());
        assertTrue(deleteResult);
        
        Permission deletedPermission = permissionService.getById(permission.getId());
        assertNull(deletedPermission);
    }

    /**
     * 测试角色CRUD
     */
    @Test
    public void testRoleCRUD() {
        // 创建角色
        Role role = new Role();
        role.setRoleCode("test:role");
        role.setRoleName("测试角色");
        role.setDataScope(1);
        role.setSortOrder(1);
        role.setStatus(1);
        
        boolean saveResult = roleService.save(role);
        assertTrue(saveResult);
        assertNotNull(role.getId());
        
        // 查询角色
        Role savedRole = roleService.getById(role.getId());
        assertNotNull(savedRole);
        assertEquals("test:role", savedRole.getRoleCode());
        
        // 更新角色
        savedRole.setRoleName("更新后的角色名称");
        boolean updateResult = roleService.updateById(savedRole);
        assertTrue(updateResult);
        
        Role updatedRole = roleService.getById(role.getId());
        assertEquals("更新后的角色名称", updatedRole.getRoleName());
        
        // 删除角色
        boolean deleteResult = roleService.removeById(role.getId());
        assertTrue(deleteResult);
        
        Role deletedRole = roleService.getById(role.getId());
        assertNull(deletedRole);
    }

    /**
     * 测试权限检查
     */
    @Test
    public void testPermissionCheck() {
        // 创建测试权限
        Permission permission1 = new Permission();
        permission1.setPermissionCode("test:read");
        permission1.setPermissionName("读取权限");
        permission1.setType(3);
        permission1.setStatus(1);
        permissionService.save(permission1);
        
        Permission permission2 = new Permission();
        permission2.setPermissionCode("test:write");
        permission2.setPermissionName("写入权限");
        permission2.setType(3);
        permission2.setStatus(1);
        permissionService.save(permission2);
        
        // 创建测试角色
        Role role = new Role();
        role.setRoleCode("test:user");
        role.setRoleName("测试用户");
        role.setDataScope(1);
        role.setStatus(1);
        roleService.save(role);
        
        // 分配权限给角色
        permissionService.assignRolePermissions(role.getId(), 
                Arrays.asList(permission1.getId(), permission2.getId()));
        
        // 验证权限分配
        List<Long> rolePermissionIds = permissionService.getRolePermissionIds(role.getId());
        assertEquals(2, rolePermissionIds.size());
        assertTrue(rolePermissionIds.contains(permission1.getId()));
        assertTrue(rolePermissionIds.contains(permission2.getId()));
    }

    /**
     * 测试角色检查
     */
    @Test
    public void testRoleCheck() {
        // 创建测试角色
        Role role1 = new Role();
        role1.setRoleCode("role:admin");
        role1.setRoleName("管理员");
        role1.setDataScope(1);
        role1.setStatus(1);
        roleService.save(role1);
        
        Role role2 = new Role();
        role2.setRoleCode("role:user");
        role2.setRoleName("普通用户");
        role2.setDataScope(4);
        role2.setStatus(1);
        roleService.save(role2);
        
        // 根据角色编码查询
        Role foundRole = roleService.getByRoleCode("role:admin");
        assertNotNull(foundRole);
        assertEquals("管理员", foundRole.getRoleName());
        
        // 测试不存在的角色
        Role notFoundRole = roleService.getByRoleCode("role:notexist");
        assertNull(notFoundRole);
    }

    /**
     * 测试数据权限范围
     */
    @Test
    public void testDataScope() {
        // 创建不同数据权限范围的角色
        Role roleAll = new Role();
        roleAll.setRoleCode("scope:all");
        roleAll.setRoleName("全部数据");
        roleAll.setDataScope(1);
        roleAll.setStatus(1);
        roleService.save(roleAll);
        
        Role roleDept = new Role();
        roleDept.setRoleCode("scope:dept");
        roleDept.setRoleName("部门数据");
        roleDept.setDataScope(2);
        roleDept.setStatus(1);
        roleService.save(roleDept);
        
        Role roleSelf = new Role();
        roleSelf.setRoleCode("scope:self");
        roleSelf.setRoleName("本人数据");
        roleSelf.setDataScope(4);
        roleSelf.setStatus(1);
        roleService.save(roleSelf);
        
        // 验证数据权限范围
        assertEquals(1, roleAll.getDataScope());
        assertEquals(2, roleDept.getDataScope());
        assertEquals(4, roleSelf.getDataScope());
    }
}
