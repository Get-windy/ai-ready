package com.aiedge.codegen.service;

import com.aiedge.codegen.entity.GenerationConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 生成配置服务接口
 * 
 * @author AI-Ready
 */
public interface GenerationConfigService extends IService<GenerationConfig> {
    
    /**
     * 获取默认生成配置
     * 
     * @return 默认生成配置
     */
    GenerationConfig getDefaultConfig();
    
    /**
     * 根据名称获取生成配置
     * 
     * @param name 配置名称
     * @return 生成配置
     */
    GenerationConfig getByName(String name);
    
    /**
     * 激活生成配置
     * 
     * @param id 配置ID
     * @return 操作结果
     */
    boolean activate(Long id);
    
    /**
     * 停用生成配置
     * 
     * @param id 配置ID
     * @return 操作结果
     */
    boolean deactivate(Long id);
    
}