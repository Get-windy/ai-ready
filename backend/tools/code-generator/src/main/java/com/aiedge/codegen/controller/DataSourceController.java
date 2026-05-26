package com.aiedge.codegen.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.aiedge.codegen.entity.DataSourceInfo;
import com.aiedge.codegen.service.DataSourceInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 数据源管理控制器
 * 
 * @author AI-Ready
 */
@RestController
@RequestMapping("/api/codegen/datasource")
@Tag(name = "数据源管理", description = "数据源管理相关接口")
@RequiredArgsConstructor
public class DataSourceController {
    
    @Autowired
    private final DataSourceInfoService dataSourceInfoService;
    
    /**
     * 创建数据源
     * 
     * @param dataSourceInfo 数据源信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建数据源")
    @SaCheckLogin
    public Object createDataSource(@RequestBody DataSourceInfo dataSourceInfo) {
        dataSourceInfo.setCreateTime(LocalDateTime.now());
        dataSourceInfo.setUpdateTime(LocalDateTime.now());
        dataSourceInfo.setCreatedBy("system");
        dataSourceInfo.setUpdatedBy("system");
        
        boolean success = dataSourceInfoService.save(dataSourceInfo);
        if (success) {
            return "数据源创建成功";
        } else {
            return "数据源创建失败";
        }
    }
    
    /**
     * 更新数据源
     * 
     * @param dataSourceInfo 数据源信息
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "更新数据源")
    @SaCheckLogin
    public Object updateDataSource(@RequestBody DataSourceInfo dataSourceInfo) {
        dataSourceInfo.setUpdateTime(LocalDateTime.now());
        dataSourceInfo.setUpdatedBy("system");
        
        boolean success = dataSourceInfoService.updateById(dataSourceInfo);
        if (success) {
            return "数据源更新成功";
        } else {
            return "数据源更新失败";
        }
    }
    
    /**
     * 删除数据源
     * 
     * @param id 数据源ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除数据源")
    @SaCheckLogin
    public Object deleteDataSource(@PathVariable("id") Long id) {
        boolean success = dataSourceInfoService.removeById(id);
        if (success) {
            return "数据源删除成功";
        } else {
            return "数据源删除失败";
        }
    }
    
    /**
     * 获取数据源详情
     * 
     * @param id 数据源ID
     * @return 数据源详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取数据源详情")
    @SaCheckLogin
    public DataSourceInfo getDataSourceDetail(@PathVariable("id") Long id) {
        return dataSourceInfoService.getById(id);
    }
    
    /**
     * 测试数据源连接
     * 
     * @param id 数据源ID
     * @return 测试结果
     */
    @PostMapping("/test/{id}")
    @Operation(summary = "测试数据源连接")
    @SaCheckLogin
    public Object testDataSource(@PathVariable("id") Long id) {
        DataSourceInfo dataSourceInfo = dataSourceInfoService.getById(id);
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
     * 激活数据源
     * 
     * @param id 数据源ID
     * @return 激活结果
     */
    @PostMapping("/activate/{id}")
    @Operation(summary = "激活数据源")
    @SaCheckLogin
    public Object activateDataSource(@PathVariable("id") Long id) {
        boolean success = dataSourceInfoService.activate(id);
        if (success) {
            return "数据源激活成功";
        } else {
            return "数据源激活失败";
        }
    }
    
    /**
     * 停用数据源
     * 
     * @param id 数据源ID
     * @return 停用结果
     */
    @PostMapping("/deactivate/{id}")
    @Operation(summary = "停用数据源")
    @SaCheckLogin
    public Object deactivateDataSource(@PathVariable("id") Long id) {
        boolean success = dataSourceInfoService.deactivate(id);
        if (success) {
            return "数据源停用成功";
        } else {
            return "数据源停用失败";
        }
    }
    
    /**
     * 分页获取数据源列表
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 数据源列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页获取数据源列表")
    @SaCheckLogin
    public IPage<DataSourceInfo> getDataSourceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<DataSourceInfo> pageInfo = new Page<>(page, size);
        return dataSourceInfoService.page(pageInfo);
    }
    
}