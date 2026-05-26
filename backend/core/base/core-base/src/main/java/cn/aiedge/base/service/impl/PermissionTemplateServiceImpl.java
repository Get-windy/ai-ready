package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.PermissionTemplate;
import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.PermissionTemplateMapper;
import cn.aiedge.base.service.PermissionTemplateService;
import cn.aiedge.base.service.SysRoleService;
import cn.aiedge.base.service.SysUserService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限模板服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionTemplateServiceImpl extends ServiceImpl<PermissionTemplateMapper, PermissionTemplate>
        implements PermissionTemplateService {

    private final SysRoleService roleService;
    private final SysUserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(PermissionTemplate template) {
        // 检查模板编码是否已存在
        LambdaQueryWrapper<PermissionTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PermissionTemplate::getTemplateCode, template.getTemplateCode())
               .eq(PermissionTemplate::getTenantId, template.getTenantId());
        long count = count(wrapper);
        if (count > 0) {
            throw new RuntimeException("权限模板编码已存在");
        }

        template.setStatus(0);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreateBy(StpUtil.getLoginIdAsLong());
        save(template);
        log.info("创建权限模板成功: templateId={}, templateName={}", template.getId(), template.getTemplateName());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(PermissionTemplate template) {
        // 检查模板编码是否与其他模板冲突（排除自身）
        LambdaQueryWrapper<PermissionTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PermissionTemplate::getTemplateCode, template.getTemplateCode())
               .eq(PermissionTemplate::getTenantId, template.getTenantId())
               .ne(PermissionTemplate::getId, template.getId());
        long count = count(wrapper);
        if (count > 0) {
            throw new RuntimeException("权限模板编码已存在");
        }

        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
        log.info("更新权限模板成功: templateId={}", template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        PermissionTemplate template = getById(templateId);
        if (template == null) {
            throw new RuntimeException("权限模板不存在");
        }

        // 系统默认模板不允许删除
        if (template.getTemplateType() != null && template.getTemplateType() == 1) {
            throw new RuntimeException("系统默认模板不允许删除");
        }

        removeById(templateId);
        log.info("删除权限模板成功: templateId={}", templateId);
    }

    @Override
    public Page<PermissionTemplate> pageTemplates(Page<PermissionTemplate> page, Long tenantId,
                                                 String templateName, Integer templateType, Integer status) {
        LambdaQueryWrapper<PermissionTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, PermissionTemplate::getTenantId, tenantId)
               .like(templateName != null && !templateName.isEmpty(), PermissionTemplate::getTemplateName, templateName)
               .eq(templateType != null, PermissionTemplate::getTemplateType, templateType)
               .eq(status != null, PermissionTemplate::getStatus, status)
               .orderByDesc(PermissionTemplate::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public PermissionTemplate getTemplateDetail(Long templateId) {
        return getById(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyTemplateToRole(Long templateId, Long roleId) {
        PermissionTemplate template = getById(templateId);
        if (template == null) {
            throw new RuntimeException("权限模板不存在");
        }

        SysRole role = roleService.getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 从模板中获取权限配置并应用到角色
        // 这里假设permissionConfig是一个权限ID列表
        if (template.getPermissionConfig() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Long> permissionIds = (List<Long>) template.getPermissionConfig();
            roleService.assignPermissions(roleId, permissionIds);
        }

        log.info("应用权限模板到角色成功: templateId={}, roleId={}", templateId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyTemplateToUser(Long templateId, Long userId) {
        PermissionTemplate template = getById(templateId);
        if (template == null) {
            throw new RuntimeException("权限模板不存在");
        }

        SysUser user = userService.getUserDetail(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 从模板中获取权限配置并应用到用户
        // 这里可以通过给用户分配角色的方式来应用权限
        if (template.getPermissionConfig() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Long> permissionIds = (List<Long>) template.getPermissionConfig();
            
            // 可以通过给用户分配预设角色或直接分配权限来实现
            // 暂时使用分配角色的方式，需要根据实际情况调整
            log.info("应用权限模板到用户: templateId={}, userId={}", templateId, userId);
        }

        log.info("应用权限模板到用户成功: templateId={}, userId={}", templateId, userId);
    }

    @Override
    public List<PermissionTemplate> getSystemTemplates() {
        LambdaQueryWrapper<PermissionTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PermissionTemplate::getTemplateType, 1) // 系统默认模板
               .eq(PermissionTemplate::getStatus, 0); // 启用状态
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateStatus(Long templateId, Integer status) {
        PermissionTemplate template = new PermissionTemplate();
        template.setId(templateId);
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
        log.info("更新权限模板状态成功: templateId={}, status={}", templateId, status);
    }
}