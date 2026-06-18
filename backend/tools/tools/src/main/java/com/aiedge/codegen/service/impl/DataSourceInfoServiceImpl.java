package com.aiedge.codegen.service.impl;

import com.aiedge.codegen.entity.DataSourceInfo;
import com.aiedge.codegen.mapper.DataSourceInfoMapper;
import com.aiedge.codegen.service.DataSourceInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据源信息服务实现类
 * 
 * @author AI-Ready
 */
@Slf4j
@Service
public class DataSourceInfoServiceImpl extends ServiceImpl<DataSourceInfoMapper, DataSourceInfo>
    implements DataSourceInfoService {
    
    @Override
    public List<DataSourceInfo> getAllEnabled() {
        QueryWrapper<DataSourceInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("enabled", true);
        return this.list(wrapper);
    }
    
    @Override
    public DataSourceInfo getByName(String name) {
        QueryWrapper<DataSourceInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        wrapper.eq("enabled", true);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean testConnection(DataSourceInfo dataSourceInfo) {
        try {
            Class.forName(dataSourceInfo.getDriverName());
            Connection connection = DriverManager.getConnection(
                dataSourceInfo.getJdbcUrl(), 
                dataSourceInfo.getUsername(), 
                dataSourceInfo.getPassword()
            );
            connection.close();
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            log.error("测试数据库连接失败, url={}", dataSourceInfo.getJdbcUrl(), e);
            return false;
        }
    }
    
    @Override
    public boolean activate(Long id) {
        DataSourceInfo dataSourceInfo = this.getById(id);
        if (dataSourceInfo != null) {
            dataSourceInfo.setEnabled(true);
            return this.updateById(dataSourceInfo);
        }
        return false;
    }
    
    @Override
    public boolean deactivate(Long id) {
        DataSourceInfo dataSourceInfo = this.getById(id);
        if (dataSourceInfo != null) {
            dataSourceInfo.setEnabled(false);
            return this.updateById(dataSourceInfo);
        }
        return false;
    }
    
}