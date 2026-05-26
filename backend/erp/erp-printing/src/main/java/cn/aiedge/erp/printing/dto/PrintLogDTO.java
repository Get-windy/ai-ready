package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrintLogDTO {

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
}