package com.aiready.dict.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 字典保存请求
 */
@Data
public class DictSaveRequest {
    
    /**
     * 字典类型编码
     */
    @NotBlank(message = "字典类型编码不能为空")
    private String dictTypeCode;
    
    /**
     * 字典类型名称
     */
    @NotBlank(message = "字典类型名称不能为空")
    private String dictTypeName;
    
    /**
     * 字典类型描述
     */
    private String description;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status = 1;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder = 0;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 字典项列表
     */
    private List<DictItemRequest> dictItems;
    
    /**
     * 获取字典编码（别名，用于兼容性）
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
     * 字典项请求内部类
     */
    @Data
    public static class DictItemRequest {
        
        /**
         * 字典项编码
         */
        @NotBlank(message = "字典项编码不能为空")
        private String dictItemCode;
        
        /**
         * 字典项名称
         */
        @NotBlank(message = "字典项名称不能为空")
        private String dictItemName;
        
        /**
         * 字典项值
         */
        @NotBlank(message = "字典项值不能为空")
        private String dictItemValue;
        
        /**
         * 字典项标签
         */
        private String dictItemLabel;
        
        /**
         * 状态（0：禁用 1：启用）
         */
        private Integer status = 1;
        
        /**
         * 排序顺序
         */
        private Integer sortOrder = 0;
        
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
    }
}
