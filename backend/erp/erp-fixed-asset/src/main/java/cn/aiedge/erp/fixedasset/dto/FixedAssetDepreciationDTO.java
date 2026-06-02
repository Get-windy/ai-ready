package cn.aiedge.erp.fixedasset.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产折旧记录DTO
 */
@Data
public class FixedAssetDepreciationDTO {
    private Long id;
    private Long assetId;
    private String period;
    private LocalDate depreciationDate;
    private BigDecimal periodAmount;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netValue;
    private BigDecimal assetOriginalValue;
    private String assetName;
    private String assetCode;
    private String status;
    private String remark;
}
