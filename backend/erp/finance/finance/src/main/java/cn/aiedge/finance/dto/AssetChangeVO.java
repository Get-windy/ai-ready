package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetChangeVO {
    
    private Long id;
    
    private Long assetId;
    
    private String assetCode;
    
    private String assetName;
    
    private Integer changeType;
    
    private String changeTypeName;
    
    private LocalDate changeDate;
    
    private BigDecimal beforeValue;
    
    private BigDecimal afterValue;
    
    private BigDecimal changeAmount;
    
    private Integer beforeStatus;
    
    private String beforeStatusName;
    
    private Integer afterStatus;
    
    private String afterStatusName;
    
    private String beforeDepartmentName;
    
    private String afterDepartmentName;
    
    private String beforeLocationName;
    
    private String afterLocationName;
    
    private String beforeCustodianName;
    
    private String afterCustodianName;
    
    private String reason;
}