package com.aiedge.codegen.service;

import com.aiedge.codegen.entity.Template;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 模板服务接口
 * 
 * @author AI-Ready
 */
public interface TemplateService extends IService<Template> {
    
    /**
     * 根据类型获取模板列表
     * 
     * @param type 模板类型
     * @return 模板列表
     */
    List<Template> getByType(String type);
    
    /**
     * 根据名称获取模板
     * 
     * @param name 模板名称
     * @return 模板
     */
    Template getByName(String name);
    
    /**
     * 根据类型和名称获取模板
     * 
     * @param type 模板类型
     * @param name 模板名称
     * @return 模板
     */
    Template getByTypeAndName(String type, String name);
    
    /**
     * 激活模板
     * 
     * @param id 模板ID
     * @return 操作结果
     */
    boolean activate(Long id);
    
    /**
     * 停用模板
     * 
     * @param id 模板ID
     * @return 操作结果
     */
    boolean deactivate(Long id);
    
}