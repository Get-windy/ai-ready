package cn.aiedge.tenant.service;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.entity.SysUserTenant;
import cn.aiedge.base.mapper.SysRoleMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.mapper.SysUserTenantMapper;
import cn.aiedge.base.entity.PermissionTemplate;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.security.StpInterfaceImpl;
import cn.aiedge.base.service.PermissionTemplateService;
import cn.aiedge.base.util.PasswordPolicy;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.tenant.dto.TenantRegisterDTO;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户注册审批服务
 * <p>
 * 处理租户自助注册、系统管理员审批、租户初始化等业务流程。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserTenantMapper sysUserTenantMapper;
    private final StpInterfaceImpl stpInterface;
    private final PermissionTemplateService permissionTemplateService;

    /**
     * 租户审核状态常量
     */
    public static class AuditStatus {
        /** 待审核 */
        public static final Integer PENDING = 1;
        /** 已通过 */
        public static final Integer APPROVED = 2;
        /** 已驳回 */
        public static final Integer REJECTED = 3;

        private AuditStatus() {}
    }

    /**
     * 租户自助注册
     * <p>
     * 创建租户记录（待审核状态），同时创建管理员用户（禁用状态），
     * 待审批通过后自动启用。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SysTenant register(TenantRegisterDTO.Register dto) {
        // 1. 校验租户编码唯一性
        SysTenant existing = tenantMapper.selectByTenantCode(dto.tenantCode());
        if (existing != null) {
            throw BusinessException.badRequest("租户编码已被占用");
        }

        // 2. 校验租户名称唯一性
        existing = tenantMapper.selectByTenantName(dto.tenantName());
        if (existing != null) {
            throw BusinessException.badRequest("租户名称已被占用");
        }

        // 3. 校验管理员用户名唯一性（全局）
        SysUser adminUser = sysUserMapper.selectByUsername(dto.adminUsername(), null);
        if (adminUser != null) {
            throw BusinessException.badRequest("管理员用户名已被占用");
        }

        // 4. 校验管理员邮箱唯一性（全局）
        List<SysUser> emailUsers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, dto.adminEmail())
                        .last("LIMIT 1")
        );
        if (!emailUsers.isEmpty()) {
            throw BusinessException.badRequest("管理员邮箱已被占用");
        }

        // 5. 校验管理员密码复杂度
        String passwordError = PasswordPolicy.validate(dto.adminPassword());
        if (passwordError != null) {
            throw BusinessException.badRequest("管理员" + passwordError
                    + "（" + PasswordPolicy.getStrengthDescription() + "）");
        }

        // 6. 创建租户（待审核，status=0 表示未启用，待审批后置为 1）
        SysTenant tenant = new SysTenant()
                .setTenantName(dto.tenantName())
                .setTenantCode(dto.tenantCode())
                .setContactPerson(dto.contactPerson())
                .setContactPhone(dto.contactPhone())
                .setContactEmail(dto.contactEmail())
                .setLevel("basic")                            // 默认基础版
                .setStatus(0)                                  // 暂未启用（待审批）
                .setDeleted(0);
        tenantMapper.insert(tenant);

        // 6. 创建管理员用户（禁用状态，审批后启用）
        SysUser user = new SysUser()
                .setTenantId(tenant.getId())
                .setUsername(dto.adminUsername())
                .setPassword(BCrypt.hashpw(dto.adminPassword(), BCrypt.gensalt()))
                .setNickname(dto.contactPerson())
                .setEmail(dto.adminEmail())
                .setUserType(1)                                // 企业用户
                .setStatus(0)                                  // 禁用（待审批后启用）
                .setDeleted(0)
                .setLoginCount(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        // 7. 关联用户与租户
        SysUserTenant userTenant = new SysUserTenant()
                .setUserId(user.getId())
                .setTenantId(tenant.getId())
                .setIsDefault(true)
                .setStatus(1)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sysUserTenantMapper.insert(userTenant);

        // 8. 更新 tenant.adminUserId
        tenant.setAdminUserId(user.getId());
        tenantMapper.updateById(tenant);

        log.info("租户注册成功: tenantId={}, tenantName={}, adminUserId={}",
                tenant.getId(), dto.tenantName(), user.getId());

        return tenant;
    }

    /**
     * 审批通过租户
     * <p>
     * 启用租户、启用管理员用户、创建默认角色、分配基础权限。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long tenantId, TenantRegisterDTO.Approve dto) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() == 1) {
            throw BusinessException.notFound("租户不存在");
        }

        // 1. 启用租户
        tenant.setStatus(1);
        if (dto != null && dto.remark() != null) {
            tenant.setRemark(dto.remark());
        }
        tenantMapper.updateById(tenant);

        // 2. 启用管理员用户并标记为租户管理员
        if (tenant.getAdminUserId() != null) {
            SysUser admin = sysUserMapper.selectById(tenant.getAdminUserId());
            if (admin != null) {
                admin.setStatus(1);
                admin.setIsTenantAdmin(true);
                sysUserMapper.updateById(admin);
            }
        }

        // 3. 创建默认租户管理员角色
        SysRole adminRole = createDefaultAdminRole(tenantId);

        // 4. 为管理员用户分配角色
        if (tenant.getAdminUserId() != null && adminRole != null) {
            SysUserRole userRole = new SysUserRole()
                    .setUserId(tenant.getAdminUserId())
                    .setRoleId(adminRole.getId())
                    .setTenantId(tenantId);
            sysUserRoleMapper.insert(userRole);
        }

        // 5. 从系统默认权限模板初始化租户管理员角色权限
        if (adminRole != null) {
            List<PermissionTemplate> systemTemplates = permissionTemplateService.getSystemTemplates();
            if (!systemTemplates.isEmpty()) {
                permissionTemplateService.applyTemplateToRole(systemTemplates.get(0).getId(), adminRole.getId());
                log.info("已从权限模板 '{}' 初始化租户管理员角色权限: roleId={}",
                        systemTemplates.get(0).getTemplateName(), adminRole.getId());
            } else {
                log.warn("未找到系统默认权限模板，租户管理员角色权限为空: roleId={}", adminRole.getId());
            }
        }

        // 6. 清除管理员用户的权限缓存
        if (tenant.getAdminUserId() != null) {
            stpInterface.clearUserPermissionCache(tenant.getAdminUserId());
        }

        log.info("租户审批通过: tenantId={}, tenantName={}", tenantId, tenant.getTenantName());
    }

    /**
     * 驳回租户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long tenantId, TenantRegisterDTO.Reject dto) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() == 1) {
            throw BusinessException.notFound("租户不存在");
        }

        tenant.setRemark(dto.reason());
        tenantMapper.updateById(tenant);

        // 删除关联的管理员用户
        if (tenant.getAdminUserId() != null) {
            sysUserMapper.deleteById(tenant.getAdminUserId());
            // 删除用户-租户关联
            sysUserTenantMapper.delete(
                    new LambdaQueryWrapper<SysUserTenant>()
                            .eq(SysUserTenant::getUserId, tenant.getAdminUserId())
            );
            tenant.setAdminUserId(null);
            tenantMapper.updateById(tenant);
        }

        // 物理删除租户记录（注册未通过不保留）
        tenantMapper.deleteById(tenantId);

        log.info("租户注册已驳回: tenantId={}, tenantName={}, reason={}",
                tenantId, tenant.getTenantName(), dto.reason());
    }

    /**
     * 查询待审核租户列表
     */
    public List<SysTenant> getPendingTenants() {
        return tenantMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getStatus, 0)
                        .eq(SysTenant::getDeleted, 0)
                        .isNotNull(SysTenant::getAdminUserId)
                        .orderByDesc(SysTenant::getCreateTime)
        );
    }

    // ========== 内部辅助方法 ==========

    /**
     * 创建租户默认管理员角色
     */
    private SysRole createDefaultAdminRole(Long tenantId) {
        // 检查是否已存在
        SysRole existing = sysRoleMapper.selectByRoleCode("TENANT_ADMIN", tenantId);
        if (existing != null) {
            return existing;
        }

        SysRole role = new SysRole()
                .setTenantId(tenantId)
                .setRoleName("租户管理员")
                .setRoleCode("TENANT_ADMIN")
                .setRoleType(1)      // 自定义角色
                .setScope("TENANT")   // 租户级
                .setDataScope(0)      // 全部数据
                .setSort(1)
                .setStatus(0)         // 正常
                .setRemark("系统自动创建 - 租户注册审批通过")
                .setDeleted(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sysRoleMapper.insert(role);
        return role;
    }

    /**
     * 检查租户是否存在且已通过审批
     */
    public boolean isTenantApproved(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant != null && tenant.getDeleted() == 0 && tenant.getStatus() == 1;
    }
}
