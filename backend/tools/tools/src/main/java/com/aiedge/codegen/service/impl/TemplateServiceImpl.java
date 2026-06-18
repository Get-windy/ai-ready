package com.aiedge.codegen.service.impl;

import com.aiedge.codegen.entity.Template;
import com.aiedge.codegen.mapper.TemplateMapper;
import com.aiedge.codegen.service.TemplateService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模板服务实现类
 * 
 * @author AI-Ready
 */
@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {
    
    @Override
    public List<Template> getByType(String type) {
        QueryWrapper<Template> wrapper = new QueryWrapper<>();
        wrapper.eq("type", type);
        wrapper.eq("enabled", true);
        return this.list(wrapper);
    }
    
    @Override
    public Template getByName(String name) {
        QueryWrapper<Template> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        wrapper.eq("enabled", true);
        return this.getOne(wrapper);
    }
    
    @Override
    public Template getByTypeAndName(String type, String name) {
        QueryWrapper<Template> wrapper = new QueryWrapper<>();
        wrapper.eq("type", type);
        wrapper.eq("name", name);
        wrapper.eq("enabled", true);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean activate(Long id) {
        Template template = this.getById(id);
        if (template != null) {
            template.setEnabled(true);
            return this.updateById(template);
        }
        return false;
    }
    
    @Override
    public boolean deactivate(Long id) {
        Template template = this.getById(id);
        if (template != null) {
            template.setEnabled(false);
            return this.updateById(template);
        }
        return false;
    }
    
}