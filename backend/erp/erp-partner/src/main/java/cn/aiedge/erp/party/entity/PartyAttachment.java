package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 往来单位附件（基础资料：供应商/客户/物流公司等的「附件」分区与列表附件列）
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("erp_partner_attachment")
public class PartyAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long partnerId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private String category;

    private Integer sortOrder;

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
