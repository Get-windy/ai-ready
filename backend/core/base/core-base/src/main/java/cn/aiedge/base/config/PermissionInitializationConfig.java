package cn.aiedge.base.config;

import cn.aiedge.base.entity.*;
import cn.aiedge.base.mapper.SysRolePermissionMapper;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.base.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SecurityContext securityContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化权限配置...");

        // 设置临时租户ID=1，确保初始化期间所有查询都能正确匹配系统租户的数据
        securityContext.setTempTenantId(1L);
        try {
            // 初始化系统角色
            initSystemRoles();

            // 初始化系统权限
            initSystemPermissions();

            // 初始化权限模板
            initPermissionTemplates();

            // 为超级管理员分配所有权限
            assignAllPermissionsToSuperAdmin();
        } finally {
            securityContext.clearTempTenantId();
        }

        log.info("权限配置初始化完成");
    }

    /**
     * 为超级管理员角色分配所有现有权限
     */
    private void assignAllPermissionsToSuperAdmin() {
        try {
            // 查找超级管理员角色
            SysRole superAdminRole = roleService.getOne(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleCode, "SUPER_ADMIN"));
            if (superAdminRole == null) {
                log.warn("未找到超级管理员角色，跳过权限分配");
                return;
            }

            // 获取所有权限
            List<SysPermission> allPermissions = permissionService.list();
            if (allPermissions == null || allPermissions.isEmpty()) {
                log.warn("权限列表为空，跳过权限分配");
                return;
            }

            // 删除旧的关联
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, superAdminRole.getId()));

            // 批量插入新的角色-权限关联
            List<SysRolePermission> rolePermissions = allPermissions.stream()
                    .map(p -> new SysRolePermission()
                            .setRoleId(superAdminRole.getId())
                            .setPermissionId(p.getId())
                            .setTenantId(1L))
                    .collect(Collectors.toList());

            rolePermissions.forEach(rp -> rolePermissionMapper.insert(rp));
            log.info("为超级管理员角色分配了 {} 个权限", rolePermissions.size());
        } catch (Exception e) {
            log.error("分配超级管理员权限失败: {}", e.getMessage());
        }
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
        savePermissions(systemPermissions);

        // 创建ERP业务权限
        List<SysPermission> erpPermissions = Arrays.asList(
            createPermission("采购管理", "purchase:manage", 1, "/erp/purchase", null, null, 16),
            createPermission("采购订单查询", "purchase:order:list", 3, null, "/api/erp/purchase/order/page", "GET", 17),
            createPermission("采购订单创建", "purchase:order:create", 3, null, "/api/erp/purchase/order", "POST", 18),
            createPermission("采购订单更新", "purchase:order:update", 3, null, "/api/erp/purchase/order/*", "PUT", 19),
            createPermission("采购订单删除", "purchase:order:delete", 3, null, "/api/erp/purchase/order/*", "DELETE", 20),
            createPermission("采购订单详情", "purchase:order:detail", 3, null, "/api/erp/purchase/order/*", "GET", 21),
            createPermission("采购订单提交", "purchase:order:submit", 3, null, "/api/erp/purchase/order/*/submit", "POST", 22),
            createPermission("采购订单审批", "purchase:order:approve", 3, null, "/api/erp/purchase/order/*/approve", "POST", 23),
            createPermission("采购订单取消", "purchase:order:cancel", 3, null, "/api/erp/purchase/order/*/cancel", "POST", 24),
            createPermission("销售管理", "sale:manage", 1, "/erp/sale", null, null, 25),
            createPermission("销售订单创建", "sale:order:create", 3, null, "/api/erp/sale/order", "POST", 26),
            createPermission("销售订单更新", "sale:order:update", 3, null, "/api/erp/sale/order/*", "PUT", 27),
            createPermission("销售订单删除", "sale:order:delete", 3, null, "/api/erp/sale/order/*", "DELETE", 28),
            createPermission("销售订单提交", "sale:order:submit", 3, null, "/api/erp/sale/order/*/submit", "POST", 29),
            createPermission("销售订单审批", "sale:order:approve", 3, null, "/api/erp/sale/order/*/approve", "POST", 30),
            createPermission("销售订单出库", "sale:order:ship", 3, null, "/api/erp/sale/order/*/ship", "POST", 31),
            createPermission("销售订单收款", "sale:order:payment", 3, null, "/api/erp/sale/order/*/payment", "POST", 32),
            createPermission("销售订单取消", "sale:order:cancel", 3, null, "/api/erp/sale/order/*/cancel", "POST", 33),
            createPermission("库存管理", "stock:manage", 1, "/erp/stock", null, null, 34),
            createPermission("库存查看", "stock:list", 3, null, "/api/erp/stock/page", "GET", 35),
            createPermission("CRM客户管理", "crm:manage", 1, "/crm", null, null, 36),
            createPermission("客户查询", "crm:customer:list", 3, null, "/api/customer/page", "GET", 37),
            createPermission("客户创建", "crm:customer:create", 3, null, "/api/customer", "POST", 38),
            createPermission("客户更新", "crm:customer:update", 3, null, "/api/customer/*", "PUT", 39),
            createPermission("客户删除", "crm:customer:delete", 3, null, "/api/customer/*", "DELETE", 40)
        );
        savePermissions(erpPermissions);

        log.info("系统权限初始化完成，共{}项", systemPermissions.size() + erpPermissions.size());
    }

    /**
     * 保存权限列表（查询存在则更新，否则插入）
     */
    private void savePermissions(List<SysPermission> permissions) {
        for (SysPermission permission : permissions) {
            try {
                SysPermission existing = permissionService.getOne(
                    new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermissionCode, permission.getPermissionCode()));
                if (existing != null) {
                    permission.setId(existing.getId());
                }
                permissionService.saveOrUpdate(permission);
            } catch (Exception e) {
                log.warn("创建权限失败: code={}, error={}", permission.getPermissionCode(), e.getMessage());
            }
        }
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