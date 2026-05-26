package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetDepreciationVO {
    
    private Long id;
    
    private Long assetId;
    
    private String assetCode;
    
    private String assetName;
    
    private String period;
    
    private LocalDate depreciationDate;
    
    private BigDecimal originalValue;
    
    private BigDecimal accumulatedDepreciation;
    
    private BigDecimal periodDepreciation;
    
    private BigDecimal netValue;
    
    private BigDecimal depreciationRate;
    
    private Integer usedMonths;
    
    private Integer remainingMonths;
    
    private Integer status;
    
    private String statusName;
    
    private String voucherNo;
    
    private String remark;
}