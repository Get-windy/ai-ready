package cn.aiedge.erp.printing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_print_log")
public class PrintLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String taskCode;

    private Long templateId;

    private String templateName;

    private Long printerId;

    private String printerName;

    private String documentId;

    private String documentType;

    private String documentNo;

    private Integer copies;

    private String status;

    private Boolean success;

    private String errorMessage;

    private Long printDuration;

    private LocalDateTime printTime;

    private String operatorName;

    private Long operatorId;

    private String clientIp;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}