package com.aiedge.codegen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据源信息实体类
 * 
 * @author AI-Ready
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("codegen_datasource_info")
public class DataSourceInfo {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据源名称
     */
    private String name;
    
    /**
     * 数据源描述
     */
    private String description;
    
    /**
     * JDBC URL
     */
    private String jdbcUrl;
    
    /**
     * 驱动名称
     */
    private String driverName;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 数据库类型 (mysql, oracle, postgresql, sqlserver)
     */
    private String dbType;
    
    /**
     * schema名称
     */
    private String schema;
    
    /**
     * 是否启用
     */
    private Boolean enabled = true;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 更新人
     */
    private String updatedBy;
    
}