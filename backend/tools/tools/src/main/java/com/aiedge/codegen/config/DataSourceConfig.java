package com.aiedge.codegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据源配置类
 * 
 * @author AI-Ready
 */
@Data
@Component
@ConfigurationProperties(prefix = "codegen.datasource")
public class DataSourceConfig {
    
    /**
     * 数据库驱动
     */
    private String driverName = "com.mysql.cj.jdbc.Driver";
    
    /**
     * 数据库URL
     */
    private String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
    
    /**
     * 数据库用户名
     */
    private String username = "root";
    
    /**
     * 数据库密码
     */
    private String password = "password";
    
    /**
     * 数据库类型
     */
    private String dbType = "mysql";
    
    /**
     * schema名称（针对Oracle、PostgreSQL等）
     */
    private String schema = "";
    
}