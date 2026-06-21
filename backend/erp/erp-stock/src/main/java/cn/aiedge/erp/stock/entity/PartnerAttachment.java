package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 往来单位附件
 */
@Data
@Accessors(chain = true)
@TableName("erp_partner_attachment")
public class PartnerAttachment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long partnerId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private String category;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
