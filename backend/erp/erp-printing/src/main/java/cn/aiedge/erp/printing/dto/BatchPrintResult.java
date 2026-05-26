package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchPrintResult {

    private Integer totalCount;

    private Integer successCount;

    private Integer failedCount;

    private List<String> taskIds;

    private List<String> failedDocumentIds;
}