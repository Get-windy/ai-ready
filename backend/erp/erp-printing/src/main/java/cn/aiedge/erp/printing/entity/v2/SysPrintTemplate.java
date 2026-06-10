package cn.aiedge.erp.printing.entity.v2;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_print_template")
public class SysPrintTemplate {

    @TableId(type = IdType.AUTO)
    private Long templateId;

    private Long tenantId;

    private String pageCode;

    private String templateName;

    private String templateJson;

    private String paperSize;

    private BigDecimal paperWidth;

    private BigDecimal paperHeight;

    private BigDecimal marginTop;

    private BigDecimal marginBottom;

    private BigDecimal marginLeft;

    private BigDecimal marginRight;

    private String status;

    private Boolean isDefault;

    private Integer version;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
