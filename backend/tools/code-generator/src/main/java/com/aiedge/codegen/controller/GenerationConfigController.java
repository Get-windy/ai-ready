package com.aiedge.codegen.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.aiedge.codegen.entity.GenerationConfig;
import com.aiedge.codegen.service.GenerationConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 生成配置管理控制器
 * 
 * @author AI-Ready
 */
@RestController
@RequestMapping("/api/codegen/config")
@Tag(name = "生成配置管理", description = "代码生成配置管理相关接口")
@RequiredArgsConstructor
public class GenerationConfigController {
    
    @Autowired
    private final GenerationConfigService generationConfigService;
    
    /**
     * 创建生成配置
     * 
     * @param config 生成配置信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建生成配置")
    @SaCheckLogin
    public Object createConfig(@RequestBody GenerationConfig config) {
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setCreatedBy("system");
        config.setUpdatedBy("system");
        
        boolean success = generationConfigService.save(config);
        if (success) {
            return "生成配置创建成功";
        } else {
            return "生成配置创建失败";
        }
    }
    
    /**
     * 更新生成配置
     * 
     * @param config 生成配置信息
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "更新生成配置")
    @SaCheckLogin
    public Object updateConfig(@RequestBody GenerationConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        config.setUpdatedBy("system");
        
        boolean success = generationConfigService.updateById(config);
        if (success) {
            return "生成配置更新成功";
        } else {
            return "生成配置更新失败";
        }
    }
    
    /**
     * 删除生成配置
     * 
     * @param id 生成配置ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除生成配置")
    @SaCheckLogin
    public Object deleteConfig(@PathVariable("id") Long id) {
        boolean success = generationConfigService.removeById(id);
        if (success) {
            return "生成配置删除成功";
        } else {
            return "生成配置删除失败";
        }
    }
    
    /**
     * 获取生成配置详情
     * 
     * @param id 生成配置ID
     * @return 生成配置详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取生成配置详情")
    @SaCheckLogin
    public GenerationConfig getConfigDetail(@PathVariable("id") Long id) {
        return generationConfigService.getById(id);
    }
    
    /**
     * 分页获取生成配置列表
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 生成配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页获取生成配置列表")
    @SaCheckLogin
    public IPage<GenerationConfig> getConfigList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<GenerationConfig> pageInfo = new Page<>(page, size);
        return generationConfigService.page(pageInfo);
    }
    
    /**
     * 获取默认生成配置
     * 
     * @return 默认生成配置
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认生成配置")
    @SaCheckLogin
    public GenerationConfig getDefaultConfig() {
        return generationConfigService.getDefaultConfig();
    }
    
}