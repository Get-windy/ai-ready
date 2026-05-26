package cn.aiedge.finance.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AccountPeriodVO {
    
    private Long id;
    
    private String periodCode;
    
    private Integer year;
    
    private Integer month;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Integer status;
    
    private String statusName;
    
    private Integer isCurrent;
    
    private LocalDate closedDate;
    
    private String closedByName;
    
    private String remark;
}