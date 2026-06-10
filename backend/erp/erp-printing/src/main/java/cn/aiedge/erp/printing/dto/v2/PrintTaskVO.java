package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PrintTaskVO {

    private Long taskId;
    private String taskCode;
    private Long tenantId;
    private String pageCode;
    private String documentType;
    private Long documentId;
    private String documentNo;

    private Map<String, Object> dataJson;

    private Long chainId;
    private String chainName;
    private Long chainItemId;
    private Integer stepOrder;

    private Long clientId;
    private String clientName;
    private String printerName;

    private Long templateId;
    private String templateName;

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
    /** 截图任务状态：PENDING / PROCESSING / COMPLETED / FAILED，无截图时为 null */
    private String screenshotStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
