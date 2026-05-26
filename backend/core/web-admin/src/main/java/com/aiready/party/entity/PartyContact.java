package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 往来单位联系人实体类
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_contact")
public class PartyContact {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 单位ID
     */
    private Long partyId;
    
    /**
     * 联系人姓名
     */
    private String contactName;
    
    /**
     * 联系人职位
     */
    private String position;
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 手机号
     */
    private String mobile;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 微信号
     */
    private String wechat;
    
    /**
     * QQ号
     */
    private String qq;
    
    /**
     * 是否主要联系人（0：否 1：是）
     */
    private Integer isPrimary;
    
    /**
     * 联系人角色（1：业务 2：财务 3：技术 4：决策人 5：其他）
     */
    private Integer contactRole;
    
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
