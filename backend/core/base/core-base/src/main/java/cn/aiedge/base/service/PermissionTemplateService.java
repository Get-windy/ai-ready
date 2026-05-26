package cn.aiedge.base.service;

import cn.aiedge.base.entity.PermissionTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 权限模板服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PermissionTemplateService extends IService<PermissionTemplate> {

    /**
     * 创建权限模板
     */
    Long createTemplate(PermissionTemplate template);

    /**
     * 更新权限模板
     */
    void updateTemplate(PermissionTemplate template);

    /**
     * 删除权限模板
     */
    void deleteTemplate(Long templateId);

    /**
     * 分页查询权限模板
     */
    Page<PermissionTemplate> pageTemplates(Page<PermissionTemplate> page, Long tenantId, 
                                           String templateName, Integer templateType, Integer status);

    /**
     * 获取权限模板详情
     */
    PermissionTemplate getTemplateDetail(Long templateId);

    /**
     * 应用权限模板到角色
     */
    void applyTemplateToRole(Long templateId, Long roleId);

    /**
     * 应用权限模板到用户
     */
    void applyTemplateToUser(Long templateId, Long userId);

    /**
     * 获取系统默认模板
     */
    List<PermissionTemplate> getSystemTemplates();

    /**
     * 启用/禁用模板
     */
    void updateTemplateStatus(Long templateId, Integer status);
}