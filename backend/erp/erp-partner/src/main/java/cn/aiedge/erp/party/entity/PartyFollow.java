package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 往来单位跟进记录
 */
@Getter
@Setter
@TableName("biz_party_follow")
public class PartyFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long partyId;

    private Integer followType;

    private String content;

    private Integer followResult;

    private LocalDateTime nextFollowTime;

    private Long followBy;

    private String followByName;

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
