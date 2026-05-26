package com.aiedge.codegen.service.impl;

import com.aiedge.codegen.entity.GenerationConfig;
import com.aiedge.codegen.mapper.GenerationConfigMapper;
import com.aiedge.codegen.service.GenerationConfigService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 生成配置服务实现类
 * 
 * @author AI-Ready
 */
@Service
public class GenerationConfigServiceImpl extends ServiceImpl<GenerationConfigMapper, GenerationConfig> 
    implements GenerationConfigService {
    
    @Override
    public GenerationConfig getDefaultConfig() {
        QueryWrapper<GenerationConfig> wrapper = new QueryWrapper<>();
        wrapper.last("LIMIT 1"); // 获取第一个配置作为默认配置
        return this.getOne(wrapper);
    }
    
    @Override
    public GenerationConfig getByName(String name) {
        QueryWrapper<GenerationConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean activate(Long id) {
        // 在这个简单的实现中，我们不区分激活/停用状态
        // 如果需要可以添加 enabled 字段
        GenerationConfig config = this.getById(id);
        return config != null;
    }
    
    @Override
    public boolean deactivate(Long id) {
        // 在这个简单的实现中，我们不区分激活/停用状态
        // 如果需要可以添加 enabled 字段
        GenerationConfig config = this.getById(id);
        return config != null;
    }
    
}