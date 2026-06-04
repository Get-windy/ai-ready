package com.aiready.notification.controller;

import com.aiready.notification.dto.NotificationTemplateDTO;
import com.aiready.notification.service.NotificationTemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-template")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @GetMapping("/list")
    public IPage<NotificationTemplateDTO> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String keyword) {
        return templateService.getTemplateList(page, size, keyword);
    }

    @GetMapping("/active")
    public List<NotificationTemplateDTO> getActiveTemplates() {
        return templateService.getActiveTemplates();
    }

    @GetMapping("/{id}")
    public NotificationTemplateDTO getById(@PathVariable Long id) {
        return convertToDTO(templateService.getById(id));
    }

    @GetMapping("/code/{templateCode}")
    public NotificationTemplateDTO getByCode(@PathVariable String templateCode) {
        return convertToDTO(templateService.getByCode(templateCode));
    }

    @PostMapping
    public void create(@RequestBody NotificationTemplateDTO templateDTO) {
        templateService.createTemplate(templateDTO);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody NotificationTemplateDTO templateDTO) {
        templateService.updateTemplate(id, templateDTO);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
    }

    @DeleteMapping("/batch")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return templateService.removeByIds(ids);
    }

    @GetMapping("/export")
    public List<com.aiready.notification.entity.NotificationTemplate> export() {
        return templateService.list();
    }

    @PostMapping("/{id}/toggle")
    public void toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        templateService.toggleStatus(id, status);
    }

    private NotificationTemplateDTO convertToDTO(com.aiready.notification.entity.NotificationTemplate template) {
        if (template == null) return null;
        NotificationTemplateDTO dto = new NotificationTemplateDTO();
        org.springframework.beans.BeanUtils.copyProperties(template, dto);
        return dto;
    }
}
