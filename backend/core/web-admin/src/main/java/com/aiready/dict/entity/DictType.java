package com.aiready.dict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 字典类型实体类
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_type")
public class DictType {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 字典类型编码
     */
    private String dictCode;
    
    /**
     * 字典类型名称
     */
    private String dictTypeName;
    
    /**
     * 字典类型描述
     */
    private String description;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 是否系统内置（0：否 1：是）
     */
    private Integer isSystem;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 获取字典名称（别名，用于兼容性）
     */
    public String getDictName() {
        return this.dictTypeName;
    }
    
    /**
     * 获取字典类型编码（别名，用于兼容性）
     */
    public String getDictTypeCode() {
        return this.dictCode;
    }
}