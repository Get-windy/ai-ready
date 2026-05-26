package com.aiready.dict.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典类型DTO
 */
@Data
public class DictTypeDTO {
    
    private Long id;
    
    /**
     * 字典类型编码
     */
    private String dictTypeCode;
    
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
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 是否系统内置
     */
    private Integer isSystem;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 字典项列表
     */
    private List<DictItemDTO> dictItems;
    
    /**
     * 字典项数量
     */
    private Integer itemCount;

    /**
     * 字典编码（别名，用于兼容性）
     */
    public String getDictCode() {
        return this.dictTypeCode;
    }

    /**
     * 设置字典编码（别名，用于兼容性）
     */
    public void setDictCode(String dictCode) {
        this.dictTypeCode = dictCode;
    }
    
    /**
     * 获取字典名称（别名，用于兼容性）
     */
    public String getDictName() {
        return this.dictTypeName;
    }
}