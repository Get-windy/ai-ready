package com.aiready.notification.service;

import com.aiready.notification.dto.NotificationTemplateDTO;
import com.aiready.notification.entity.NotificationTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 消息模板服务接口
 */
public interface NotificationTemplateService extends IService<NotificationTemplate> {

    /**
     * 根据模板编码获取模板
     */
    NotificationTemplate getByCode(String templateCode);

    /**
     * 渲染模板内容
     */
    String renderTemplate(String templateContent, Map<String, Object> params);

    /**
     * 获取模板列表
     */
    IPage<NotificationTemplateDTO> getTemplateList(Integer page, Integer size, String keyword);

    /**
     * 创建模板
     */
    void createTemplate(NotificationTemplateDTO templateDTO);

    /**
     * 更新模板
     */
    void updateTemplate(Long id, NotificationTemplateDTO templateDTO);

    /**
     * 删除模板
     */
    void deleteTemplate(Long id);

    /**
     * 启用/禁用模板
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 获取所有启用的模板
     */
    List<NotificationTemplateDTO> getActiveTemplates();
}
