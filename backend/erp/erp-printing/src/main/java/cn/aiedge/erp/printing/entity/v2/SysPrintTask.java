package cn.aiedge.erp.printing.entity.v2;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_print_task")
public class SysPrintTask {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private String taskCode;

    private Long tenantId;

    private String pageCode;

    private String documentType;

    private Long documentId;

    private String documentNo;

    private String dataJson;

    private Long chainId;

    private Long chainItemId;

    private Integer stepOrder;

    private Long clientId;

    private String printerName;

    private Long templateId;

    private String status;

    private Integer priority;

    private Integer retryCount;

    private Integer maxRetry;

    private String errorMessage;

    private String resultLog;

    private LocalDateTime submitTime;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private Long screenshotId;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
