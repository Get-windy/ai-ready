package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.util.List;

@Data
public class PrintTaskCreateRequest {

    private Long templateId;

    private Long printerId;

    private String documentId;

    private String documentType;

    private String documentNo;

    private String printData;

    private Integer copies;

    private Integer priority;

    private String operatorName;
}