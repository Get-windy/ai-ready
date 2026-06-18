package com.aiedge.codegen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代码生成配置实体类
 * 
 * @author AI-Ready
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("codegen_generation_config")
public class GenerationConfig {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置名称
     */
    private String name;
    
    /**
     * 作者
     */
    private String author = "AI-Ready";
    
    /**
     * 包名
     */
    private String packageName = "com.aiedge";
    
    /**
     * 模块名
     */
    private String moduleName = "demo";
    
    /**
     * 生成路径
     */
    private String pathInfo;
    
    /**
     * 是否覆盖已存在的文件
     */
    private Boolean fileOverride = false;
    
    /**
     * 注释日期格式
     */
    private String commentDate = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 是否开启swagger
     */
    private Boolean enableSwagger = false;
    
    /**
     * 是否开启Lombok
     */
    private Boolean enableLombok = true;
    
    /**
     * 是否开启链式模型
     */
    private Boolean enableChainModel = false;
    
    /**
     * 是否开启ActiveRecord
     */
    private Boolean enableActiveRecord = false;
    
    /**
     * 是否开启BaseResultMap
     */
    private Boolean enableBaseResultMap = false;
    
    /**
     * 是否开启BaseColumnList
     */
    private Boolean enableBaseColumnList = false;
    
    /**
     * 描述
     */
    private String description;
    
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