package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrintTaskDTO {

    private Long id;

    private String taskCode;

    private Long templateId;

    private String templateName;

    private Long printerId;

    private String printerName;

    private String documentId;

    private String documentType;

    private String documentNo;

    private String printData;

    private Integer copies;

    private Integer priority;

    private String status;

    private Integer retryCount;

    private String errorMessage;

    private LocalDateTime submitTime;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private Long printDuration;

    private String operatorName;
}