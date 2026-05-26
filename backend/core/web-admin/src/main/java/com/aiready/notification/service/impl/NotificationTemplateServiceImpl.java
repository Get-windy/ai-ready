package com.aiready.notification.service.impl;

import com.aiready.notification.dto.NotificationTemplateDTO;
import com.aiready.notification.entity.NotificationTemplate;
import com.aiready.notification.mapper.NotificationTemplateMapper;
import com.aiready.notification.service.NotificationTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl extends ServiceImpl<NotificationTemplateMapper, NotificationTemplate> 
        implements NotificationTemplateService {

    private final NotificationTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public NotificationTemplate getByCode(String templateCode) {
        return templateMapper.selectByCode(templateCode);
    }

    @Override
    public String renderTemplate(String templateContent, Map<String, Object> params) {
        if (!StringUtils.hasText(templateContent) || params == null) {
            return templateContent;
        }
        
        Matcher matcher = TEMPLATE_PATTERN.matcher(templateContent);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            if (value != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    @Override
    public IPage<NotificationTemplateDTO> getTemplateList(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getDeleted, 0);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(NotificationTemplate::getTemplateName, keyword)
                    .or()
                    .like(NotificationTemplate::getTemplateCode, keyword));
        }
        
        wrapper.orderByDesc(NotificationTemplate::getCreateTime);
        IPage<NotificationTemplate> templatePage = page(new Page<>(page, size), wrapper);
        return templatePage.convert(this::convertToDTO);
    }

    @Override
    public void createTemplate(NotificationTemplateDTO templateDTO) {
        NotificationTemplate template = new NotificationTemplate();
        BeanUtils.copyProperties(templateDTO, template);
        
        if (templateDTO.getParams() != null) {
            try {
                template.setParams(objectMapper.writeValueAsString(templateDTO.getParams()));
            } catch (Exception e) {
                log.error("序列化模板参数失败", e);
            }
        }
        
        save(template);
    }

    @Override
    public void updateTemplate(Long id, NotificationTemplateDTO templateDTO) {
        NotificationTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        
        BeanUtils.copyProperties(templateDTO, template);
        template.setId(id);
        
        if (templateDTO.getParams() != null) {
            try {
                template.setParams(objectMapper.writeValueAsString(templateDTO.getParams()));
            } catch (Exception e) {
                log.error("序列化模板参数失败", e);
            }
        }
        
        updateById(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        removeById(id);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        NotificationTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        template.setStatus(status);
        updateById(template);
    }

    @Override
    public List<NotificationTemplateDTO> getActiveTemplates() {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getStatus, 1)
               .eq(NotificationTemplate::getDeleted, 0)
               .orderByDesc(NotificationTemplate::getCreateTime);
        
        List<NotificationTemplate> templates = list(wrapper);
        return templates.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private NotificationTemplateDTO convertToDTO(NotificationTemplate template) {
        NotificationTemplateDTO dto = new NotificationTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        
        if (StringUtils.hasText(template.getParams())) {
            try {
                List<String> params = objectMapper.readValue(template.getParams(), new TypeReference<List<String>>() {});
                dto.setParams(params);
            } catch (Exception e) {
                log.error("解析模板参数失败", e);
            }
        }
        
        return dto;
    }
}
