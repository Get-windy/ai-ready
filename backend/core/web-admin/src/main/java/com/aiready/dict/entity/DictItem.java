package com.aiready.dict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 字典项实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_item")
public class DictItem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 字典类型ID
     */
    private Long dictTypeId;
    
    /**
     * 字典项编码
     */
    private String dictItemCode;
    
    /**
     * 字典项名称
     */
    private String dictItemName;
    
    /**
     * 字典项值
     */
    private String dictItemValue;
    
    /**
     * 字典项标签
     */
    private String dictItemLabel;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 颜色样式
     */
    private String colorStyle;
    
    /**
     * 图标
     */
    private String icon;
    
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
}
