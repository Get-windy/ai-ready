package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchPrintRequest {

    private Long templateId;

    private Long printerId;

    private List<String> documentIds;

    private String documentType;

    private Integer copies;

    private String operatorName;
}