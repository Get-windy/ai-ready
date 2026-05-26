package cn.aiedge.erp.printing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_print_task")
public class PrintTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    private Long templateId;

    private Long printerId;

    private String documentId;

    private String documentType;

    private String documentNo;

    private String printData;

    private Integer copies;

    private Integer priority;

    private String status;

    private Integer retryCount;

    private Integer maxRetry;

    private String errorMessage;

    private LocalDateTime submitTime;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private Long printDuration;

    private String operatorName;

    private String clientIp;

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