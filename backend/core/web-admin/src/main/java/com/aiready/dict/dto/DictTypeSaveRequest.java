package com.aiready.dict.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 字典类型保存请求
 */
@Data
public class DictTypeSaveRequest {
    
    /**
     * 字典类型编码
     */
    @NotBlank(message = "字典类型编码不能为空")
    private String dictCode;
    
    /**
     * 字典类型名称
     */
    @NotBlank(message = "字典类型名称不能为空")
    private String dictName;
    
    /**
     * 字典描述
     */
    private String description;
    
    /**
     * 是否系统内置
     */
    private Integer isSystem;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 排序
     */
    private Integer sortOrder;
}
