package com.aiready.dict.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典查询请求
 */
@Data
public class DictQueryRequest {
    
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
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 当前页
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
    
    /**
     * 是否只返回启用状态的字典
     */
    private Boolean onlyEnabled = false;
    
    /**
     * 是否包含字典项
     */
    private Boolean includeItems = true;
    
    /**
     * 是否系统内置
     */
    private Integer isSystem;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createTimeStart;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createTimeEnd;

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
     * 获取页码（别名，用于兼容性）
     */
    public Integer getPage() {
        return this.pageNum;
    }
    
    /**
     * 获取每页大小（别名，用于兼容性）
     */
    public Integer getSize() {
        return this.pageSize;
    }
    
    /**
     * 获取字典名称（别名，用于兼容性）
     */
    public String getDictName() {
        return this.dictTypeName;
    }
}