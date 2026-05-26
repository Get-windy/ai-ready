package com.aiready.dict.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项DTO
 */
@Data
public class DictItemDTO {
    
    private Long id;
    
    /**
     * 字典类型ID
     */
    private Long dictTypeId;
    
    /**
     * 字典类型编码
     */
    private String dictTypeCode;
    
    /**
     * 字典类型名称
     */
    private String dictTypeName;
    
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
     * 状态描述
     */
    private String statusDesc;
    
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
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 字典编码（别名，用于兼容性）
     */
    public String getDictCode() {
        return this.dictItemCode;
    }

    /**
     * 设置字典编码（别名，用于兼容性）
     */
    public void setDictCode(String dictCode) {
        this.dictItemCode = dictCode;
    }
}