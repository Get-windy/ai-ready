package com.aiready.dict.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 字典项保存请求
 */
@Data
public class DictItemSaveRequest {
    
    /**
     * 字典类型ID
     */
    @NotNull(message = "字典类型ID不能为空")
    private Long dictTypeId;
    
    /**
     * 字典项编码
     */
    @NotBlank(message = "字典项编码不能为空")
    private String itemCode;
    
    /**
     * 字典项名称
     */
    @NotBlank(message = "字典项名称不能为空")
    private String itemName;
    
    /**
     * 字典项值
     */
    private String itemValue;
    
    /**
     * 字典项描述
     */
    private String description;
    
    /**
     * 父级ID
     */
    private Long parentId;
    
    /**
     * CSS样式类名
     */
    private String cssClass;
    
    /**
     * 扩展属性（JSON格式）
     */
    private String extraAttr;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 是否默认选中
     */
    private Integer isDefault;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 排序
     */
    private Integer sortOrder;
}
