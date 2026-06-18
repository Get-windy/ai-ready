package com.aiedge.codegen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代码模板实体类
 * 
 * @author AI-Ready
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("codegen_template")
public class Template {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 模板名称
     */
    private String name;
    
    /**
     * 模板类型 (entity, controller, service, mapper, frontend)
     */
    private String type;
    
    /**
     * 模板内容
     */
    private String content;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 版本号
     */
    private String version;
    
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