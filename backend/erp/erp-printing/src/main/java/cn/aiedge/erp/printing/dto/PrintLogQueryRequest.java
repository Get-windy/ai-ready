package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PrintLogQueryRequest {

    private Integer page;

    private Integer size;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long printerId;

    private Long templateId;

    private String documentType;

    private String status;

    private Boolean success;

    private String operatorName;
}