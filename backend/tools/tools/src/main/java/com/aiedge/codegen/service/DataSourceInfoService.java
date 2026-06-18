package com.aiedge.codegen.service;

import com.aiedge.codegen.entity.DataSourceInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 数据源信息服务接口
 * 
 * @author AI-Ready
 */
public interface DataSourceInfoService extends IService<DataSourceInfo> {
    
    /**
     * 获取所有启用的数据源
     * 
     * @return 启用的数据源列表
     */
    List<DataSourceInfo> getAllEnabled();
    
    /**
     * 根据名称获取数据源
     * 
     * @param name 数据源名称
     * @return 数据源信息
     */
    DataSourceInfo getByName(String name);
    
    /**
     * 测试数据源连接
     * 
     * @param dataSourceInfo 数据源信息
     * @return 连接测试结果
     */
    boolean testConnection(DataSourceInfo dataSourceInfo);
    
    /**
     * 激活数据源
     * 
     * @param id 数据源ID
     * @return 操作结果
     */
    boolean activate(Long id);
    
    /**
     * 停用数据源
     * 
     * @param id 数据源ID
     * @return 操作结果
     */
    boolean deactivate(Long id);
    
}