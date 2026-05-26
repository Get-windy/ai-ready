package cn.aiedge.erp.printing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_print_template")
public class PrintTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String templateName;

    private String templateType;

    private String fileType;

    private String filePath;

    private String content;

    private String variables;

    private Integer width;

    private Integer height;

    private Integer orientation;

    private Integer copies;

    private Integer paperSize;

    private Integer status;

    private String remark;

    private Long tenantId;

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