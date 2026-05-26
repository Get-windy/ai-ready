package cn.aiedge.erp.printing.dto;

import lombok.Data;

@Data
public class PrinterStatusDTO {

    private Long printerId;

    private String printerName;

    private String status;

    private Boolean isOnline;

    private Integer activeTasks;

    private Integer paperStatus;

    private Integer inkStatus;

    private String lastError;
}