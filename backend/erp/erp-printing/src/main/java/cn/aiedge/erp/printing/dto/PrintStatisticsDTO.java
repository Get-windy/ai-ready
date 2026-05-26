package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PrintStatisticsDTO {

    private Long totalPrints;

    private Long successPrints;

    private Long failedPrints;

    private Long totalDuration;

    private Double avgDuration;

    private Map<String, Long> printerStats;

    private Map<String, Long> templateStats;

    private Map<String, Long> documentTypeStats;

    private Map<String, Long> dailyStats;
}