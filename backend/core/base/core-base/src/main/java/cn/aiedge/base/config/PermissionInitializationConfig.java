package cn.aiedge.base.config;

import cn.aiedge.base.entity.*;
import cn.aiedge.base.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 权限初始化配置
 * 初始化系统默认的角色、权限和权限模板
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionInitializationConfig implements ApplicationRunner {

    private final SysRoleService roleService;
    private final SysPermissionService permissionService;
    private final PermissionTemplateService templateService;
    private final SysUserService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化权限配置...");
        
        // 初始化系统角色
        initSystemRoles();
        
        // 初始化系统权限
        initSystemPermissions();
        
        // 初始化权限模板
        initPermissionTemplates();
        
        log.info("权限配置初始化完成");
    }

    /**
     * 初始化系统角色
     */
    private void initSystemRoles() {
        log.info("初始化系统角色...");
        
        // 检查是否已经有系统角色
        // 这里简化处理，实际应用中需要更详细的检查逻辑
        
        // 创建超级管理员角色
        SysRole superAdminRole = new SysRole();
        superAdminRole.setRoleName("超级管理员");
        superAdminRole.setRoleCode("SUPER_ADMIN");
        superAdminRole.setRoleType(0); // 系统角色
        superAdminRole.setDataScope(0); // 全部数据权限
        superAdminRole.setStatus(0); // 启用
        superAdminRole.setRemark("系统最高权限角色");
        superAdminRole.setTenantId(1L); // 系统租户
        superAdminRole.setCreateTime(LocalDateTime.now());
        superAdminRole.setUpdateTime(LocalDateTime.now());
        
        try {
            // 先查询是否存在
            SysRole existingRole = roleService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, "SUPER_ADMIN"));
            if (existingRole != null) {
                // 存在则更新
                superAdminRole.setId(existingRole.getId());
            }
            roleService.saveOrUpdate(superAdminRole);
        } catch (Exception e) {
            log.warn("创建超级管理员角色失败: {}", e.getMessage());
        }

        // 创建系统管理员角色
        SysRole systemAdminRole = new SysRole();
        systemAdminRole.setRoleName("系统管理员");
        systemAdminRole.setRoleCode("SYSTEM_ADMIN");
        systemAdminRole.setRoleType(0); // 系统角色
        systemAdminRole.setDataScope(0); // 全部数据权限
        systemAdminRole.setStatus(0); // 启用
        systemAdminRole.setRemark("系统管理角色");
        systemAdminRole.setTenantId(1L); // 系统租户
        systemAdminRole.setCreateTime(LocalDateTime.now());
        systemAdminRole.setUpdateTime(LocalDateTime.now());
        
        try {
            // 先查询是否存在
            SysRole existingRole = roleService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, "SYSTEM_ADMIN"));
            if (existingRole != null) {
                // 存在则更新
                systemAdminRole.setId(existingRole.getId());
            }
            roleService.saveOrUpdate(systemAdminRole);
        } catch (Exception e) {
            log.warn("创建系统管理员角色失败: {}", e.getMessage());
        }

        // 创建部门管理员角色
        SysRole deptAdminRole = new SysRole();
        deptAdminRole.setRoleName("部门管理员");
        deptAdminRole.setRoleCode("DEPT_ADMIN");
        deptAdminRole.setRoleType(0); // 系统角色
        deptAdminRole.setDataScope(2); // 本部门及以下
        deptAdminRole.setStatus(0); // 启用
        deptAdminRole.setRemark("部门管理角色");
        deptAdminRole.setTenantId(1L); // 系统租户
        deptAdminRole.setCreateTime(LocalDateTime.now());
        deptAdminRole.setUpdateTime(LocalDateTime.now());
        
        try {
            // 先查询是否存在
            SysRole existingRole = roleService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, "DEPT_ADMIN"));
            if (existingRole != null) {
                // 存在则更新
                deptAdminRole.setId(existingRole.getId());
            }
            roleService.saveOrUpdate(deptAdminRole);
        } catch (Exception e) {
            log.warn("创建部门管理员角色失败: {}", e.getMessage());
        }

        log.info("系统角色初始化完成");
    }

    /**
     * 初始化系统权限
     */
    private void initSystemPermissions() {
        log.info("初始化系统权限...");
        
        // 创建系统管理相关权限
        List<SysPermission> systemPermissions = Arrays.asList(
            createPermission("系统管理", "system:manage", 1, "/system", null, null, 0),
            createPermission("用户管理", "user:manage", 1, "/user", null, null, 1),
            createPermission("用户查询", "user:list", 3, null, "/api/user/page", "GET", 2),
            createPermission("用户创建", "user:create", 3, null, "/api/user", "POST", 3),
            createPermission("用户更新", "user:update", 3, null, "/api/user/*", "PUT", 4),
            createPermission("用户删除", "user:delete", 3, null, "/api/user/*", "DELETE", 5),
            createPermission("角色管理", "role:manage", 1, "/role", null, null, 6),
            createPermission("权限管理", "permission:manage", 1, "/permission", null, null, 7),
            createPermission("角色查询", "role:list", 3, null, "/api/role/page", "GET", 8),
            createPermission("角色创建", "role:create", 3, null, "/api/role", "POST", 9),
            createPermission("角色更新", "role:update", 3, null, "/api/role/*", "PUT", 10),
            createPermission("角色删除", "role:delete", 3, null, "/api/role/*", "DELETE", 11),
            createPermission("权限查询", "permission:list", 3, null, "/api/permission/page", "GET", 12),
            createPermission("权限创建", "permission:create", 3, null, "/api/permission", "POST", 13),
            createPermission("权限更新", "permission:update", 3, null, "/api/permission/*", "PUT", 14),
            createPermission("权限删除", "permission:delete", 3, null, "/api/permission/*", "DELETE", 15)
        );

        // 保存权限
        for (SysPermission permission : systemPermissions) {
            try {
                // 先查询是否存在
                SysPermission existingPermission = permissionService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermissionCode, permission.getPermissionCode()));
                if (existingPermission != null) {
                    // 存在则更新
                    permission.setId(existingPermission.getId());
                }
                permissionService.saveOrUpdate(permission);
            } catch (Exception e) {
                log.warn("创建权限失败: code={}, error={}", permission.getPermissionCode(), e.getMessage());
            }
        }

        log.info("系统权限初始化完成，共{}项", systemPermissions.size());
    }

    /**
     * 初始化权限模板
     */
    private void initPermissionTemplates() {
        log.info("初始化权限模板...");
        
        try {
            // 创建管理员权限模板（使用 saveOrUpdate 避免重复）
            PermissionTemplate adminTemplate = new PermissionTemplate();
            adminTemplate.setTemplateName("管理员权限模板");
            adminTemplate.setTemplateCode("ADMIN_TEMPLATE");
            adminTemplate.setDescription("包含系统管理权限的模板");
            adminTemplate.setTemplateType(1);
            adminTemplate.setStatus(0);
            adminTemplate.setTenantId(1L);
            adminTemplate.setPermissionConfig(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L));
            adminTemplate.setCreateTime(LocalDateTime.now());
            adminTemplate.setUpdateTime(LocalDateTime.now());
            
            PermissionTemplate existingAdmin = templateService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PermissionTemplate>()
                    .eq(PermissionTemplate::getTemplateCode, "ADMIN_TEMPLATE"));
            if (existingAdmin != null) {
                adminTemplate.setId(existingAdmin.getId());
            }
            templateService.saveOrUpdate(adminTemplate);

            // 创建普通用户权限模板（使用 saveOrUpdate 避免重复）
            PermissionTemplate userTemplate = new PermissionTemplate();
            userTemplate.setTemplateName("普通用户权限模板");
            userTemplate.setTemplateCode("USER_TEMPLATE");
            userTemplate.setDescription("包含基本用户权限的模板");
            userTemplate.setTemplateType(1);
            userTemplate.setStatus(0);
            userTemplate.setTenantId(1L);
            userTemplate.setPermissionConfig(Arrays.asList(1L, 2L, 3L));
            userTemplate.setCreateTime(LocalDateTime.now());
            userTemplate.setUpdateTime(LocalDateTime.now());
            
            PermissionTemplate existingUser = templateService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PermissionTemplate>()
                    .eq(PermissionTemplate::getTemplateCode, "USER_TEMPLATE"));
            if (existingUser != null) {
                userTemplate.setId(existingUser.getId());
            }
            templateService.saveOrUpdate(userTemplate);

            log.info("权限模板初始化完成");
        } catch (Exception e) {
            log.error("初始化权限模板失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建权限实体的辅助方法
     */
    private SysPermission createPermission(String name, String code, Integer type, String path, 
                                          String apiPath, String method, Integer sort) {
        SysPermission permission = new SysPermission();
        permission.setPermissionName(name);
        permission.setPermissionCode(code);
        permission.setPermissionType(type); // 1-菜单 2-按钮 3-API
        permission.setPath(path);
        permission.setApiPath(apiPath);
        permission.setMethod(method);
        permission.setSort(sort);
        permission.setVisible(1); // 显示
        permission.setStatus(0); // 启用
        permission.setTenantId(1L); // 系统租户
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        permission.setParentId(0L); // 根节点
        
        return permission;
    }
}