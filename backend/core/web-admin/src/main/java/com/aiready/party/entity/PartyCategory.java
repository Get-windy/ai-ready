package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 往来单位分类实体类
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_category")
public class PartyCategory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类编码
     */
    private String categoryCode;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 所属单位类型（1：客户 2：供应商 3：物流商 4：配套商）
     */
    private Integer partyType;
    
    /**
     * 上级分类ID
     */
    private Long parentId;
    
    /**
     * 分类级别
     */
    private Integer level;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
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
