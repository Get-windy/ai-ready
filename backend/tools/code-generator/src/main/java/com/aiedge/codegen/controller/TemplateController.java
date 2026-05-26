package com.aiedge.codegen.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.aiedge.codegen.entity.Template;
import com.aiedge.codegen.service.TemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 模板管理控制器
 * 
 * @author AI-Ready
 */
@RestController
@RequestMapping("/api/codegen/template")
@Tag(name = "模板管理", description = "代码模板管理相关接口")
@RequiredArgsConstructor
public class TemplateController {
    
    @Autowired
    private final TemplateService templateService;
    
    /**
     * 创建模板
     * 
     * @param template 模板信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建模板")
    @SaCheckLogin
    public Object createTemplate(@RequestBody Template template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreatedBy("system");
        template.setUpdatedBy("system");
        
        boolean success = templateService.save(template);
        if (success) {
            return "模板创建成功";
        } else {
            return "模板创建失败";
        }
    }
    
    /**
     * 更新模板
     * 
     * @param template 模板信息
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "更新模板")
    @SaCheckLogin
    public Object updateTemplate(@RequestBody Template template) {
        template.setUpdateTime(LocalDateTime.now());
        template.setUpdatedBy("system");
        
        boolean success = templateService.updateById(template);
        if (success) {
            return "模板更新成功";
        } else {
            return "模板更新失败";
        }
    }
    
    /**
     * 删除模板
     * 
     * @param id 模板ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除模板")
    @SaCheckLogin
    public Object deleteTemplate(@PathVariable("id") Long id) {
        boolean success = templateService.removeById(id);
        if (success) {
            return "模板删除成功";
        } else {
            return "模板删除失败";
        }
    }
    
    /**
     * 获取模板详情
     * 
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取模板详情")
    @SaCheckLogin
    public Template getTemplateDetail(@PathVariable("id") Long id) {
        return templateService.getById(id);
    }
    
    /**
     * 激活模板
     * 
     * @param id 模板ID
     * @return 激活结果
     */
    @PostMapping("/activate/{id}")
    @Operation(summary = "激活模板")
    @SaCheckLogin
    public Object activateTemplate(@PathVariable("id") Long id) {
        boolean success = templateService.activate(id);
        if (success) {
            return "模板激活成功";
        } else {
            return "模板激活失败";
        }
    }
    
    /**
     * 停用模板
     * 
     * @param id 模板ID
     * @return 停用结果
     */
    @PostMapping("/deactivate/{id}")
    @Operation(summary = "停用模板")
    @SaCheckLogin
    public Object deactivateTemplate(@PathVariable("id") Long id) {
        boolean success = templateService.deactivate(id);
        if (success) {
            return "模板停用成功";
        } else {
            return "模板停用失败";
        }
    }
    
    /**
     * 分页获取模板列表
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 模板列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页获取模板列表")
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
    @GetMapping("/list/type/{type}")
    @Operation(summary = "根据类型获取模板列表")
    @SaCheckLogin
    public java.util.List<Template> getTemplateListByType(
            @PathVariable("type") String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        return templateService.getByType(type);
    }
    
}