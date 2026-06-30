package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_contact")
public class PartyContact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long partyId;

    private String contactName;

    private String position;

    private String department;

    private String phone;

    private String mobile;

    private String email;

    private String wechat;

    private String qq;

    private Integer isPrimary;

    private Integer contactRole;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
