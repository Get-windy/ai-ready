package cn.aiedge.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_contract_attachment")
public class ContractAttachment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long contractId;

    private String fileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private Integer attachmentType;

    private String description;

    private Long uploadedBy;

    private LocalDateTime uploadedTime;

    private Boolean signed;

    private LocalDateTime signedTime;

    private String signedBy;

    private String signatureLocation;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}