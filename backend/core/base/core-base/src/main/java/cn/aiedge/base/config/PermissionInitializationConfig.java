package cn.aiedge.base.config;

import cn.aiedge.base.entity.*;
import cn.aiedge.base.mapper.SysRoleBillTypeMapper;
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
    private final RoleBillTypeService roleBillTypeService;

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

            // 为超级管理员分配所有单据类型权限
            assignBillTypesToSuperAdmin();
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
     * 为超级管理员角色分配所有单据类型权限（级别3=审核）
     */
    private void assignBillTypesToSuperAdmin() {
        try {
            SysRole superAdminRole = roleService.getOne(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleCode, "SUPER_ADMIN"));
            if (superAdminRole == null) {
                log.warn("未找到超级管理员角色，跳过单据类型权限分配");
                return;
            }

            // 定义所有单据类型及最高权限级别
            List<cn.aiedge.base.service.RoleBillTypeService.BillTypeAssignment> allBillTypes = List.of(
                    new cn.aiedge.base.service.RoleBillTypeService.BillTypeAssignment("504", 3), // 采购订单
                    new cn.aiedge.base.service.RoleBillTypeService.BillTypeAssignment("601", 3), // 销售出库
                    new cn.aiedge.base.service.RoleBillTypeService.BillTypeAssignment("604", 3), // 销售订单
                    new cn.aiedge.base.service.RoleBillTypeService.BillTypeAssignment("801", 3), // 收款单
                    new cn.aiedge.base.service.RoleBillTypeService.BillTypeAssignment("802", 3)  // 付款单
            );

            roleBillTypeService.assignBillTypes(superAdminRole.getId(), allBillTypes);
            log.info("已为超级管理员角色分配 {} 个单据类型权限", allBillTypes.size());
        } catch (Exception e) {
            log.error("分配超级管理员单据类型权限失败: {}", e.getMessage());
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

        // 创建岗位管理权限
        List<SysPermission> positionPermissions = Arrays.asList(
            createPermission("岗位管理", "position:manage", 1, "/position", null, null, 41),
            createPermission("岗位查询", "position:list", 3, null, "/api/position/page", "GET", 42),
            createPermission("岗位创建", "position:create", 3, null, "/api/position", "POST", 43),
            createPermission("岗位更新", "position:edit", 3, null, "/api/position/*", "PUT", 44),
            createPermission("岗位删除", "position:delete", 3, null, "/api/position/*", "DELETE", 45),
            createPermission("岗位详情", "position:query", 3, null, "/api/position/*", "GET", 46),
            createPermission("岗位分配", "position:assign", 3, null, "/api/position/assign", "POST", 47)
        );
        savePermissions(positionPermissions);

        // 创建部门管理权限
        List<SysPermission> departmentPermissions = Arrays.asList(
            createPermission("部门管理", "department:manage", 1, "/department", null, null, 48),
            createPermission("部门查询", "department:list", 3, null, "/api/department/page", "GET", 49),
            createPermission("部门创建", "department:create", 3, null, "/api/department", "POST", 50),
            createPermission("部门更新", "department:edit", 3, null, "/api/department/*", "PUT", 51),
            createPermission("部门删除", "department:delete", 3, null, "/api/department/*", "DELETE", 52),
            createPermission("部门详情", "department:query", 3, null, "/api/department/*", "GET", 53)
        );
        savePermissions(departmentPermissions);

        // 创建系统管理权限（统一使用 system: 前缀，匹配 @SaCheckPermission 注解）
        List<SysPermission> systemMgmtPermissions = Arrays.asList(
            createPermission("用户列表", "system:user:list", 3, null, "/api/v2/user/page", "GET", 54),
            createPermission("用户详情", "system:user:detail", 3, null, "/api/v2/user/{id}", "GET", 55),
            createPermission("用户创建", "system:user:create", 3, null, "/api/v2/user", "POST", 56),
            createPermission("用户更新", "system:user:update", 3, null, "/api/v2/user", "PUT", 57),
            createPermission("用户删除", "system:user:delete", 3, null, "/api/v2/user/{id}", "DELETE", 58),
            createPermission("用户导出", "system:user:export", 3, null, "/api/v2/user/export", "GET", 59),
            createPermission("用户角色分配", "system:role:assign", 3, null, "/api/v2/user/{id}/roles", "POST", 60),
            createPermission("角色列表", "system:role:list", 3, null, "/api/role/page", "GET", 61),
            createPermission("角色创建", "system:role:create", 3, null, "/api/role", "POST", 62),
            createPermission("角色更新", "system:role:update", 3, null, "/api/role", "PUT", 63),
            createPermission("角色删除", "system:role:delete", 3, null, "/api/role/{id}", "DELETE", 64),
            createPermission("角色导出", "system:role:export", 3, null, "/api/role/export", "GET", 65),
            createPermission("角色权限分配", "system:permission:assign", 3, null, "/api/role/{id}/permissions", "POST", 66),
            createPermission("权限列表", "system:permission:list", 3, null, "/api/permission/page", "GET", 67),
            createPermission("权限详情", "system:permission:detail", 3, null, "/api/permission/{id}", "GET", 68),
            createPermission("权限创建", "system:permission:create", 3, null, "/api/permission", "POST", 69),
            createPermission("权限更新", "system:permission:update", 3, null, "/api/permission", "PUT", 70),
            createPermission("权限删除", "system:permission:delete", 3, null, "/api/permission/{id}", "DELETE", 71),
            createPermission("权限导出", "system:permission:export", 3, null, "/api/permission/export", "GET", 72),
            createPermission("字典创建", "system:dict:create", 3, null, "/api/dict/type", "POST", 73),
            createPermission("字典更新", "system:dict:update", 3, null, "/api/dict/type", "PUT", 74),
            createPermission("字典删除", "system:dict:delete", 3, null, "/api/dict/type/{id}", "DELETE", 75),
            createPermission("字典导出", "system:dict:export", 3, null, "/api/dict/type/export", "GET", 76),
            createPermission("配置列表", "system:config:list", 3, null, "/api/config/list", "GET", 77),
            createPermission("配置更新", "system:config:update", 3, null, "/api/config/save", "POST", 78),
            createPermission("配置删除", "system:config:delete", 3, null, "/api/config/{configKey}", "DELETE", 79),
            createPermission("配置导出", "system:config:export", 3, null, "/api/config/export", "GET", 80),
            createPermission("审计日志创建", "log:audit:create", 3, null, "/api/audit/record", "POST", 81),
            createPermission("审计日志查询", "log:audit:list", 3, null, "/api/audit/query", "GET", 82),
            createPermission("审计日志详情", "log:audit:detail", 3, null, "/api/audit/detail/{logId}", "GET", 83),
            createPermission("审计日志统计", "log:audit:stats", 3, null, "/api/audit/statistics", "GET", 84),
            createPermission("审计日志导出", "log:audit:export", 3, null, "/api/audit/export", "GET", 85),
            createPermission("审计日志删除", "log:audit:delete", 3, null, "/api/audit/clean", "DELETE", 86)
        );
        savePermissions(systemMgmtPermissions);

        // 创建ERP费用报销权限（匹配 FeeApplicationController @RequiresPermission 注解）
        List<SysPermission> expensePermissions = Arrays.asList(
            createPermission("费用申请查询", "erp:expense:application:list", 3, null, "/api/erp/expense/application/page", "GET", 87),
            createPermission("费用申请详情", "erp:expense:application:query", 3, null, "/api/erp/expense/application/{id}", "GET", 88),
            createPermission("费用申请创建", "erp:expense:application:create", 3, null, "/api/erp/expense/application", "POST", 89),
            createPermission("费用申请编辑", "erp:expense:application:edit", 3, null, "/api/erp/expense/application/{id}", "PUT", 90),
            createPermission("费用申请删除", "erp:expense:application:delete", 3, null, "/api/erp/expense/application/{id}", "DELETE", 91),
            createPermission("费用申请提交", "erp:expense:application:submit", 3, null, "/api/erp/expense/application/{id}/submit", "POST", 92),
            createPermission("费用申请审批", "erp:expense:application:approve", 3, null, "/api/erp/expense/application/{id}/approve", "POST", 93),
            createPermission("费用审批处理", "erp:expense:approval:process", 3, null, "/api/erp/expense/approval/process", "POST", 94),
            createPermission("费用审批查询", "erp:expense:approval:query", 3, null, "/api/erp/expense/approval/{id}", "GET", 95),
            createPermission("费用审批列表", "erp:expense:approval:list", 3, null, "/api/erp/expense/approval/page", "GET", 96),
            createPermission("费用付款创建", "erp:expense:payment:create", 3, null, "/api/erp/expense/payment", "POST", 97),
            createPermission("费用付款确认", "erp:expense:payment:confirm", 3, null, "/api/erp/expense/payment/{id}/confirm", "POST", 98),
            createPermission("费用付款取消", "erp:expense:payment:cancel", 3, null, "/api/erp/expense/payment/{id}/cancel", "POST", 99),
            createPermission("费用付款列表", "erp:expense:payment:list", 3, null, "/api/erp/expense/payment/page", "GET", 100),
            createPermission("费用付款详情", "erp:expense:payment:query", 3, null, "/api/erp/expense/payment/{id}", "GET", 101),
            createPermission("报销查询", "erp:expense:reimbursement:list", 3, null, "/api/erp/expense/reimbursement/page", "GET", 102),
            createPermission("报销详情", "erp:expense:reimbursement:query", 3, null, "/api/erp/expense/reimbursement/{id}", "GET", 103),
            createPermission("报销创建", "erp:expense:reimbursement:create", 3, null, "/api/erp/expense/reimbursement", "POST", 104),
            createPermission("报销编辑", "erp:expense:reimbursement:edit", 3, null, "/api/erp/expense/reimbursement/{id}", "PUT", 105),
            createPermission("报销删除", "erp:expense:reimbursement:delete", 3, null, "/api/erp/expense/reimbursement/{id}", "DELETE", 106),
            createPermission("报销提交", "erp:expense:reimbursement:submit", 3, null, "/api/erp/expense/reimbursement/{id}/submit", "POST", 107),
            createPermission("费用统计查询", "erp:expense:statistics:list", 3, null, "/api/erp/expense/statistics", "GET", 108),
            createPermission("费用统计刷新", "erp:expense:statistics:refresh", 3, null, "/api/erp/expense/statistics/refresh", "POST", 109)
        );
        savePermissions(expensePermissions);

        // 创建财务管理权限（匹配 ReceivableController @RequiresPermission 注解）
        List<SysPermission> financePermissions = Arrays.asList(
            createPermission("应收列表", "finance:receivable:list", 3, null, "/api/finance/receivable/page", "GET", 110),
            createPermission("应收详情", "finance:receivable:query", 3, null, "/api/finance/receivable/{id}", "GET", 111),
            createPermission("应收创建", "finance:receivable:create", 3, null, "/api/finance/receivable", "POST", 112),
            createPermission("应收编辑", "finance:receivable:edit", 3, null, "/api/finance/receivable/{id}", "PUT", 113),
            createPermission("应收删除", "finance:receivable:delete", 3, null, "/api/finance/receivable/{id}", "DELETE", 114),
            createPermission("应收收款", "finance:receivable:payment", 3, null, "/api/finance/receivable/{id}/payment", "POST", 115),
            createPermission("应收分析", "finance:receivable:analysis", 3, null, "/api/finance/receivable/analysis", "GET", 116),
            createPermission("应收导出", "finance:receivable:export", 3, null, "/api/finance/receivable/export", "GET", 117)
        );
        savePermissions(financePermissions);

        // 创建旧版权限管理所需权限（匹配 PermissionController @RequirePermission 注解）
        List<SysPermission> legacyPermMgmtPermissions = Arrays.asList(
            createPermission("查看用户权限", "system:permission:view", 3, null, "/api/permission/user/{userId}/permissions", "GET", 118),
            createPermission("查看用户角色", "system:role:view", 3, null, "/api/permission/user/{userId}/roles", "GET", 119),
            createPermission("清除用户角色", "system:role:clear", 3, null, "/api/permission/user/{userId}/roles", "DELETE", 120),
            createPermission("清除角色权限", "system:permission:clear", 3, null, "/api/permission/role/{roleId}/permissions", "DELETE", 121),
            createPermission("权限校验", "system:permission:check", 3, null, "/api/permission/check/api", "GET", 122),
            createPermission("管理权限缓存", "system:permission:cache", 3, null, "/api/permission/cache/refresh/{userId}", "POST", 123)
        );
        savePermissions(legacyPermMgmtPermissions);

        // 创建租户管理权限
        List<SysPermission> tenantPermissions = Arrays.asList(
            createPermission("租户列表", "tenant:list", 3, null, "/api/tenant/page", "GET", 124),
            createPermission("租户创建", "tenant:create", 3, null, "/api/tenant", "POST", 125),
            createPermission("租户更新", "tenant:update", 3, null, "/api/tenant/*", "PUT", 126),
            createPermission("租户删除", "tenant:delete", 3, null, "/api/tenant/*", "DELETE", 127),
            createPermission("租户配置", "tenant:config", 3, null, "/api/tenant/*/config", "GET", 128)
        );
        savePermissions(tenantPermissions);

        // 创建数据导入配置权限
        List<SysPermission> dataImportPermissions = Arrays.asList(
            createPermission("导入配置管理", "system:dataimport:manage", 1, "/system/data-import", null, null, 129),
            createPermission("导入配置列表", "system:dataimport:list", 3, null, "/api/v1/sync-config", "GET", 130),
            createPermission("导入配置创建", "system:dataimport:create", 3, null, "/api/v1/sync-config", "POST", 131),
            createPermission("导入配置更新", "system:dataimport:update", 3, null, "/api/v1/sync-config/{id}", "PUT", 132),
            createPermission("导入配置删除", "system:dataimport:delete", 3, null, "/api/v1/sync-config/{id}", "DELETE", 133),
            createPermission("导入配置测试", "system:dataimport:test", 3, null, "/api/v1/sync-config/{id}/test", "POST", 134),
            createPermission("导入配置同步", "system:dataimport:sync", 3, null, "/api/v1/sync-config/{id}/sync", "POST", 135)
        );
        savePermissions(dataImportPermissions);

        log.info("系统权限初始化完成，共{}项",
            systemPermissions.size() + erpPermissions.size() + positionPermissions.size()
            + departmentPermissions.size() + systemMgmtPermissions.size()
            + expensePermissions.size() + financePermissions.size()
            + legacyPermMgmtPermissions.size()
            + tenantPermissions.size()
            + dataImportPermissions.size());
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