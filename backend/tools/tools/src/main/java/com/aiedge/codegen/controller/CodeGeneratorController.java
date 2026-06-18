package com.aiedge.codegen.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.aiedge.codegen.entity.DataSourceInfo;
import com.aiedge.codegen.entity.GenerationConfig;
import com.aiedge.codegen.entity.Template;
import com.aiedge.codegen.generator.CodeGeneratorService;
import com.aiedge.codegen.service.DataSourceInfoService;
import com.aiedge.codegen.service.GenerationConfigService;
import com.aiedge.codegen.service.TemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 代码生成器控制器
 * 
 * @author AI-Ready
 */
@RestController
@RequestMapping("/api/codegen")
@Tag(name = "代码生成器", description = "代码生成相关接口")
@RequiredArgsConstructor
public class CodeGeneratorController {
    
    @Autowired
    private final CodeGeneratorService codeGeneratorService;
    
    @Autowired
    private final DataSourceInfoService dataSourceInfoService;
    
    @Autowired
    private final GenerationConfigService generationConfigService;
    
    @Autowired
    private final TemplateService templateService;
    
    /**
     * 生成代码
     * 
     * @param dataSourceId 数据源ID
     * @param configId 生成配置ID
     * @param tables 表名列表（逗号分隔）
     * @param outputDir 输出目录
     * @return 生成结果
     */
    @PostMapping("/generate")
    @Operation(summary = "生成代码")
    @SaCheckLogin
    public Object generateCode(
            @RequestParam Long dataSourceId,
            @RequestParam Long configId,
            @RequestParam String tables,
            @RequestParam String outputDir) {
        
        List<String> tableList = Arrays.asList(tables.split(","));
        boolean success = codeGeneratorService.generateCode(dataSourceId, configId, tableList, outputDir);
        
        if (success) {
            return "代码生成成功";
        } else {
            return "代码生成失败";
        }
    }
    
    /**
     * 使用默认配置生成代码
     * 
     * @param dataSourceId 数据源ID
     * @param tables 表名列表（逗号分隔）
     * @param outputDir 输出目录
     * @return 生成结果
     */
    @PostMapping("/generate/default")
    @Operation(summary = "使用默认配置生成代码")
    @SaCheckLogin
    public Object generateCodeWithDefaultConfig(
            @RequestParam Long dataSourceId,
            @RequestParam String tables,
            @RequestParam String outputDir) {
        
        List<String> tableList = Arrays.asList(tables.split(","));
        boolean success = codeGeneratorService.generateCodeWithDefaultConfig(dataSourceId, tableList, outputDir);
        
        if (success) {
            return "代码生成成功";
        } else {
            return "代码生成失败";
        }
    }
    
    /**
     * 测试数据源连接
     * 
     * @param dataSourceId 数据源ID
     * @return 测试结果
     */
    @PostMapping("/datasource/test/{id}")
    @Operation(summary = "测试数据源连接")
    @SaCheckLogin
    public Object testDataSource(@PathVariable("id") Long dataSourceId) {
        DataSourceInfo dataSourceInfo = dataSourceInfoService.getById(dataSourceId);
        if (dataSourceInfo == null) {
            return "数据源不存在";
        }
        
        boolean success = dataSourceInfoService.testConnection(dataSourceInfo);
        if (success) {
            return "连接测试成功";
        } else {
            return "连接测试失败";
        }
    }
    
    /**
     * 获取数据源列表
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 数据源列表
     */
    @GetMapping("/datasource/list")
    @Operation(summary = "获取数据源列表")
    @SaCheckLogin
    public IPage<DataSourceInfo> getDataSourceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<DataSourceInfo> pageInfo = new Page<>(page, size);
        return dataSourceInfoService.page(pageInfo);
    }
    
    /**
     * 获取生成配置列表
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 生成配置列表
     */
    @GetMapping("/config/list")
    @Operation(summary = "获取生成配置列表")
    @SaCheckLogin
    public IPage<GenerationConfig> getGenerationConfigList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<GenerationConfig> pageInfo = new Page<>(page, size);
        return generationConfigService.page(pageInfo);
    }
    
    /**
     * 获取模板列表
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 模板列表
     */
    @GetMapping("/template/list")
    @Operation(summary = "获取模板列表")
    @SaCheckLogin
    public IPage<Template> getTemplateList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<Template> pageInfo = new Page<>(page, size);
        return templateService.page(pageInfo);
    }
    
    /**
     * 根据类型获取模板列表
     * 
     * @param type 模板类型
     * @param page 页码
     * @param size 页面大小
     * @return 模板列表
     */
    @GetMapping("/template/list/type/{type}")
    @Operation(summary = "根据类型获取模板列表")
    @SaCheckLogin
    public List<Template> getTemplateListByType(
            @PathVariable("type") String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        return templateService.getByType(type);
    }
    
}